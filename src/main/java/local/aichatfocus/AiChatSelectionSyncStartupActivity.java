package local.aichatfocus;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.SelectionEvent;
import com.intellij.openapi.editor.event.SelectionListener;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

public final class AiChatSelectionSyncStartupActivity implements StartupActivity, DumbAware {
  @Override
  public void runActivity(@NotNull Project project) {
    EditorFactory.getInstance().getEventMulticaster().addSelectionListener(new SelectionListener() {
      @Override
      public void selectionChanged(@NotNull SelectionEvent event) {
        Editor editor = event.getEditor();
        if (editor.getProject() != project) {
          return;
        }
        if (FileEditorManager.getInstance(project).getSelectedTextEditor() != editor) {
          return;
        }
        ApplicationManager.getApplication().invokeLater(() -> AiChatSelectionSync.sync(project, editor));
      }
    }, project);
  }
}
