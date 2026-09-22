# AI Chat Selection Sync

An IntelliJ IDEA plugin that keeps the current file-editor selection available in JetBrains AI Assistant chat and puts focus in the chat input.

## Features

- Adds an `@selection` reference to the current chat input when a file-editor selection is available.
- Refreshes the reference as the selection changes. Rapid selection changes are coalesced while you drag.
- Opens the AI Chat tool window if needed, then focuses its input.
- Keeps the current chat open. It does not start a new chat or send a prompt.
- Ignores selections made inside the AI Chat input.

## Requirements

- IntelliJ IDEA 2026.2 (build `262.10968.*`).
- The JetBrains AI Assistant plugin enabled in the IDE.

This plugin uses JetBrains AI Assistant classes that are not part of a stable public plugin API. A JetBrains AI Assistant update may require changes to this plugin.

## Build

The build script uses an installed IntelliJ IDEA distribution and the JetBrains AI Assistant plugin files.

```bash
./build.sh
```

The build creates:

- `dist/ai-chat-focus.jar`, for local installation.
- `dist/ai-chat-selection-sync.zip`, a plugin distribution archive.

The build script defaults to a macOS IntelliJ IDEA layout. To build from a different installation, set the paths it uses:

```bash
IDEA_HOME="/path/to/IntelliJ IDEA installation" \
ML_LLM_PLUGIN="/path/to/AI Assistant plugin" \
JAVAC="/path/to/javac" \
FULL_LINE_JAR="/path/to/fullLine.jar" \
./build.sh
```

## Install

1. Build the plugin.
2. In IntelliJ IDEA, open **Settings | Plugins**.
3. Select **Install Plugin from Disk** and choose `dist/ai-chat-focus.jar`.
4. Restart the IDE when prompted.

## Configure a shortcut

1. Open **Settings | Keymap**.
2. Search for **Focus AI Chat Input**.
3. Assign a shortcut of your choice.

The action ID is `Local.AIChat.OpenAndFocusInput`.

## Use

Select code in a file editor and invoke **Focus AI Chat Input**. The plugin opens the current AI Chat tool window if needed, refreshes the `@selection` reference, and focuses the chat input. If AI Chat is already open, it stays open and the input receives focus.

When you change the selection in the active file editor, the plugin refreshes the reference in the current chat. Selections made inside the AI Chat input do not trigger this update.

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE).
