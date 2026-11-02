#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
BUILD_DIR="$REPO_DIR/out/classes"

: "${PROVIDED_DIR:?Set PROVIDED_DIR to the directory containing the course starter files}"
: "${SODIUM_JAR:?Set SODIUM_JAR to the Sodium jar}"
: "${SWIDGET_JAR:?Set SWIDGET_JAR to the Swidget jar}"

if [[ ! -d "$PROVIDED_DIR" ]]; then
    echo "PROVIDED_DIR is not a directory: $PROVIDED_DIR" >&2
    exit 2
fi
for dependency in "$SODIUM_JAR" "$SWIDGET_JAR"; do
    if [[ ! -f "$dependency" ]]; then
        echo "Dependency jar not found: $dependency" >&2
        exit 2
    fi
done

JAVA_BIN="${JAVA_HOME:+$JAVA_HOME/bin/java}"
JAVAC_BIN="${JAVA_HOME:+$JAVA_HOME/bin/javac}"
JAVA_BIN="${JAVA_BIN:-$(command -v java)}"
JAVAC_BIN="${JAVAC_BIN:-$(command -v javac)}"

java_version="$($JAVA_BIN -version 2>&1 | sed -n '1s/.*version "\([0-9]*\).*/\1/p')"
if [[ "$java_version" != "11" ]]; then
    echo "JDK 11 is required; selected java reports version ${java_version:-unknown}." >&2
    echo "Set JAVA_HOME to a JDK 11 installation." >&2
    exit 2
fi

rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR"

sources=()
while IFS= read -r -d '' source_file; do
    sources+=("$source_file")
done < <(find "$REPO_DIR/src" -type f -name '*.java' -print0)
while IFS= read -r -d '' source_file; do
    sources+=("$source_file")
done < <(find "$PROVIDED_DIR" -maxdepth 1 -type f -name '*.java' -print0)

if (( ${#sources[@]} == 0 )); then
    echo "No production or starter Java sources found; compilation skipped."
    exit 0
fi

"$JAVAC_BIN" --release 11 \
    -cp "$SODIUM_JAR:$SWIDGET_JAR" \
    -d "$BUILD_DIR" \
    "${sources[@]}"

echo "Compiled ${#sources[@]} Java source file(s) with JDK 11."
