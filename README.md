# AI Chat Focus

A small local IntelliJ IDEA plugin for JetBrains AI Chat.

It adds one action that:

- opens the current AI Chat tool window;
- focuses the chat input;
- keeps the current editor selection as chat context;
- refreshes a typed `@selection` reference when the editor selection changes;
- refreshes the selection again just before the message is sent.

It does not create a new chat and it does not send a prompt.

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
2. Search for **Open and Focus AI Chat**.
3. Assign `Control+\`.

The action id is `Local.AIChat.OpenAndFocusInput`.

## Use

Select code in the editor and press `Control+\`. The existing AI Chat thread stays open and the input gets focus. If the input contains `@selection`, the plugin re-creates the input document so that reference uses the current editor selection.
