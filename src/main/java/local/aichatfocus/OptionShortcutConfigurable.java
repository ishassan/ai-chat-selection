package local.aichatfocus;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.ui.components.JBCheckBox;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;

public final class OptionShortcutConfigurable implements Configurable {
  private JBCheckBox enabledCheckBox;

  @Override
  public @Nls String getDisplayName() {
    return "ai-chat-selection";
  }

  @Override
  public @Nullable JComponent createComponent() {
    enabledCheckBox = new JBCheckBox("Handle assigned Option shortcuts in IntelliJ text fields");
    enabledCheckBox.setToolTipText("Turn this off to use the IDE's normal Option key handling.");

    JPanel panel = new JPanel(new BorderLayout());
    panel.add(enabledCheckBox, BorderLayout.NORTH);
    reset();
    return panel;
  }

  @Override
  public boolean isModified() {
    return enabledCheckBox != null
        && enabledCheckBox.isSelected() != getSettings().isOptionShortcutInterceptionEnabled();
  }

  @Override
  public void apply() {
    if (enabledCheckBox == null) {
      return;
    }

    boolean enabled = enabledCheckBox.isSelected();
    getSettings().setOptionShortcutInterceptionEnabled(enabled);
    ApplicationManager.getApplication()
        .getService(OptionShortcutInterceptor.class)
        .setInterceptionEnabled(enabled);
  }

  @Override
  public void reset() {
    if (enabledCheckBox != null) {
      enabledCheckBox.setSelected(getSettings().isOptionShortcutInterceptionEnabled());
    }
  }

  @Override
  public void disposeUIResources() {
    enabledCheckBox = null;
  }

  private static OptionShortcutSettings getSettings() {
    return ApplicationManager.getApplication().getService(OptionShortcutSettings.class);
  }
}
