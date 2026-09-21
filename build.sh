#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
IDEA_HOME="${IDEA_HOME:-/Applications/IntelliJ IDEA.app/Contents}"
USER_HOME="${HOME:?HOME is not set}"
ML_LLM_PLUGIN="${ML_LLM_PLUGIN:-$USER_HOME/Library/Application Support/JetBrains/IntelliJIdea2026.2/plugins/ml-llm}"
JAVAC="${JAVAC:-$IDEA_HOME/jbr/Contents/Home/bin/javac}"
FULL_LINE_JAR="${FULL_LINE_JAR:-$IDEA_HOME/plugins/fullLine/lib/fullLine.jar}"
BUILD_DIR="$SCRIPT_DIR/build/classes"
DIST_DIR="$SCRIPT_DIR/dist"

if [[ ! -x "$JAVAC" ]]; then
  printf 'javac not found: %s\n' "$JAVAC" >&2
  exit 1
fi

if [[ ! -f "$FULL_LINE_JAR" ]]; then
  printf 'fullLine.jar not found: %s\n' "$FULL_LINE_JAR" >&2
  exit 1
fi

if [[ ! -d "$ML_LLM_PLUGIN/lib" ]]; then
  printf 'AI Assistant plugin directory not found: %s\n' "$ML_LLM_PLUGIN" >&2
  exit 1
fi

rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR" "$DIST_DIR"

CLASSPATH="$IDEA_HOME/lib/*:$ML_LLM_PLUGIN/lib/*:$ML_LLM_PLUGIN/lib/modules/*:$FULL_LINE_JAR"

JAVA_SOURCES=()
while IFS= read -r source_file; do
  JAVA_SOURCES[${#JAVA_SOURCES[@]}]="$source_file"
done < <(find "$SCRIPT_DIR/src/main/java" -type f -name '*.java' -print | sort)
if [[ "${#JAVA_SOURCES[@]}" -eq 0 ]]; then
  printf 'No Java sources found.\n' >&2
  exit 1
fi

"$JAVAC" \
  -encoding UTF-8 \
  -source 17 \
  -target 17 \
  -cp "$CLASSPATH" \
  -d "$BUILD_DIR" \
  "${JAVA_SOURCES[@]}"

mkdir -p "$BUILD_DIR/META-INF"
cp "$SCRIPT_DIR/src/main/resources/META-INF/plugin.xml" "$BUILD_DIR/META-INF/plugin.xml"

rm -f "$DIST_DIR/ai-chat-focus.jar"
jar cf "$DIST_DIR/ai-chat-focus.jar" -C "$BUILD_DIR" .

printf 'Built %s\n' "$DIST_DIR/ai-chat-focus.jar"
