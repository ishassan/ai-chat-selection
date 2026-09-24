package local.aichatfocus;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;

@State(name = "AiChatFocusSettings", storages = @Storage("ai-chat-focus.xml"))
public final class OptionShortcutSettings implements PersistentStateComponent<OptionShortcutSettings.State> {
  private volatile State state = new State();

  @Override
  public @NotNull State getState() {
    return state;
  }

  @Override
  public void loadState(@NotNull State state) {
    this.state = state;
  }

  public boolean isOptionShortcutInterceptionEnabled() {
    return state.optionShortcutInterceptionEnabled;
  }

  public void setOptionShortcutInterceptionEnabled(boolean enabled) {
    State updated = new State();
    updated.optionShortcutInterceptionEnabled = enabled;
    state = updated;
  }

  public static final class State {
    public boolean optionShortcutInterceptionEnabled = true;
  }
}
