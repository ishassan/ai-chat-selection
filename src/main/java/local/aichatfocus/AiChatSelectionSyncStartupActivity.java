package local.aichatfocus;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.SelectionEvent;
import com.intellij.openapi.editor.event.SelectionListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.Alarm;
import org.jetbrains.annotations.NotNull;

public final class AiChatSelectionSyncStartupActivity implements StartupActivity, DumbAware {
  private static final int REFRESH_DELAY_MILLIS = 75;

  @Override
  public void runActivity(@NotNull Project project) {
    if (SystemInfo.isMac) {
      ApplicationManager.getApplication().getService(OptionShortcutInterceptor.class);
    }
    Alarm alarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, project);
    EditorFactory.getInstance().getEventMulticaster().addSelectionListener(new SelectionListener() {
      @Override
      public void selectionChanged(@NotNull SelectionEvent event) {
        Editor editor = event.getEditor();
        if (!AiChatSelectionSync.isSourceEditor(project, editor)) {
          return;
        }

        alarm.cancelAllRequests();
        alarm.addRequest(() -> AiChatSelectionSync.refresh(project, editor), REFRESH_DELAY_MILLIS);
      }
    }, project);
  }
}
