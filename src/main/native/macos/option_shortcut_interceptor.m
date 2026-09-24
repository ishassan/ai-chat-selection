#import <AppKit/AppKit.h>
#import <dispatch/dispatch.h>
#import <jni.h>
#import <objc/message.h>
#import <objc/runtime.h>
#import <pthread.h>
#import <stdlib.h>

typedef BOOL (*PerformKeyEquivalentFn)(id, SEL, NSEvent *);
typedef void (*KeyDownFn)(id, SEL, NSEvent *);
typedef void (*KeyUpFn)(id, SEL, NSEvent *);

static JavaVM *gJavaVM;
static jobject gInterceptor;
static jmethodID gShouldInterceptNativeKey;
static jmethodID gDispatchNativeShortcut;

static Class gAWTViewClass;
static IMP gOriginalPerformKeyEquivalent;
static IMP gOriginalKeyDown;
static IMP gOriginalKeyUp;

static unsigned short gLastInterceptedKeyCode;
static BOOL gHasInterceptedKey;
static NSEvent *gLastInterceptedEvent;

static BOOL ClassDeclaresMethod(Class cls, SEL selector) {
  unsigned int count = 0;
  Method *methods = class_copyMethodList(cls, &count);
  BOOL found = NO;
  for (unsigned int index = 0; index < count; index++) {
    if (method_getName(methods[index]) == selector) {
      found = YES;
      break;
    }
  }
  free(methods);
  return found;
}

static void ClearInterceptedKey(void) {
  gHasInterceptedKey = NO;
  gLastInterceptedEvent = nil;
}

static JNIEnv *GetJNIEnv(void) {
  if (gJavaVM == NULL) return NULL;

  JNIEnv *env = NULL;
  jint result = (*gJavaVM)->GetEnv(gJavaVM, (void **)&env, JNI_VERSION_1_6);
  if (result == JNI_OK) return env;
  if (result != JNI_EDETACHED) return NULL;
  if ((*gJavaVM)->AttachCurrentThread(gJavaVM, (void **)&env, NULL) != JNI_OK) return NULL;
  return env;
}

static BOOL ShouldIntercept(NSEvent *event) {
  const NSUInteger modifiers = [event modifierFlags];
  if ((modifiers & NSEventModifierFlagOption) == 0 || gInterceptor == NULL) return NO;

  JNIEnv *env = GetJNIEnv();
  if (env == NULL) return NO;

  const jboolean result = (*env)->CallBooleanMethod(
      env, gInterceptor, gShouldInterceptNativeKey, (jint)[event keyCode], (jint)modifiers);
  if ((*env)->ExceptionCheck(env)) {
    (*env)->ExceptionClear(env);
    return NO;
  }
  return result == JNI_TRUE;
}

static void DispatchShortcut(NSEvent *event, id view) {
  JNIEnv *env = GetJNIEnv();
  if (env == NULL) return;

  (*env)->CallVoidMethod(env, gInterceptor, gDispatchNativeShortcut,
                         (jint)[event keyCode], (jint)[event modifierFlags]);
  if ((*env)->ExceptionCheck(env)) {
    (*env)->ExceptionClear(env);
  }

  (void)view;
  gLastInterceptedKeyCode = [event keyCode];
  gHasInterceptedKey = YES;
  gLastInterceptedEvent = event;
}

static BOOL InterceptOrPass(id view, NSEvent *event) {
  if (event == gLastInterceptedEvent) return YES;
  if (ShouldIntercept(event)) {
    if (![event isARepeat]) {
      DispatchShortcut(event, view);
    }
    return YES;
  }
  return NO;
}

static BOOL AIFocusPerformKeyEquivalent(id self, SEL selector, NSEvent *event) {
  if (InterceptOrPass(self, event)) return YES;
  return ((PerformKeyEquivalentFn)gOriginalPerformKeyEquivalent)(self, selector, event);
}

static void AIFocusKeyDown(id self, SEL selector, NSEvent *event) {
  if (InterceptOrPass(self, event)) return;
  ((KeyDownFn)gOriginalKeyDown)(self, selector, event);
}

static void AIFocusKeyUp(id self, SEL selector, NSEvent *event) {
  if (gHasInterceptedKey && [event keyCode] == gLastInterceptedKeyCode) {
    ClearInterceptedKey();
    return;
  }
  ((KeyUpFn)gOriginalKeyUp)(self, selector, event);
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
  (void)reserved;
  gJavaVM = vm;
  return JNI_VERSION_1_6;
}

