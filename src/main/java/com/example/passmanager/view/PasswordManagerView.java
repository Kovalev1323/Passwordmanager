package com.example.passmanager.view;

import com.example.passmanager.model.PasswordEntry;
import com.example.passmanager.model.PasswordStrength;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * JavaFX-представление менеджера паролей.
 */
public final class PasswordManagerView {

    private final ObservableList<PasswordEntry> entries = FXCollections.observableArrayList();

    private final TableView<PasswordEntry> tableView = new TableView<>(entries);
    private final TextField serviceField = new TextField();
    private final TextField loginField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final TextField visiblePasswordField = new TextField();
    private final TextArea notesArea = new TextArea();
    private final Spinner<Integer> lengthSpinner = new Spinner<>(6, 32, 12);
    private final Label passwordStrengthLabel = new Label("Надежность: —");

    private Consumer<Integer> onGenerate = length -> {};
    private Consumer<String> onDelete = id -> {};
    private QuadConsumer<String, String, String, String> onAdd = (service, login, password, notes) -> {};
    private Runnable onRefresh = () -> {};
    private Consumer<String> onPasswordInput = text -> {};
    private Consumer<Path> onImport = path -> {};
    private Consumer<Path> onExport = path -> {};

    private PasswordEntry selected;

    public PasswordManagerView(Stage stage) {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        root.setCenter(buildTable());
        root.setRight(buildForm());
        root.setTop(buildToolbar());

        Scene scene = new Scene(root, 1000, 540);
        stage.setTitle("Password Manager");
        stage.setScene(scene);
        stage.show();
    }

    private ToolBar buildToolbar() {
        Button refreshButton = new Button("Обновить");
        refreshButton.setOnAction(event -> onRefresh.run());

        Button deleteButton = new Button("Удалить выбранную");
        deleteButton.setOnAction(event -> {
            if (selected != null) {
                onDelete.accept(selected.getId());
            } else {
                showError("Сначала выберите запись");
            }
        });

        Button importButton = new Button("Загрузить из файла");
        importButton.setOnAction(event -> chooseFile(true).ifPresent(onImport));

        Button exportButton = new Button("Сохранить в файл");
        exportButton.setOnAction(event -> chooseFile(false).ifPresent(onExport));

        return new ToolBar(refreshButton, new Separator(), deleteButton, new Separator(), importButton, exportButton);
    }

