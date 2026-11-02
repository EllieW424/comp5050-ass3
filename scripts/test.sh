#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
BUILD_DIR="$REPO_DIR/out/classes"
TEST_BUILD_DIR="$REPO_DIR/out/test-classes"

: "${JUNIT_JAR:?Set JUNIT_JAR to the JUnit 4 jar}"
: "${HAMCREST_JAR:?Set HAMCREST_JAR to the Hamcrest jar}"

for dependency in "$JUNIT_JAR" "$HAMCREST_JAR"; do
    if [[ ! -f "$dependency" ]]; then
        echo "Dependency jar not found: $dependency" >&2
        exit 2
    fi
done

"$SCRIPT_DIR/compile.sh"

JAVA_BIN="${JAVA_HOME:+$JAVA_HOME/bin/java}"
JAVAC_BIN="${JAVA_HOME:+$JAVA_HOME/bin/javac}"
JAVA_BIN="${JAVA_BIN:-$(command -v java)}"
JAVAC_BIN="${JAVAC_BIN:-$(command -v javac)}"

rm -rf "$TEST_BUILD_DIR"
mkdir -p "$TEST_BUILD_DIR"

test_sources=()
while IFS= read -r -d '' source_file; do
    test_sources+=("$source_file")
done < <(find "$REPO_DIR/test" -type f -name '*.java' -print0)

if (( ${#test_sources[@]} == 0 )); then
    echo "No test Java sources found; tests skipped."
    exit 0
fi

test_classpath="$BUILD_DIR:$SODIUM_JAR:$SWIDGET_JAR:$JUNIT_JAR:$HAMCREST_JAR"
"$JAVAC_BIN" --release 11 \
    -cp "$test_classpath" \
    -d "$TEST_BUILD_DIR" \
    "${test_sources[@]}"

test_classes=()
while IFS= read -r -d '' test_file; do
    class_name="$(basename "$test_file" .java)"
    package_name="$(sed -nE 's/^[[:space:]]*package[[:space:]]+([[:alnum:]_.]+)[[:space:]]*;.*/\1/p' "$test_file" | head -n 1)"
    if [[ -n "$package_name" ]]; then
        class_name="$package_name.$class_name"
    fi
    test_classes+=("$class_name")
done < <(find "$REPO_DIR/test" -type f -name '*_Test.java' -print0)

if (( ${#test_classes[@]} == 0 )); then
    echo "Compiled ${#test_sources[@]} test source file(s), but found no *_Test.java classes; tests skipped."
    exit 0
fi

"$JAVA_BIN" -cp "$TEST_BUILD_DIR:$test_classpath" \
    org.junit.runner.JUnitCore "${test_classes[@]}"
