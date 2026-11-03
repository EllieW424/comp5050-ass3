#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
BUILD_DIR="$REPO_DIR/out/classes"

: "${PROVIDED_DIR:?Set PROVIDED_DIR to the directory containing gps.dat}"
: "${SODIUM_JAR:?Set SODIUM_JAR to the Sodium jar}"
: "${SWIDGET_JAR:?Set SWIDGET_JAR to the Swidget jar}"

if [[ ! -d "$PROVIDED_DIR" || ! -f "$PROVIDED_DIR/gps.dat" ]]; then
    echo "PROVIDED_DIR must contain gps.dat: $PROVIDED_DIR" >&2
    exit 2
fi
for dependency in "$SODIUM_JAR" "$SWIDGET_JAR"; do
    if [[ ! -f "$dependency" ]]; then
        echo "Dependency jar not found: $dependency" >&2
        exit 2
    fi
done

"$SCRIPT_DIR/compile.sh"

JAVA_BIN="${JAVA_HOME:+$JAVA_HOME/bin/java}"
JAVA_BIN="${JAVA_BIN:-$(command -v java)}"
MAIN_CLASS="${MAIN_CLASS:-GpsGUI}"
main_path="${MAIN_CLASS//.//}.class"
if [[ ! -f "$BUILD_DIR/$main_path" ]]; then
    echo "Main class is not compiled: $MAIN_CLASS" >&2
    echo "Set MAIN_CLASS to a compiled application entry point." >&2
    exit 2
fi

classpath="$BUILD_DIR:$SODIUM_JAR:$SWIDGET_JAR"
cd "$PROVIDED_DIR"
exec "$JAVA_BIN" -cp "$classpath" "$MAIN_CLASS" "$@"
