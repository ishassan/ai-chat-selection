package local.aichatfocus;

import com.intellij.ide.IdeEventQueue;
import com.intellij.ide.DataManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManagerListener;
import com.intellij.openapi.keymap.ex.KeymapManagerEx;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.messages.MessageBusConnection;

import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Routes assigned Option shortcuts around macOS text composition. */
public final class OptionShortcutInterceptor implements Disposable {
  private static final Logger LOG = Logger.getInstance(OptionShortcutInterceptor.class);
  private static final String NATIVE_LIBRARY = "/native/macos/libai_chat_option_shortcuts.dylib";

  private volatile Set<Long> assignedOptionShortcuts = Collections.emptySet();
  private volatile FocusTarget focusTarget = new FocusTarget(null, false);
  private volatile boolean interceptionEnabled;
  private boolean nativeLibraryLoaded;
  private boolean nativeHookInstalled;
  private volatile boolean disposed;

  private final PropertyChangeListener focusOwnerListener = event -> {
    Object newValue = event.getNewValue();
    updateFocusTarget(newValue instanceof Component component ? component : null);
  };

  private final KeymapManagerListener keymapListener = new KeymapManagerListener() {
    @Override
    public void activeKeymapChanged(Keymap keymap) {
      refreshAssignedShortcuts();
    }

    @Override
    public void shortcutsChanged(Keymap keymap, Collection<String> actionIds, boolean added) {
      refreshAssignedShortcuts();
    }
  };

  public OptionShortcutInterceptor() {
    OptionShortcutSettings settings = ApplicationManager.getApplication().getService(OptionShortcutSettings.class);
    interceptionEnabled = SystemInfo.isMac && settings.isOptionShortcutInterceptionEnabled();

    KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
    manager.addPropertyChangeListener("focusOwner", focusOwnerListener);
    updateFocusTarget(manager.getFocusOwner());

    MessageBusConnection connection = ApplicationManager.getApplication().getMessageBus().connect(this);
    connection.subscribe(KeymapManagerListener.TOPIC, keymapListener);
    refreshAssignedShortcuts();

    if (interceptionEnabled) {
      installNativeHook();
    }
  }

  private void updateFocusTarget(Component component) {
    if (SwingUtilities.isEventDispatchThread()) {
      if (!disposed) {
        focusTarget = resolveFocusTarget(component);
      }
    } else {
      SwingUtilities.invokeLater(() -> {
        if (!disposed) {
          Component current = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
          focusTarget = resolveFocusTarget(current);
        }
      });
    }
  }

  public void setInterceptionEnabled(boolean enabled) {
    interceptionEnabled = SystemInfo.isMac && enabled;
    if (interceptionEnabled) {
      installNativeHook();
    } else {
      uninstallNativeHook();
    }
  }

  private void refreshAssignedShortcuts() {
    Keymap keymap = KeymapManagerEx.getInstanceEx().getActiveKeymap();
    if (keymap == null) {
      assignedOptionShortcuts = Collections.emptySet();
      return;
    }

    Set<Long> shortcuts = new HashSet<>();
    for (String actionId : keymap.getActionIds()) {
      for (Shortcut shortcut : keymap.getShortcuts(actionId)) {
        if (!(shortcut instanceof KeyboardShortcut keyboardShortcut)
            || keyboardShortcut.getSecondKeyStroke() != null) {
          continue;
        }

        KeyStroke stroke = keyboardShortcut.getFirstKeyStroke();
        if (stroke == null) {
          continue;
        }
        int modifiers = normalizeModifiers(stroke.getModifiers());
        if ((modifiers & InputEvent.ALT_DOWN_MASK) == 0) {
          continue;
        }

        for (int macKeyCode : MacKeyCodeMap.fromAwtKeyCode(stroke.getKeyCode())) {
          shortcuts.add(signature(macKeyCode, modifiers));
        }
      }
    }
    assignedOptionShortcuts = Collections.unmodifiableSet(shortcuts);
    LOG.info("Loaded " + shortcuts.size() + " assigned Option shortcuts");
  }

  private void installNativeHook() {
    if (!SystemInfo.isMac || !interceptionEnabled || nativeHookInstalled) {
      return;
    }

    try {
      if (!nativeLibraryLoaded) {
        loadNativeLibrary();
        nativeLibraryLoaded = true;
      }
      nativeHookInstalled = installNativeHook0(this);
      if (!nativeHookInstalled) {
        LOG.warn("Could not install the macOS Option shortcut hook");
      } else {
        LOG.info("Installed macOS Option shortcut hook");
      }
    } catch (IOException | UnsatisfiedLinkError | SecurityException exception) {
      LOG.error("Could not load the macOS Option shortcut hook", exception);
    }
  }

  private void uninstallNativeHook() {
    if (nativeHookInstalled) {
      uninstallNativeHook0();
      nativeHookInstalled = false;
      LOG.info("Uninstalled macOS Option shortcut hook");
    }
  }

