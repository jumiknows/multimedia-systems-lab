#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT_DIR="$PROJECT_DIR/out"

"$PROJECT_DIR/scripts/build.sh"

mkdir -p "$OUT_DIR/test"
find "$PROJECT_DIR/src/test/java" -name '*.java' -print | sort > "$OUT_DIR/test-sources.txt"

if command -v javac >/dev/null 2>&1; then
  COMPILER=(javac)
else
  COMPILER=(java -m jdk.compiler/com.sun.tools.javac.Main)
fi

"${COMPILER[@]}" --release 17 -encoding UTF-8 -cp "$OUT_DIR/main" -d "$OUT_DIR/test" @"$OUT_DIR/test-sources.txt"
java -Djava.awt.headless=true -cp "$OUT_DIR/main:$OUT_DIR/test" ca.ernestwong.multimedia.TestRunner
