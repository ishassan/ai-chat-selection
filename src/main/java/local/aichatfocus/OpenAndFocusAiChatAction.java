package local.aichatfocus;

import com.intellij.llmInstaller.api.AiToolWindowService;
import com.intellij.ml.llm.core.chat.ui.chat.AIAssistantChatPanel;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import org.jetbrains.annotations.NotNull;

import java.awt.Component;
import java.awt.Container;

public final class OpenAndFocusAiChatAction extends DumbAwareAction {
  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    Project project = event.getProject();
    if (project == null) {
      return;
    }

    AiToolWindowService service =
        ApplicationManager.getApplication().getService(AiToolWindowService.class);
    if (service == null) {
      return;
    }

    ToolWindow toolWindow = service.getToolWindow(project);
    if (toolWindow == null) {
      return;
    }

    if (toolWindow.isVisible()) {
      toolWindow.hide(null);
      return;
    }

    toolWindow.activate(
      () -> ApplicationManager.getApplication().invokeLater(
          () -> {
            AIAssistantChatPanel panel = findChatPanel(toolWindow.getComponent());
            if (panel != null) {
              AiChatSelectionSync.clearManualSelectionAttachments(project);
              Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
              AiChatSelectionSync.refresh(project, editor, panel);
              panel.focusInput();
            }
          }),
        true);
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;
  }

  private static AIAssistantChatPanel findChatPanel(Component component) {
    if (component instanceof AIAssistantChatPanel panel) {
      return panel;
    }
    if (component instanceof Container container) {
      for (Component child : container.getComponents()) {
        AIAssistantChatPanel panel = findChatPanel(child);
        if (panel != null) {
          return panel;
        }
      }
    }
    return null;
  }
}