  private static void loadNativeLibrary() throws IOException {
    Path library = Files.createTempFile("ai-chat-option-shortcuts-", ".dylib");
    try (InputStream input = OptionShortcutInterceptor.class.getResourceAsStream(NATIVE_LIBRARY)) {
      if (input == null) {
        throw new IOException("Missing native library resource: " + NATIVE_LIBRARY);
      }
      Files.copy(input, library, StandardCopyOption.REPLACE_EXISTING);
    }
    library.toFile().setReadable(true, true);
    library.toFile().setExecutable(true, true);
    library.toFile().deleteOnExit();
    System.load(library.toAbsolutePath().toString());
  }

  // Called from the AppKit thread. Keep this method limited to volatile state reads.
  private boolean shouldInterceptNativeKey(int macKeyCode, int nativeModifiers) {
    if (!interceptionEnabled) {
      return false;
    }
    FocusTarget target = focusTarget;
    if (!target.eligible()) return false;
    int modifiers = nativeModifiersToAwt(nativeModifiers);
    return assignedOptionShortcuts.contains(signature(macKeyCode, modifiers));
  }

  // Called from the AppKit thread. EventQueue.postEvent is thread safe.
  private void dispatchNativeShortcut(int macKeyCode, int nativeModifiers) {
    if (!interceptionEnabled) {
      return;
    }
    FocusTarget currentTarget = focusTarget;
    Component target = currentTarget.component();
    int keyCode = MacKeyCodeMap.toAwtKeyCode(macKeyCode);
    if (!currentTarget.eligible() || keyCode == KeyEvent.VK_UNDEFINED) {
      return;
    }

    int modifiers = nativeModifiersToAwt(nativeModifiers);
    long when = System.currentTimeMillis();
    IdeEventQueue queue = IdeEventQueue.getInstance();
    queue.postEvent(new KeyEvent(target, KeyEvent.KEY_PRESSED, when, modifiers,
        keyCode, KeyEvent.CHAR_UNDEFINED, MacKeyCodeMap.keyLocationForMacCode(macKeyCode)));
    queue.postEvent(new KeyEvent(target, KeyEvent.KEY_RELEASED, when, modifiers,
        keyCode, KeyEvent.CHAR_UNDEFINED, MacKeyCodeMap.keyLocationForMacCode(macKeyCode)));
  }

  private static FocusTarget resolveFocusTarget(Component component) {
    if (component == null) return new FocusTarget(null, false);
    for (Component current = component; current != null; current = current.getParent()) {
      String className = current.getClass().getName().toLowerCase();
      if (className.contains("terminal") || className.contains("jediterm")
          || className.contains("jcef") || className.contains("cef")
          || className.contains("browser")) {
        return new FocusTarget(component, false);
      }
    }

    Editor editor = CommonDataKeys.EDITOR.getData(DataManager.getInstance().getDataContext(component));
    if (editor != null) {
      boolean editable = !editor.isViewer() && editor.getDocument().isWritable();
      return new FocusTarget(component, editable);
    }
    if (component instanceof JTextComponent textComponent) {
      return new FocusTarget(component, textComponent.isEnabled() && textComponent.isEditable());
    }
    return new FocusTarget(component, false);
  }

  private record FocusTarget(Component component, boolean eligible) {}

  private static int normalizeModifiers(int modifiers) {
    int result = 0;
    if ((modifiers & (InputEvent.SHIFT_DOWN_MASK | InputEvent.SHIFT_MASK)) != 0) {
      result |= InputEvent.SHIFT_DOWN_MASK;
    }
    if ((modifiers & (InputEvent.CTRL_DOWN_MASK | InputEvent.CTRL_MASK)) != 0) {
      result |= InputEvent.CTRL_DOWN_MASK;
    }
    if ((modifiers & (InputEvent.META_DOWN_MASK | InputEvent.META_MASK)) != 0) {
      result |= InputEvent.META_DOWN_MASK;
    }
    if ((modifiers & (InputEvent.ALT_DOWN_MASK | InputEvent.ALT_MASK)) != 0) {
      result |= InputEvent.ALT_DOWN_MASK;
    }
    return result;
  }

  private static int nativeModifiersToAwt(int modifiers) {
    int result = 0;
    if ((modifiers & (1 << 17)) != 0) result |= InputEvent.SHIFT_DOWN_MASK;
    if ((modifiers & (1 << 18)) != 0) result |= InputEvent.CTRL_DOWN_MASK;
    if ((modifiers & (1 << 19)) != 0) result |= InputEvent.ALT_DOWN_MASK;
    if ((modifiers & (1 << 20)) != 0) result |= InputEvent.META_DOWN_MASK;
    return result;
  }

  private static long signature(int macKeyCode, int modifiers) {
    return ((long) macKeyCode << 32) | (modifiers & 0xffffffffL);
  }

  private static native boolean installNativeHook0(OptionShortcutInterceptor interceptor);

  private static native void uninstallNativeHook0();

  @Override
  public void dispose() {
    disposed = true;
    KeyboardFocusManager.getCurrentKeyboardFocusManager().removePropertyChangeListener("focusOwner", focusOwnerListener);
    KeymapManagerEx.getInstanceEx().removeWeakListener(keymapListener);
    interceptionEnabled = false;
    uninstallNativeHook();
  }
}
