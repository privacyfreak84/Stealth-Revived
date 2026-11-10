#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
git -C "$ROOT_DIR" submodule update --init --recursive

if [[ ! -f "$ROOT_DIR/android/gradlew" ]]; then
    echo "Android source submodule is missing; initialise submodules first." >&2
    exit 1
fi

while IFS= read -r -d '' overlay_file; do
    relative_path="${overlay_file#"$ROOT_DIR/overlay/"}"
    install -D "$overlay_file" "$ROOT_DIR/android/$relative_path"
done < <(find "$ROOT_DIR/overlay" -type f -print0)

cd "$ROOT_DIR/android"
if (($# == 0)); then set -- assembleDebug; fi
exec ./gradlew --no-daemon "$@"