JNIEXPORT jboolean JNICALL
Java_local_aichatfocus_OptionShortcutInterceptor_installNativeHook0(
    JNIEnv *env, jclass owner, jobject interceptor) {
  (void)owner;
  if (gInterceptor != NULL) return JNI_TRUE;

  jclass interceptorClass = (*env)->GetObjectClass(env, interceptor);
  if (interceptorClass == NULL) return JNI_FALSE;
  gShouldInterceptNativeKey = (*env)->GetMethodID(env, interceptorClass,
      "shouldInterceptNativeKey", "(II)Z");
  gDispatchNativeShortcut = (*env)->GetMethodID(env, interceptorClass,
      "dispatchNativeShortcut", "(II)V");
  if (gShouldInterceptNativeKey == NULL || gDispatchNativeShortcut == NULL) {
    (*env)->ExceptionClear(env);
    return JNI_FALSE;
  }
  gInterceptor = (*env)->NewGlobalRef(env, interceptor);
  if (gInterceptor == NULL) return JNI_FALSE;

  __block BOOL installed = NO;
  void (^installBlock)(void) = ^{
    gAWTViewClass = objc_lookUpClass("AWTView");
    if (gAWTViewClass == Nil) return;

    SEL performSelector = @selector(performKeyEquivalent:);
    SEL keyDownSelector = @selector(keyDown:);
    SEL keyUpSelector = @selector(keyUp:);
    if (!ClassDeclaresMethod(gAWTViewClass, performSelector)
        || !ClassDeclaresMethod(gAWTViewClass, keyDownSelector)
        || !ClassDeclaresMethod(gAWTViewClass, keyUpSelector)) {
      gAWTViewClass = Nil;
      return;
    }

    Method performMethod = class_getInstanceMethod(gAWTViewClass, performSelector);
    Method keyDownMethod = class_getInstanceMethod(gAWTViewClass, keyDownSelector);
    Method keyUpMethod = class_getInstanceMethod(gAWTViewClass, keyUpSelector);
    if (performMethod == NULL || keyDownMethod == NULL || keyUpMethod == NULL) return;

    gOriginalPerformKeyEquivalent = method_setImplementation(performMethod, (IMP)AIFocusPerformKeyEquivalent);
    gOriginalKeyDown = method_setImplementation(keyDownMethod, (IMP)AIFocusKeyDown);
    gOriginalKeyUp = method_setImplementation(keyUpMethod, (IMP)AIFocusKeyUp);
    installed = gOriginalPerformKeyEquivalent != NULL && gOriginalKeyDown != NULL && gOriginalKeyUp != NULL;
    if (!installed) {
      if (method_getImplementation(performMethod) == (IMP)AIFocusPerformKeyEquivalent) {
        method_setImplementation(performMethod, gOriginalPerformKeyEquivalent);
      }
      if (method_getImplementation(keyDownMethod) == (IMP)AIFocusKeyDown) {
        method_setImplementation(keyDownMethod, gOriginalKeyDown);
      }
      if (method_getImplementation(keyUpMethod) == (IMP)AIFocusKeyUp) {
        method_setImplementation(keyUpMethod, gOriginalKeyUp);
      }
      gAWTViewClass = Nil;
    }
  };

  if (pthread_main_np()) {
    installBlock();
  } else {
    dispatch_sync(dispatch_get_main_queue(), installBlock);
  }

  if (!installed) {
    (*env)->DeleteGlobalRef(env, gInterceptor);
    gInterceptor = NULL;
    gAWTViewClass = Nil;
    return JNI_FALSE;
  }
  return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_local_aichatfocus_OptionShortcutInterceptor_uninstallNativeHook0(JNIEnv *env, jclass owner) {
  (void)owner;
  if (gInterceptor == NULL) return;

  void (^uninstallBlock)(void) = ^{
    if (gAWTViewClass != Nil) {
      Method performMethod = class_getInstanceMethod(gAWTViewClass, @selector(performKeyEquivalent:));
      Method keyDownMethod = class_getInstanceMethod(gAWTViewClass, @selector(keyDown:));
      Method keyUpMethod = class_getInstanceMethod(gAWTViewClass, @selector(keyUp:));
      if (performMethod != NULL && method_getImplementation(performMethod) == (IMP)AIFocusPerformKeyEquivalent) {
        method_setImplementation(performMethod, gOriginalPerformKeyEquivalent);
      }
      if (keyDownMethod != NULL && method_getImplementation(keyDownMethod) == (IMP)AIFocusKeyDown) {
        method_setImplementation(keyDownMethod, gOriginalKeyDown);
      }
      if (keyUpMethod != NULL && method_getImplementation(keyUpMethod) == (IMP)AIFocusKeyUp) {
        method_setImplementation(keyUpMethod, gOriginalKeyUp);
      }
    }
    ClearInterceptedKey();
  };

  if (pthread_main_np()) {
    uninstallBlock();
  } else {
    dispatch_sync(dispatch_get_main_queue(), uninstallBlock);
  }

  (*env)->DeleteGlobalRef(env, gInterceptor);
  gInterceptor = NULL;
  gAWTViewClass = Nil;
}
