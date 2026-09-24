package local.aichatfocus;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;

final class AiAssistantToolWindowAccess {
  private static final String TOOL_WINDOW_ID = "AIAssistant";

  private AiAssistantToolWindowAccess() {
  }

  static ToolWindow getToolWindow(Project project) {
    return ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID);
  }
}
