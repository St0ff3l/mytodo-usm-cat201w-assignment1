package com.mytodo;

// ---------------------------------------------------------------------
// Imports
// ---------------------------------------------------------------------

// JavaFX core
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.Region;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.geometry.Pos;
import java.nio.file.Path;
import java.nio.file.Paths;

// Java Standard Library
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

// Project-specific classes
import com.mytodo.util.JsonDataManager;
import com.mytodo.SuccessMessageDialogController;
import com.mytodo.AddNewListDialogController;


/**
 * Main screen controller (MainController).
 * Responsible for handling all user interactions, data management, and UI updates.
 */
public class MainController {

    // ==== FXML bindings ====
    @FXML private VBox root;
    @FXML private VBox sidebar;
    @FXML private Button btnToday, btnImportant, btnAll, btnFinished, btnPending, btnOverdue;
    @FXML private VBox listContainer;
    @FXML private Button addNewListButton;
    @FXML private ListView<Task> taskList;
    @FXML private TextField searchField;
    @FXML private Button filterBtn;
    @FXML private Button searchClearBtn;
    @FXML private HBox floatingAddBox;
    @FXML private TextField quickAddField;
    @FXML private Button quickAddBtn;
    @FXML private Button detailAddBtn;

    // Top category number labels
    private Label todayCountLabel;
    private Label importantCountLabel;
    private Label allCountLabel;
    private Label pendingCountLabel;
    private Label overdueCountLabel;
    private Label completedCountLabel;

    private final ObservableList<Task> masterTasks = FXCollections.observableArrayList();
    // Store all custom lists (name + icon path)
    private final ObservableList<ListInfo> masterLists = FXCollections.observableArrayList();
    private final FilteredList<Task> filteredTasks = new FilteredList<>(masterTasks, t -> true);
    private String currentFilterType = "ALL";
    private String activeListFilter = null;

    private static final File DATA_FILE = new File("tasks.json");
    private static final File LISTS_DATA_FILE = new File("lists.json");
    private final JsonDataManager dataManager = new JsonDataManager();
    private final LocalTime DEFAULT_END_OF_DAY_TIME = LocalTime.of(23, 59);
    private static final String SPACER_TITLE = "(SPACER_ITEM)";

    // ============== [FIX 2: add safe path configuration and method] ==============
    private static final String APP_DIR = ".mytodo_app"; // Hidden configuration folder
    private static final String TASKS_FILE_NAME = "tasks.json";
    private static final String LISTS_FILE_NAME = "lists.json";

    /**
     * Get a safe, writable File object under the user's home directory
     */
    private static File getSafeDataFile(String fileName) {
        // 1. Get user home directory (e.g., /Users/stoffel)
        String homeDir = System.getProperty("user.home");

        // 2. Build data directory (e.g., /Users/stoffel/.mytodo_app)
        File dataDir = Paths.get(homeDir, APP_DIR).toFile();

        // 3. Ensure directory exists (this is important!)
        if (!dataDir.exists()) {
            dataDir.mkdirs(); // Create directory
            System.out.println("[DEBUG] Created persistent data directory: " + dataDir.getAbsolutePath());
        }

        // 4. Return final file path
        return Paths.get(dataDir.getAbsolutePath(), fileName).toFile();
    }


    // =========================================================================
    // 4. Initialization
    // =========================================================================

    @FXML
    private void initialize() {
        System.out.println("[DEBUG] MainController initializing...");
        // Icon management (if you had it before)

        // Load lists first, then load tasks
        loadLists();
        try {
            loadTasks();
            System.out.println("[DEBUG] Tasks loaded. Count: " + masterTasks.size());
        } catch (Exception ex) {
            System.err.println("[ERROR] loadTasks failed during initialization: " + ex.getMessage());
            ex.printStackTrace();
        }

        // Ensure the ghost spacer item exists
        ensureSpacerExists();

        // ListView binding
        taskList.setItems(filteredTasks);
        taskList.setCellFactory(list -> new TaskListCell(this));
        VBox.setVgrow(taskList, Priority.ALWAYS);
        HBox.setHgrow(taskList, Priority.ALWAYS);

        // Wrap top category buttons as "icon + text + right-side number"
        setupFixedCategoryButtons();

        // Bind various events
        bindActionEvents();

        // Update list area + category statistics
        updateFixedCategoryCounts();
        updateListSidebar();

        // Select "All" by default
        setNavFilter("ALL", btnAll);

        System.out.println("[DEBUG] Initialization complete.");
    }

