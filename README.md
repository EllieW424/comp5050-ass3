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

`run.sh` launches `GpsGUI`; use `MAIN_CLASS=Example ./scripts/run.sh` for the course example.

## Done

Added GPS models, merged streams, ten altitude-free tracker positions, and the Swing dashboard layout.
