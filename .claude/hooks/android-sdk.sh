#!/usr/bin/env bash
# SessionStart hook: makes sure the Android SDK is available in Claude Code on the web
# sessions so the app can be built, tested and linted. Idempotent; a no-op locally.
set -euo pipefail

[ "${CLAUDE_CODE_REMOTE:-}" = "true" ] || exit 0

SDK="${ANDROID_HOME:-$HOME/android-sdk}"
PROJECT="${CLAUDE_PROJECT_DIR:-$(pwd)}"
TOOLS_ZIP="commandlinetools-linux-11076708_latest.zip"
PACKAGES=("platforms;android-35" "build-tools;35.0.0" "platform-tools")

if [ ! -x "$SDK/cmdline-tools/latest/bin/sdkmanager" ]; then
  tmp="$(mktemp -d)"
  curl -fsSL "https://dl.google.com/android/repository/$TOOLS_ZIP" -o "$tmp/tools.zip"
  unzip -q "$tmp/tools.zip" -d "$tmp"
  mkdir -p "$SDK/cmdline-tools"
  rm -rf "$SDK/cmdline-tools/latest"
  mv "$tmp/cmdline-tools" "$SDK/cmdline-tools/latest"
  rm -rf "$tmp"
fi

if [ ! -d "$SDK/platforms/android-35" ] || [ ! -d "$SDK/build-tools/35.0.0" ]; then
  yes | "$SDK/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$SDK" --licenses >/dev/null 2>&1 || true
  "$SDK/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$SDK" "${PACKAGES[@]}" >/dev/null
fi

# local.properties is gitignored; point Gradle at the SDK, keeping other entries (e.g. API keys).
props="$PROJECT/local.properties"
touch "$props"
grep -v '^sdk\.dir=' "$props" > "$props.tmp" || true
{ echo "sdk.dir=$SDK"; cat "$props.tmp"; } > "$props"
rm -f "$props.tmp"

if [ -n "${CLAUDE_ENV_FILE:-}" ]; then
  echo "export ANDROID_HOME=$SDK" >> "$CLAUDE_ENV_FILE"
fi