    /**
     * Top 6 category buttons, unified into:
     * [icon] [title] ....... [count]
     */
    private void setupFixedCategoryButtons() {
        todayCountLabel     = buildNavButtonWithCount(btnToday,     "Today");
        importantCountLabel = buildNavButtonWithCount(btnImportant, "Important");
        allCountLabel       = buildNavButtonWithCount(btnAll,       "All");
        pendingCountLabel   = buildNavButtonWithCount(btnPending,   "Pending");
        overdueCountLabel   = buildNavButtonWithCount(btnOverdue,   "Overdue");
        completedCountLabel = buildNavButtonWithCount(btnFinished,  "Completed");
    }

    /**
     * Transform one Button into:
     *  [icon] [title] (spacer) [countLabel]
     */
    private Label buildNavButtonWithCount(Button btn, String title) {
        if (btn == null) return null;

        Node icon = btn.getGraphic();   // ImageView already put in FXML
        btn.setText("");                // Do not use the Button's own text

        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);

        if (icon != null) {
            row.getChildren().add(icon);
        }

        Label titleLabel = new Label(title);
        // Optionally add a CSS class
        titleLabel.getStyleClass().add("nav-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label countLabel = new Label("0");
        countLabel.getStyleClass().add("list-count"); // Style via CSS

        row.getChildren().addAll(titleLabel, spacer, countLabel);
        btn.setGraphic(row);

        return countLabel;
    }

    /**
     * Helper method: centralize all event bindings for FXML elements.
     */
    private void bindActionEvents() {
        if (searchField != null) searchField.setOnAction(e -> performSearch());
        if (filterBtn != null) filterBtn.setOnAction(e -> performSearch());
        if (searchClearBtn != null) {
            searchClearBtn.setOnAction(e -> {
                searchField.clear();
                applyFilters();
                taskList.refresh();
                System.out.println("[DEBUG] Search cleared.");
            });
        }
        if (btnAll != null)       btnAll.setOnAction(e -> setNavFilter("ALL", btnAll));
        if (btnToday != null)     btnToday.setOnAction(e -> setNavFilter("TODAY", btnToday));
        if (btnImportant != null) btnImportant.setOnAction(e -> setNavFilter("IMPORTANT", btnImportant));
        if (btnFinished != null)  btnFinished.setOnAction(e -> setNavFilter("FINISHED", btnFinished));
        if (btnPending != null)   btnPending.setOnAction(e -> setNavFilter("PENDING", btnPending));
        if (btnOverdue != null)   btnOverdue.setOnAction(e -> setNavFilter("OVERDUE", btnOverdue));

        if (quickAddBtn != null)   quickAddBtn.setOnAction(e -> addQuickTask());
        if (quickAddField != null) quickAddField.setOnAction(e -> addQuickTask());
        if (detailAddBtn != null)  detailAddBtn.setOnAction(e -> openTaskDetailDialog(null));
        if (addNewListButton != null) addNewListButton.setOnAction(e -> handleAddNewList());
    }


    // =========================================================================
    // 5. Core task operations (Create / Delete / Update)
    // =========================================================================

    /**
     * Quick add task (from floating bar at the bottom)
     */
    private void addQuickTask() {
        String text = quickAddField.getText();
        if (text == null || text.isBlank()) return;

        int insertPos = Math.max(0, masterTasks.size() - 1);
        Task task = new Task(
                text.trim(), "", LocalDate.now(), DEFAULT_END_OF_DAY_TIME, "Normal"
        );

        if ("LIST".equals(currentFilterType) && activeListFilter != null) {
            task.setListName(activeListFilter);
        }

        masterTasks.add(insertPos, task);
        quickAddField.clear();
        saveTasks();
        applyFilters();
        taskList.refresh();
        updateFixedCategoryCounts();
        updateListSidebar();
    }