    private TableView<PasswordEntry> buildTable() {
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        TableColumn<PasswordEntry, String> serviceCol = new TableColumn<>("Сервис");
        serviceCol.setCellValueFactory(new PropertyValueFactory<>("service"));

        TableColumn<PasswordEntry, String> loginCol = new TableColumn<>("Логин");
        loginCol.setCellValueFactory(new PropertyValueFactory<>("username"));

        TableColumn<PasswordEntry, String> passwordCol = new TableColumn<>("Пароль");
        passwordCol.setCellValueFactory(new PropertyValueFactory<>("password"));

        TableColumn<PasswordEntry, String> createdCol = new TableColumn<>("Создано");
        createdCol.setCellValueFactory(cell -> javafx.beans.binding.Bindings.createStringBinding(
                () -> cell.getValue().getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))));

        tableView.getColumns().addAll(serviceCol, loginCol, passwordCol, createdCol);

        tableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            selected = newSel;
            if (newSel != null) {
                fillForm(newSel);
            }
        });

        return tableView;
    }

    private VBox buildForm() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(10));
        box.setPrefWidth(400);

        serviceField.setPromptText("example.com");
        loginField.setPromptText("user@example.com");
        passwordField.setPromptText("Пароль");
        visiblePasswordField.setManaged(false);
        visiblePasswordField.setVisible(false);
        visiblePasswordField.textProperty().bindBidirectional(passwordField.textProperty());
        notesArea.setPromptText("Дополнительные сведения");
        notesArea.setWrapText(true);
        notesArea.setPrefRowCount(3);

        passwordField.textProperty().addListener((obs, oldVal, newVal) -> onPasswordInput.accept(newVal));

        Button toggleShow = new Button("Показать");
        toggleShow.setOnAction(event -> togglePasswordVisibility(toggleShow));

        Button saveButton = new Button("Сохранить запись");
        saveButton.setMaxWidth(Double.MAX_VALUE);
        saveButton.setOnAction(event -> onAdd.accept(
                serviceField.getText(),
                loginField.getText(),
                passwordField.getText(),
                notesArea.getText()));

        Button generateButton = new Button("Сгенерировать пароль");
        generateButton.setWrapText(false);
        generateButton.setOnAction(event -> onGenerate.accept(lengthSpinner.getValue()));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        // Чтобы метки не сжимались
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(120); // фиксированная ширина для меток
        col1.setHgrow(Priority.NEVER);

        // Поля растягиваются
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);

        grid.getColumnConstraints().addAll(col1, col2);

        StackPane passwordStack = new StackPane(passwordField, visiblePasswordField);
        passwordStack.setMaxWidth(Double.MAX_VALUE);
        HBox passwordRow = new HBox(8, passwordStack, toggleShow);
        passwordRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(passwordStack, Priority.ALWAYS);

        grid.add(new Label("Название сервиса:"), 0, 0);
        grid.add(serviceField, 1, 0);
        grid.add(new Label("Логин или почта:"), 0, 1);
        grid.add(loginField, 1, 1);
        grid.add(new Label("Пароль:"), 0, 2);
        grid.add(passwordRow, 1, 2);
        grid.add(passwordStrengthLabel, 1, 3);
        
        // Генератор пароля в отдельной строке GridPane для полной видимости
        grid.add(new Label("Длина:"), 0, 4);
        HBox generatorBox = new HBox(8, lengthSpinner, generateButton);
        generatorBox.setAlignment(Pos.CENTER_LEFT);
        grid.add(generatorBox, 1, 4);
        
        grid.add(new Label("Примечание:"), 0, 5);
        grid.add(notesArea, 1, 5);

        box.getChildren().addAll(new Label("Новая запись"), grid, saveButton);

        // Здесь убираем растягивание GridPane по вертикали
        // VBox.setVgrow(grid, Priority.ALWAYS); // <- убрали эту строку

        return box;
    }


    private void togglePasswordVisibility(Button toggleButton) {
        boolean showingPlain = visiblePasswordField.isVisible();
        visiblePasswordField.setVisible(!showingPlain);
        visiblePasswordField.setManaged(!showingPlain);
        passwordField.setVisible(showingPlain);
        passwordField.setManaged(showingPlain);
        toggleButton.setText(showingPlain ? "Показать" : "Скрыть");
        TextInputControl control = showingPlain ? passwordField : visiblePasswordField;
        control.requestFocus();
        control.positionCaret(control.getText().length());
    }

    private void fillForm(PasswordEntry entry) {
        serviceField.setText(entry.getService());
        loginField.setText(entry.getUsername());
        passwordField.setText(entry.getPassword());
        notesArea.setText(entry.getNotes());
        onPasswordInput.accept(entry.getPassword());
    }

    public void setEntries(List<PasswordEntry> newEntries) {
        entries.setAll(newEntries);
        if (!entries.contains(selected)) {
            selected = null;
        }
    }

    public void setGeneratedPassword(String password) {
        passwordField.setText(password);
        visiblePasswordField.setText(password);
        onPasswordInput.accept(password);
    }

    public void updatePasswordStrength(PasswordStrength strength) {
        passwordStrengthLabel.setText("Надежность: " + strength.getLabel());
        passwordStrengthLabel.setStyle("-fx-text-fill: " + strength.getColor() + ";");
    }

    public void clearForm() {
        selected = null;
        serviceField.clear();
        loginField.clear();
        passwordField.clear();
        notesArea.clear();
        passwordStrengthLabel.setText("Надежность: —");
        passwordStrengthLabel.setStyle("-fx-text-fill: -fx-text-base-color;");
        serviceField.requestFocus();
    }

    public void showError(String message) {
        showAlert(Alert.AlertType.ERROR, "Ошибка", message);
    }

    public void showInfo(String message) {
        showAlert(Alert.AlertType.INFORMATION, "Информация", message);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void onGenerate(Consumer<Integer> handler) {
        this.onGenerate = Objects.requireNonNull(handler);
    }

    public void onDelete(Consumer<String> handler) {
        this.onDelete = Objects.requireNonNull(handler);
    }

    public void onAdd(QuadConsumer<String, String, String, String> handler) {
        this.onAdd = Objects.requireNonNull(handler);
    }

    public void onRefresh(Runnable handler) {
        this.onRefresh = Objects.requireNonNull(handler);
    }

    public void onPasswordInput(Consumer<String> handler) {
        this.onPasswordInput = Objects.requireNonNull(handler);
    }

    public void onImport(Consumer<Path> handler) {
        this.onImport = Objects.requireNonNull(handler);
    }

    public void onExport(Consumer<Path> handler) {
        this.onExport = Objects.requireNonNull(handler);
    }

    private java.util.Optional<Path> chooseFile(boolean open) {
        FileChooser chooser = new FileChooser();
        // Только JSON файлы
        FileChooser.ExtensionFilter jsonFilter = new FileChooser.ExtensionFilter("JSON файлы", "*.json");
        chooser.getExtensionFilters().add(jsonFilter);
        chooser.setSelectedExtensionFilter(jsonFilter);
        
        // Для сохранения устанавливаем имя файла по умолчанию
        if (!open) {
            chooser.setInitialFileName("vault.json");
        }
        
        java.io.File file = open ? chooser.showOpenDialog(tableView.getScene().getWindow())
                : chooser.showSaveDialog(tableView.getScene().getWindow());
        
        if (file != null) {
            Path path = file.toPath();
            // При сохранении автоматически добавляем расширение .json если его нет
            if (!open && !path.getFileName().toString().toLowerCase().endsWith(".json")) {
                path = path.resolveSibling(path.getFileName().toString() + ".json");
            }
            return java.util.Optional.of(path);
        }
        return java.util.Optional.empty();
    }

    @FunctionalInterface
    public interface QuadConsumer<A, B, C, D> {
        void accept(A a, B b, C c, D d);
    }
}

