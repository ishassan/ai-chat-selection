package local.aichatfocus;

import com.intellij.ml.llm.core.chat.context.selection.AIAssistantSelectionService;
import com.intellij.ml.llm.core.chat.context.selection.SelectionContextAttachment;
import com.intellij.ml.llm.core.chat.session.ChatSession;
import com.intellij.ml.llm.core.chat.session.FocusedChatSessionHost;
import com.intellij.ml.llm.core.chat.ui.chat.AIAssistantChatPanel;
import com.intellij.ml.llm.core.chat.ui.chat.context.AIChatContextViewModel;
import com.intellij.ml.llm.core.chat.ui.chat.context.attachments.ContextAttachment;
import com.intellij.ml.llm.core.chat.ui.chat.context.attachments.ContextAttachmentKind;
import com.intellij.ml.llm.core.chat.ui.chat.input.AIAssistantInput;
import com.intellij.ml.llm.core.chat.ui.chat.input.AIAssistantInputListener;
import com.intellij.ml.llm.privacy.PSString;
import com.intellij.ml.llm.privacy.extensions.ExtensionsKtKt;
import com.intellij.llmInstaller.api.AiToolWindowService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.awt.Component;
import java.awt.Container;
import javax.swing.Icon;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

final class AiChatSelectionSync {
  private static final Set<AIAssistantInput> LISTENING_INPUTS =
      Collections.newSetFromMap(new WeakHashMap<>());

  private AiChatSelectionSync() {
  }

  static void sync(Project project, Editor editor) {
    sync(project, editor, findChatPanel(project));
  }

  static void sync(Project project, Editor editor, AIAssistantChatPanel panel) {
    if (project.isDisposed() || editor.isDisposed()) {
      return;
    }

    ChatSession session = FocusedChatSessionHost.Companion.getInstance(project).getFocusedChatSession();
    if (session == null) {
      return;
    }

    AIChatContextViewModel context = session.getContextViewModel().getModelImpl();
    if (context == null) {
      return;
    }

    List<ContextAttachment> existingItems = new ArrayList<>(context.getContextItems());
    for (ContextAttachment item : existingItems) {
      if (item.getKind() == ContextAttachmentKind.SELECTION) {
        context.removeContextItem(item);
      }
    }

    String inputText = getInputText(panel);
    if (inputText != null && inputText.contains("@selection")) {
      panel.setText(inputText);
      return;
    }

    PSString selectedText = ExtensionsKtKt.getPrivacySafe(editor.getSelectionModel()).getSelectedText();
    if (selectedText == null || selectedText.length() == 0) {
      return;
    }

    PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    String fileName = psiFile != null ? psiFile.getName() : "";
    Icon icon = psiFile != null ? psiFile.getIcon(0) : null;
    int selectionStart = editor.getSelectionModel().getSelectionStart();
    int selectionEnd = editor.getSelectionModel().getSelectionEnd();
    TextRange documentRange = new TextRange(selectionStart, selectionEnd);
    var presentationPositions =
        AIAssistantSelectionService.Companion.calculatePositionsForAttachmentPresentation(editor, documentRange);

    SelectionContextAttachment attachment = new SelectionContextAttachment(
        project,
        editor,
        "Selection",
        fileName,
        String.valueOf(editor.hashCode()),
        icon,
        selectedText,
        presentationPositions.getFirst(),
        presentationPositions.getSecond(),
        session.getUid(),
        documentRange,
        ContextAttachmentKind.SELECTION
    );
    context.addContextItem(attachment);
  }

  private static String getInputText(AIAssistantChatPanel panel) {
    if (panel == null) {
      return null;
    }
    return panel.getInput().getText().unwrap().toString();
  }

  static void attachSubmitListener(AIAssistantChatPanel panel, Project project) {
    AIAssistantInput input = panel.getInput();
    if (!LISTENING_INPUTS.add(input)) {
      return;
    }

    input.addListener(new AIAssistantInputListener() {
      @Override
      public void onSubmit(@NotNull com.intellij.ml.llm.core.chat.ui.chat.input.AIAssistantInputTrigger trigger) {
        syncCurrentEditor(project, panel);
      }

      @Override
      public void onSubmitToNewChat(
          @NotNull com.intellij.ml.llm.core.chat.ui.chat.input.AIAssistantInputTrigger trigger) {
        syncCurrentEditor(project, panel);
      }
    });
  }

  private static void syncCurrentEditor(Project project, AIAssistantChatPanel panel) {
    Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
    if (editor != null) {
      sync(project, editor, panel);
    }
  }

  private static AIAssistantChatPanel findChatPanel(Project project) {
    AiToolWindowService service =
        ApplicationManager.getApplication().getService(AiToolWindowService.class);
    if (service == null) {
      return null;
    }
    ToolWindow toolWindow = service.getToolWindow(project);
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