    /**
     * [PUBLIC] Delete a task.
     */
    public void deleteTask(Task task) {
        if (task == null || SPACER_TITLE.equals(task.getTitle())) return;

        ButtonType confirmResult = showCustomAlert(
                "Delete Confirmation",
                "Are you sure to delete: " + task.getTitle() + " ?",
                "This action cannot be undone."
        );

        if (confirmResult == ButtonType.OK) {
            masterTasks.remove(task);
            saveTasks();
            applyFilters();
            taskList.refresh();
            updateFixedCategoryCounts();
            updateListSidebar();
            System.out.println("[DEBUG] Task deleted: " + task.getTitle());
        }
    }

    /**
     * [PUBLIC] Toggle completion status for a task.
     */
    public void toggleCompletion(Task task) {
        if (task == null || SPACER_TITLE.equals(task.getTitle())) return;
        task.setCompleted(!task.isCompleted());
        saveTasks();
        applyFilters();
        taskList.refresh();
        updateFixedCategoryCounts();
        updateListSidebar();
    }

    /**
     * [PUBLIC] Open task detail dialog (for add or edit).
     */
    public void openTaskDetailDialog(Task taskToEdit) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mytodo/TaskDetailDialog.fxml"));
            DialogPane pane = loader.load();
            TaskDetailController controller = loader.getController();

            // Pass masterLists to the dialog
            controller.loadData(taskToEdit, masterLists);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle(taskToEdit == null ? "Add Task" : "Edit Task");
            dialog.setDialogPane(pane);
            pane.getButtonTypes().clear();
            dialog.getDialogPane().getStylesheets().add(getClass().getResource("/com/mytodo/Main.css").toExternalForm());
            dialog.showAndWait();

