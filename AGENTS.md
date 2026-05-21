# OpenCode Agent Instructions

This file contains high-signal, repo-specific guidance for AI agents working in the mytodo-usm-cat201w-assignment1 project.

## Important
- **MANDATORY**: You MUST read this file at the beginning of EVERY coding session before making any changes to the codebase.

## Architecture & Conventions
- **Framework**: JavaFX 24 MVC with FXML. UI definitions live in `src/main/resources/com/mytodo/`, and controllers in `src/main/java/com/mytodo/`.
- **Data Persistence**: State is stored in the user's home directory under `.mytodo_app/`:
  - `~/.mytodo_app/tasks.json`: Task data persisted via `com.mytodo.util.JsonDataManager` using Jackson ObjectMapper with `JavaTimeModule` support (handles `LocalDate`/`LocalTime`).
  - `~/.mytodo_app/lists.json`: Custom list metadata, stored as plain text with format `name|iconPath` (one list per line). Parsed/written by `MainController.loadLists()` and `saveLists()`.
  - **Note**: Data files are NOT in the project root; use `MainController.getSafeDataFile()` helper to access them. To reset application state during testing, delete the `~/.mytodo_app/` directory.
- **Model Details**: 
  - `Task` class uses `listName` (String) field to associate tasks with custom lists (replaces older "tags" list concept).
  - Tasks are rendered in `taskList` ListView via `TaskListCell` custom cell factory. A hidden spacer task (`"(SPACER_ITEM)"`) is maintained at the end for UI stability and must never be serialized.
- **Dependencies**: Primary libraries include Jackson (JSON serialization), JavaFX (UI), and ControlsFX (advanced controls). All are declared in `pom.xml` and must have corresponding `requires` directives in `module-info.java`.
- **Java Module System (JPMS)**: This project is modularized (`src/main/java/module-info.java`). **Critical**: Whenever you add a new dependency to `pom.xml` (e.g., a new library), you *must* also add the corresponding `requires` directive to `module-info.java`.
- **Main Entrypoint**: `com.mytodo.Main`.

## Commands & Workflows
- **Build**: `mvn clean compile`
- **Run**: `mvn javafx:run` (Uses `javafx-maven-plugin`)
- **Java Version Requirements**: The project compiles to Java release 25. Ensure `JAVA_HOME` is appropriately set to JDK 25+ when running Maven commands from the terminal.

## Historical Agent Log
- *2026-04-09*: Upgraded `jackson-core` dependencies to `2.17.0` to resolve CVEs (GHSA-72hv-8253-57qq, WS-2026-0003). Migrated application data storage to `~/.mytodo_app/` (user home directory) for better platform compliance. Changed `lists.json` format to plain-text key-value pairs. Added `ControlsFX` dependency for enhanced UI controls.
