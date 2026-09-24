#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT_DIR="$PROJECT_DIR/out"

mkdir -p "$OUT_DIR/main"
find "$PROJECT_DIR/src/main/java" -name '*.java' -print | sort > "$OUT_DIR/main-sources.txt"

if command -v javac >/dev/null 2>&1; then
  COMPILER=(javac)
else
  COMPILER=(java -m jdk.compiler/com.sun.tools.javac.Main)
fi

"${COMPILER[@]}" --release 17 -encoding UTF-8 -d "$OUT_DIR/main" @"$OUT_DIR/main-sources.txt"

echo "Build complete: $OUT_DIR/main"
