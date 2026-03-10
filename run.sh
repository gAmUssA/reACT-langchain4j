#!/usr/bin/env bash
set -euo pipefail

if [ -z "${ANTHROPIC_API_KEY:-}" ]; then
  echo "ERROR: ANTHROPIC_API_KEY is not set."
  echo "Export it first:  export ANTHROPIC_API_KEY=sk-ant-..."
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# Build and resolve classpath
./gradlew -q jar 2>/dev/null
CP=$(./gradlew -q printClasspath 2>/dev/null)

exec java -cp "$CP" dev.demo.react.MainKt "$@"