            if (controller.isOkClicked()) {
                Task updatedTask = controller.getTask();
                if (updatedTask != null) {
                    String msg = (taskToEdit == null) ? "Task added: " : "Task updated: ";
                    showSuccessAlert(msg + updatedTask.getTitle(), null);

                    if (taskToEdit == null) {
                        int insertPos = Math.max(0, masterTasks.size() - 1);
                        masterTasks.add(insertPos, updatedTask);
                    } else {
                        taskList.refresh();
                    }
                    saveTasks();
                    applyFilters();
                    taskList.refresh();
                    updateFixedCategoryCounts();
                    updateListSidebar();
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            showCustomAlert("Error", "Unexpected error", "Failed to open task dialog: " + ex.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
            showCustomAlert("Error", "Unexpected error", "Unexpected error: " + ex.getMessage());
        }
    }


    // =========================================================================
    // 6. Alerts & dialog management
    // =========================================================================

    private void showSuccessAlert(String header, String content) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mytodo/SuccessMessageDialogView.fxml"));
            DialogPane pane = loader.load();
            SuccessMessageDialogController controller = loader.getController();
            controller.setSuccessMessage(header, content);
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Success");
            dialog.setDialogPane(pane);
            pane.getButtonTypes().clear();
            dialog.showAndWait();
        } catch (IOException ex) {
            ex.printStackTrace();
            Alert fallback = new Alert(AlertType.INFORMATION, content);
            fallback.setTitle("Success");
            fallback.setHeaderText(header);
            fallback.showAndWait();
        }
    }

    private ButtonType showCustomAlert(String title, String header, String content) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mytodo/CustomAlertDialogView.fxml"));
            DialogPane pane = loader.load();
            CustomAlertController controller = loader.getController();
            controller.setMessage(header, content);
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle(title);
            dialog.setDialogPane(pane);
            pane.getButtonTypes().clear();
            dialog.showAndWait();
            return controller.getResult();
        } catch (IOException ex) {
            ex.printStackTrace();
            Alert fallback = new Alert(AlertType.ERROR, "Failed to load custom dialog: " + ex.getMessage());
            fallback.showAndWait();
            return ButtonType.CANCEL;
        }
    }

    /**
     * Create a new list using a custom FXML dialog (with icon selection)
     */
    @FXML
    private void handleAddNewList() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mytodo/AddNewListDialogView.fxml"));
            DialogPane pane = loader.load();
            pane.getStylesheets().add(getClass().getResource("/com/mytodo/Main.css").toExternalForm());

            AddNewListDialogController controller = loader.getController();

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("New List");
            dialog.setDialogPane(pane);
            pane.getButtonTypes().clear(); // Use our own OK/Cancel buttons
            dialog.showAndWait();

            if (controller.isOkClicked()) {
                String newName  = controller.getNewListName();
                String iconPath = controller.getSelectedIconPath();

                boolean exists = masterLists.stream()
                        .anyMatch(li -> li.getName().equalsIgnoreCase(newName));
                if (exists) {
                    showCustomAlert("Error", "List already exists.", "A list with this name already exists.");
                    return;
                }

                ListInfo info = new ListInfo(newName, iconPath);
                masterLists.add(info);

                saveLists();
                updateListSidebar();
                System.out.println("[DEBUG] New list added: " + info);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            showCustomAlert("Error", "Load Error", "Failed to load the 'Add New List' dialog.");
        }
    }


    // =========================================================================
    // 7. Filtering & search logic
    // =========================================================================

    private void performSearch() {
        applyFilters();
        System.out.println("[DEBUG] performSearch done. results=" + filteredTasks.size());
    }

    private void setNavFilter(String filterType, Button selectedButton) {
        activeListFilter = null;
        currentFilterType = filterType;
        clearAllSidebarSelections();
        if (selectedButton != null) {
            selectedButton.getStyleClass().add("selected");
        }
        applyFilters();
    }

    private void setListFilter(String listName, Button selectedButton) {
        currentFilterType = "LIST";
        activeListFilter = listName;
        clearAllSidebarSelections();
        if (selectedButton != null) {
            selectedButton.getStyleClass().add("selected");
        }
        applyFilters();
        System.out.println("[DEBUG] List filter set: " + listName);
    }

    private void clearAllSidebarSelections() {
        sidebar.getChildren().stream()
                .filter(node -> node instanceof Button)
                .map(node -> (Button) node)
                .forEach(btn -> btn.getStyleClass().remove("selected"));

        if (listContainer != null) {
            listContainer.getChildren().stream()
                    .filter(node -> node instanceof Button)
                    .map(node -> (Button) node)
                    .forEach(btn -> btn.getStyleClass().remove("selected"));
        }
    }

    private void applyFilters() {
        String searchText = (searchField != null && searchField.getText() != null)
                ? searchField.getText().toLowerCase().trim() : "";
        filteredTasks.setPredicate(task -> {
            if (task == null) return false;
            if (SPACER_TITLE.equals(task.getTitle())) return true;
            if (!isNavFilterMatch(task)) return false;
            if (searchText.isEmpty()) return true;
            String title = (task.getTitle() == null) ? "" : task.getTitle().toLowerCase();
            String desc = (task.getDescription() == null) ? "" : task.getDescription().toLowerCase();
            return title.contains(searchText) || desc.contains(searchText);
        });
        System.out.println("[DEBUG] applyFilters -> " + currentFilterType + " search='" + searchText + "' remaining=" + filteredTasks.size());
    }

    /**
     * Navigation filter logic + Overdue
     */
    private boolean isNavFilterMatch(Task task) {
        if (SPACER_TITLE.equals(task.getTitle())) return true;

        LocalDate today = LocalDate.now();
        boolean isToday   = task.getDueDate() != null && task.getDueDate().isEqual(today);
        boolean isOverdue = task.getDueDate() != null
                && task.getDueDate().isBefore(today)
                && !task.isCompleted();

        switch (currentFilterType) {
            case "TODAY":     return isToday;
            case "IMPORTANT": return task.isImportant();
            case "FINISHED":  return task.isCompleted();
            case "PENDING":   return !task.isCompleted();
            case "OVERDUE":   return isOverdue;
            case "LIST":
                if (activeListFilter == null) return true;
                return activeListFilter.equals(task.getListName());
            case "ALL":
            default:
                return true;
        }
    }


    // =========================================================================
    // 8. Data persistence (Load / Save)
    // =========================================================================

    private void loadTasks() {
        // ============== [FIX 3: use safe path] ==============
        File dataFile = getSafeDataFile(TASKS_FILE_NAME);
        try {
            var loaded = dataManager.load(dataFile);
            if (loaded != null) {
                masterTasks.addAll(loaded);
            }
        } catch (Exception ex) {
            System.err.println("[ERROR] dataManager.load failed: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void saveTasks() {
        // ============== [FIX 4: use safe path] ==============
        File dataFile = getSafeDataFile(TASKS_FILE_NAME);
        try {
            var toSaveList = masterTasks.stream()
                    .filter(t -> t != null && !SPACER_TITLE.equals(t.getTitle()))
                    .collect(Collectors.toList());
            ObservableList<Task> toSave = FXCollections.observableArrayList(toSaveList);
            dataManager.save(dataFile, toSave);
            System.out.println("[DEBUG] Tasks saved. Count: " + toSave.size());
        } catch (Exception ex) {
            System.err.println("[ERROR] dataManager.save failed: " + ex.getMessage());
            ex.printStackTrace();
            showCustomAlert("Save Error", "Failed to save tasks", "Your changes might be lost. Error: " + ex.getMessage());
        }
    }

    private void ensureSpacerExists() {
        masterTasks.removeIf(t -> t != null && SPACER_TITLE.equals(t.getTitle()));
        Task spacer = new Task(SPACER_TITLE, "", null, null, "Normal");
        masterTasks.add(spacer);
    }

    /**
     * Load custom lists from lists.json (each line: name|iconPath)
     */
    private void loadLists() {
        // ============== [FIX 5: use safe path] ==============
        Path listPath = getSafeDataFile(LISTS_FILE_NAME).toPath();
        if (!Files.exists(listPath)) {
            System.out.println("[DEBUG] lists.json not found in persistent location. No lists loaded.");
            return;
        }

        try {
            List<String> lines = Files.readAllLines(listPath);
            masterLists.clear();

            for (String line : lines) {
                if (line == null || line.isBlank()) continue;
                String[] parts = line.split("\\|", 2);
                String name = parts[0];
                String iconPath = (parts.length > 1 && !parts[1].isBlank()) ? parts[1] : null;
                masterLists.add(new ListInfo(name, iconPath));
            }

            System.out.println("[DEBUG] Lists loaded from lists.json. Count: " + masterLists.size());
        } catch (IOException e) {
            System.err.println("[ERROR] Failed to load lists.json: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Save lists.json: one list per line -> name|iconPath
     */
    private void saveLists() {
        // ============== [FIX 6: use safe path] ===============
        Path listPath = getSafeDataFile(LISTS_FILE_NAME).toPath();
        try {
            List<String> lines = masterLists.stream()
                    .map(li -> li.getName() + "|" + (li.getIconPath() == null ? "" : li.getIconPath()))
                    .collect(Collectors.toList());

            Files.write(listPath, lines);
            System.out.println("[DEBUG] Lists saved to lists.json.");
        } catch (IOException e) {
            System.err.println("[ERROR] Failed to save lists.json: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Update the left LISTS area (use ListInfo: icon + name + right-side count)
     */
    private void updateListSidebar() {
        if (listContainer == null) {
            System.err.println("[ERROR] listContainer is null. Cannot update list.");
            return;
        }

        listContainer.getChildren().clear();

        for (ListInfo li : masterLists) {
            Button listButton = new Button();
            listButton.setMaxWidth(Double.MAX_VALUE);
            listButton.getStyleClass().add("nav-item");

            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);

            // Icon
            if (li.getIconPath() != null && !li.getIconPath().isBlank()) {
                try {
                    var url = getClass().getResource(li.getIconPath());
                    if (url != null) {
                        ImageView iconView = new ImageView(new Image(url.toExternalForm()));
                        iconView.setFitWidth(18);
                        iconView.setFitHeight(18);
                        iconView.setPreserveRatio(true);
                        row.getChildren().add(iconView);
                    }
                } catch (Exception ex) {
                    System.err.println("[WARN] Failed to load icon for list: " + li + " -> " + ex.getMessage());
                }
            }

            // Name
            Label nameLabel = new Label(li.getName());

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            // Count
            int count = getTaskCountForList(li.getName());
            Label countLabel = new Label(String.valueOf(count));
            countLabel.getStyleClass().add("list-count");

            row.getChildren().addAll(nameLabel, spacer, countLabel);
            listButton.setGraphic(row);

            listButton.setOnAction(event -> setListFilter(li.getName(), listButton));

            // Context menu: delete list
            ContextMenu contextMenu = new ContextMenu();
            MenuItem deleteItem = new MenuItem("Delete List");
            deleteItem.setOnAction(event -> deleteList(li));
            contextMenu.getItems().add(deleteItem);
            listButton.setContextMenu(contextMenu);

            listContainer.getChildren().add(listButton);
        }

        System.out.println("[DEBUG] List sidebar updated. Found " + masterLists.size() + " lists.");
    }

    private int getTaskCountForList(String listName) {
        int count = 0;
        for (Task t : masterTasks) {
            if (t == null || SPACER_TITLE.equals(t.getTitle())) continue;
            if (listName.equals(t.getListName())) {
                count++;
            }
        }
        return count;
    }

    /**
     * Delete a custom list
     */
    private void deleteList(ListInfo listInfo) {
        String listName = listInfo.getName();

        ButtonType confirmResult = showCustomAlert(
                "Delete List",
                "Are you sure to delete the list: " + listName + "?",
                "All tasks in this list will be moved to 'Unlisted'."
        );

        if (confirmResult != ButtonType.OK) {
            return;
        }

        masterLists.remove(listInfo);

        for (Task task : masterTasks) {
            if (listName.equals(task.getListName())) {
                task.setListName(null);
            }
        }

        saveLists();
        saveTasks();
        updateListSidebar();

        if (listName.equals(activeListFilter)) {
            setNavFilter("ALL", btnAll);
        } else {
            applyFilters();
        }

        System.out.println("[DEBUG] List deleted: " + listName);
    }


    // === Top category number statistics ===

    private boolean isRealTask(Task t) {
        return t != null && !SPACER_TITLE.equals(t.getTitle());
    }

    private void updateFixedCategoryCounts() {
        LocalDate today = LocalDate.now();

        int allCount = 0;
        int todayCount = 0;
        int importantCount = 0;
        int pendingCount = 0;
        int overdueCount = 0;
        int finishedCount = 0;

        for (Task t : masterTasks) {
            if (!isRealTask(t)) continue;

            allCount++;

            if (t.getDueDate() != null && t.getDueDate().isEqual(today)) {
                todayCount++;
            }
            if (t.isImportant()) {
                importantCount++;
            }
            if (t.isCompleted()) {
                finishedCount++;
            } else {
                pendingCount++;
            }
            if (t.getDueDate() != null && t.getDueDate().isBefore(today) && !t.isCompleted()) {
                overdueCount++;
            }
        }

        if (todayCountLabel != null)     todayCountLabel.setText(String.valueOf(todayCount));
        if (importantCountLabel != null) importantCountLabel.setText(String.valueOf(importantCount));
        if (allCountLabel != null)       allCountLabel.setText(String.valueOf(allCount));
        if (pendingCountLabel != null)   pendingCountLabel.setText(String.valueOf(pendingCount));
        if (overdueCountLabel != null)   overdueCountLabel.setText(String.valueOf(overdueCount));
        if (completedCountLabel != null) completedCountLabel.setText(String.valueOf(finishedCount));


        // ============================================================
        // Number colors — fully aligned with icon colors
        // ============================================================

        if (todayCountLabel != null)
            todayCountLabel.setStyle("-fx-text-fill: #FFCC00;");     // Today yellow

        if (importantCountLabel != null)
            importantCountLabel.setStyle("-fx-text-fill: #AF52DE;"); // Important purple

        if (allCountLabel != null)
            allCountLabel.setStyle("-fx-text-fill: #007AFF;");       // All blue

        if (pendingCountLabel != null)
            pendingCountLabel.setStyle("-fx-text-fill: #FF3B30;");   // Pending red

        if (overdueCountLabel != null)
            overdueCountLabel.setStyle("-fx-text-fill: #FFCC00;");   // Overdue yellow

        if (completedCountLabel != null)
            completedCountLabel.setStyle("-fx-text-fill: #8E8E93;"); // Completed gray
    }


    // =========================================================================
    // 9. FXML event handlers (menu bar & shortcuts)
    // =========================================================================

    @FXML private void handleExit() {
        saveAndExit();
    }

    @FXML
    private void handleDeleteCompleted() {
        ButtonType confirmResult = showCustomAlert(
                "Clear Completed Tasks",
                "Delete all completed tasks?",
                "This cannot be undone."
        );

        if (confirmResult == ButtonType.OK) {
            masterTasks.removeIf(t -> t != null && t.isCompleted() && !SPACER_TITLE.equals(t.getTitle()));
            applyFilters();
            saveTasks();
            taskList.refresh();
            updateFixedCategoryCounts();
            updateListSidebar();
            System.out.println("[DEBUG] All completed tasks deleted.");
        }
    }

    @FXML
    private void handleToggleTheme() {
        Scene scene = root.getScene();
        if (scene == null) return;

        String gradientPath = getClass().getResource("/com/mytodo/Main.css").toExternalForm();
        if (scene.getStylesheets().contains(gradientPath)) {
            scene.getStylesheets().remove(gradientPath);
            System.out.println("[UI] Switched to Classic Theme (Default JavaFX)");
        } else {
            scene.getStylesheets().add(gradientPath);
            System.out.println("[UI] Switched to Custom Theme (Main.css)");
        }
    }

    @FXML
    private void handleHelp() {
        if (root != null && root.getScene() != null && root.getScene().getWindow() != null) {
            AboutDialogController.showAboutDialog(root.getScene().getWindow());
        } else {
            Alert tempAlert = new Alert(AlertType.INFORMATION);
            tempAlert.setTitle("About");
            tempAlert.setHeaderText(null);
            tempAlert.setContentText("mytodo-usm-cat201w-assignment1 v1.0");
            tempAlert.showAndWait();
        }
    }

    @FXML
    public void saveAndExit() {
        System.out.println("[DEBUG] Save and Exit requested...");
        try {
            saveTasks();
            saveLists();
            Platform.exit();
            System.exit(0);
        } catch (Exception e) {
            Alert errorAlert = new Alert(AlertType.ERROR);
            errorAlert.setTitle("Error");
            errorAlert.setHeaderText("Failed to save tasks on exit");
            errorAlert.setContentText("Error: " + e.getMessage());
            errorAlert.showAndWait();
            System.exit(1);
        }
    }

    // --- FXML shortcuts (used by SceneBuilder 'onAction') ---
    @FXML public void onQuickAdd()       { addQuickTask(); }
    @FXML public void onAddDetails()     { openTaskDetailDialog(null); }
    @FXML public void onSearchClicked()  { performSearch(); }
    @FXML public void onClearSearch()    {
        if (searchField != null) searchField.clear();
        applyFilters();
    }
    @FXML public void onFilterToday()     { setNavFilter("TODAY",    btnToday); }
    @FXML public void onFilterImportant() { setNavFilter("IMPORTANT",btnImportant); }
    @FXML public void onFilterAll()       { setNavFilter("ALL",      btnAll); }
    @FXML public void onFilterPending()   { setNavFilter("PENDING",  btnPending); }
    @FXML public void onFilterFinished()  { setNavFilter("FINISHED", btnFinished); }
    @FXML public void onFilterOverdue()   { setNavFilter("OVERDUE",  btnOverdue); }
}
