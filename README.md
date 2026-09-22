# AI Chat Selection Sync

A small local IntelliJ IDEA plugin that keeps selected file-editor text in JetBrains AI Chat and manages the chat view.

It:

- adds an `@selection` reference when you select text in a file editor;
- refreshes that reference when the file editor selection changes;
- toggles the current AI Chat tool window with `Control+\`;
- focuses the chat input when it opens;
- ignores selections made inside the AI Chat input;
- removes stale selection attachments left by version 1.0.0.

It does not create a new chat and it does not send a prompt.

The plugin only treats selections in the file editor as source selections. It leaves JetBrains AI Assistant's input filter in place.

## Why a plugin is needed

The built-in actions and keymaps each miss part of this workflow in IntelliJ IDEA 2026.2:

| Built-in option | What it does | What is missing |
| --- | --- | --- |
| `AIAssistant.ToolWindow.ShowOrFocus` | Opens or focuses AI Chat | In a new or empty chat, focus can land on **Open Chat in Editor**, not the input box. |
| `AIAssistantAddToChatAction` | Adds the selected editor text to the current chat | It leaves focus in the code editor, so a second action is needed before typing. |
| `AIAssistantAskInChatAction` | Opens a composer and focuses it with the selection | It starts a new chat instead of continuing the current thread. |
| A macro or keymap chain | Copies text, opens chat, and pastes it | It pastes a snapshot into the prompt and does not solve the focus problem. |

`@selection` is represented by a selection attachment after it is inserted. The visible attachment can keep the original range when the editor selection changes. This plugin adds the token when the editor has a selection, removes the old manual selection attachment, and recreates the same input document, so the token points to the current editor range. It coalesces quick selection events while dragging, so one change produces one chip.

## Requirements

- IntelliJ IDEA 2026.2, build `262.10968.*`.
- JetBrains AI Assistant enabled and signed in.
- macOS installation at `/Applications/IntelliJ IDEA.app` by default.

The plugin uses JetBrains AI Assistant classes that are not a stable public plugin API. A future AI Assistant update may require a source change.

## Build

Run:

```bash
./build.sh
```

The jar is written to `dist/ai-chat-focus.jar`.

You can point the build at another IntelliJ installation with:

```bash
IDEA_HOME="/Applications/IntelliJ IDEA.app/Contents" ./build.sh
```

## Install

Copy `dist/ai-chat-focus.jar` into the IDE plugin directory, then restart IntelliJ.

For the default IntelliJ 2026.2 macOS setup:

```bash
mkdir -p "$HOME/Library/Application Support/JetBrains/IntelliJIdea2026.2/plugins/ai-chat-focus/lib"
cp dist/ai-chat-focus.jar "$HOME/Library/Application Support/JetBrains/IntelliJIdea2026.2/plugins/ai-chat-focus/lib/"
```

## Keymap

In IntelliJ:

1. Open Settings, Keymap.
2. Search for **Toggle AI Chat**.
3. Assign `Control+\`.

The action id is `Local.AIChat.OpenAndFocusInput`.

## Use

Select code in the editor and press `Control+\`. If AI Chat is open, the shortcut hides it. If it is hidden, the shortcut shows the current thread, focuses the input, and keeps one `@selection` reference for the selected code. Type your prompt after it. If you change the file editor selection later, the plugin refreshes that reference.
