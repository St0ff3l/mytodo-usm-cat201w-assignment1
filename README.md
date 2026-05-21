# Mytodo-Usm-Cat201w-Assignment1

A JavaFX-based Todo List application managed with Maven. This project helps users keep track of their tasks and lists.

## Features
- Manage tasks and lists seamlessly.
- Built using JavaFX 24.0.1 for the UI.
- Data handled via JSON formatting (using Jackson).

## Prerequisites
- Java 25 or compatible JDK.
- Maven (to handle dependencies and build the application).

## Building and Running
To compile the application, ensure `JAVA_HOME` is set correctly, then run:
```bash
mvn clean compile
```

To run the application via Maven:
```bash
mvn javafx:run
```

## Recent Updates
- **Security Fix**: Upgraded `com.fasterxml.jackson.core:jackson-core` (and related libraries) from `2.16.1` to `2.17.0` to resolve vulnerabilities `GHSA-72hv-8253-57qq` and `WS-2026-0003`.
