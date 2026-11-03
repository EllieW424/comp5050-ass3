# Programming Assignment 3

## Run

Requires JDK 11. Set paths to the course files and local jars:

```bash
export PROVIDED_DIR=/path/to/provided
export SODIUM_JAR=/path/to/sodium.jar
export SWIDGET_JAR=/path/to/swidget.jar
export JUNIT_JAR=/path/to/junit.jar
export HAMCREST_JAR=/path/to/hamcrest-core.jar
./scripts/compile.sh
./scripts/test.sh
./scripts/run.sh
```

`run.sh` currently launches the course `Example`; select a compiled entry point with `MAIN_CLASS=...`.

## Done

Added GPS models and merged streams; tested ten altitude-free tracker positions.
