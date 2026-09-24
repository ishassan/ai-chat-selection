package local.aichatfocus;

import com.intellij.ml.llm.context.ContextEntity;
import com.intellij.ml.llm.core.chat.session.ChatSession;
import com.intellij.ml.llm.core.chat.session.ContextStorage;
import com.intellij.ml.llm.core.chat.session.FocusedChatSessionHost;
import com.intellij.ml.llm.core.chat.session.HasContextStorage;
import com.intellij.ml.llm.core.chat.ui.chat.AIAssistantChatPanel;
import com.intellij.ml.llm.core.chat.ui.chat.context.UserManualContextStorageScope;
import com.intellij.ml.llm.core.chat.ui.chat.context.attachments.ContextAttachment;
import com.intellij.ml.llm.core.chat.ui.chat.context.attachments.ContextAttachmentKind;
import com.intellij.ml.llm.core.chat.ui.chat.input.AIAssistantInputEditorTextField;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;

final class AiChatSelectionSync {
  private static final String SELECTION_REFERENCE = "@selection";

  private AiChatSelectionSync() {
  }

  static void refresh(Project project, Editor editor) {
    refresh(project, editor, findChatPanel(project));
  }

  static void refresh(Project project, Editor editor, AIAssistantChatPanel panel) {
    if (!isSourceEditor(project, editor) || !editor.getSelectionModel().hasSelection() || panel == null) {
      return;
    }

    String inputText = getInputText(panel);
    if (inputText == null) {
      return;
    }

    ChatSession session = focusedSession(project);
    if (session == null) {
      return;
    }

    clearManualSelectionAttachments(session);
    panel.setText(ensureSelectionReference(inputText));
  }

  static void clearManualSelectionAttachments(Project project) {
    ChatSession session = focusedSession(project);
    if (session != null) {
      clearManualSelectionAttachments(session);
    }
  }

  static boolean isSourceEditor(Project project, Editor editor) {
    if (project == null || editor == null || project.isDisposed() || editor.isDisposed()) {
      return false;
    }
    if (editor.getProject() != project) {
      return false;
    }
    if (AIAssistantInputEditorTextField.Companion.isAIAssistantInputEditor(editor)) {
      return false;
    }
    return FileEditorManager.getInstance(project).getSelectedTextEditor() == editor;
  }

  private static ChatSession focusedSession(Project project) {
    return FocusedChatSessionHost.Companion.getInstance(project).getFocusedChatSession();
  }

  private static void clearManualSelectionAttachments(ChatSession session) {
    if (!(session.getRetrievalSession() instanceof HasContextStorage storageOwner)) {
      return;
    }

    ContextStorage storage = storageOwner.getContextStorage();
    List<ContextEntity> selectionItems = new ArrayList<>();
    for (ContextEntity item : storage.getItems()) {
      if (item instanceof ContextAttachment attachment
          && attachment.getKind() == ContextAttachmentKind.SELECTION) {
        selectionItems.add(item);
      }
    }
    if (!selectionItems.isEmpty()) {
      storage.remove(UserManualContextStorageScope.INSTANCE, selectionItems);
    }
  }

  private static String getInputText(AIAssistantChatPanel panel) {
    return panel.getInput().getEditorTextField().getDocument().getText();
  }

  private static String ensureSelectionReference(String inputText) {
    if (inputText.contains(SELECTION_REFERENCE)) {
      return inputText;
    }
    if (inputText.isBlank()) {
      return SELECTION_REFERENCE + " ";
    }
    return inputText + " " + SELECTION_REFERENCE;
  }

  private static AIAssistantChatPanel findChatPanel(Project project) {
    ToolWindow toolWindow = AiAssistantToolWindowAccess.getToolWindow(project);
    return toolWindow == null ? null : findChatPanel(toolWindow.getComponent());
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
