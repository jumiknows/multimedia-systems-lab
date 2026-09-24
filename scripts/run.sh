#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

"$PROJECT_DIR/scripts/build.sh"
java -cp "$PROJECT_DIR/out/main" ca.ernestwong.multimedia.MultimediaLabApp

