package com.example;

import javafx.application.Application;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * JavaFX window of the Temperature Converter. Converts the value the user types,
 * saves every conversion to the database and lists the saved conversions.
 */
public class Main extends Application {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TempCalculator calculator = new TempCalculator();
    private final DBConnection database;
    private final TemperatureUnitDAO unitDao;
    private final TempRecordDAO recordDao;

    // Package-private so the tests can fill in the form and press the buttons
    TextField valueField;
    ComboBox<TemperatureUnit> fromBox;
    ComboBox<TemperatureUnit> toBox;
    Button convertButton;
    Button swapButton;
    Button clearButton;
    Label resultLabel;
    Label messageLabel;
    TableView<TempRecord> historyTable;

    /** Used by JavaFX: connects to the database given by DB_URL, DB_USER and DB_PASSWORD. */
    public Main() {
        this(DBConnection.fromEnvironment());
    }

    Main(DBConnection database) {
        this.database = database;
        this.unitDao = new TemperatureUnitDAO(database);
        this.recordDao = new TempRecordDAO(database);
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("Temperature Converter");
        stage.setScene(new Scene(buildRoot(), 640, 560));
        stage.show();
    }

    /** Builds the window contents and loads the units and the history from the database. */
    Parent buildRoot() {
        Label title = new Label("Temperature Converter");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

        valueField = new TextField();
        valueField.setPromptText("e.g. 21.5");
        valueField.setPrefColumnCount(8);
        fromBox = new ComboBox<>();
        toBox = new ComboBox<>();
        swapButton = new Button("Swap");
        swapButton.setOnAction(e -> swapUnits());
        HBox inputRow = new HBox(8, valueField, fromBox, new Label("→"), toBox, swapButton);
        inputRow.setAlignment(Pos.CENTER_LEFT);

        convertButton = new Button("Convert and save");
        convertButton.setDefaultButton(true);
        convertButton.setOnAction(e -> convert());

        resultLabel = new Label("Type a temperature and press Enter");
        resultLabel.setStyle("-fx-font-size: 20px;");
        messageLabel = new Label();

        Label historyTitle = new Label("Saved conversions");
        historyTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        historyTable = new TableView<>();
        historyTable.getColumns().setAll(List.of(
                column("Time", r -> r.getCreatedAt().format(TIME_FORMAT)),
                column("From", r -> formatValue(r.getInputValue(), r.getFromUnit())),
                column("To", r -> formatValue(r.getResultValue(), r.getToUnit()))));
        historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        historyTable.setPlaceholder(new Label("No conversions saved yet"));
        VBox.setVgrow(historyTable, Priority.ALWAYS);

        clearButton = new Button("Clear history");
        clearButton.setOnAction(e -> clearHistory());

        Label databaseLabel = new Label("Database: " + database.getUrl());
        databaseLabel.setStyle("-fx-text-fill: gray;");

        VBox root = new VBox(12, title, inputRow, convertButton, resultLabel, messageLabel,
                historyTitle, historyTable, clearButton, databaseLabel);
        root.setPadding(new Insets(20));

        connectDatabase();
        return root;
    }

    /** Creates the tables if needed and fills the unit lists and the history. */
    private void connectDatabase() {
        try {
            database.initializeSchema();
            List<TemperatureUnit> units = unitDao.findAll();
            fromBox.getItems().setAll(units);
            toBox.getItems().setAll(units);
            fromBox.getSelectionModel().select(0);
            toBox.getSelectionModel().select(1);
            refreshHistory();
        } catch (SQLException e) {
            showError("Cannot connect to the database: " + e.getMessage());
            valueField.setDisable(true);
            convertButton.setDisable(true);
            swapButton.setDisable(true);
            clearButton.setDisable(true);
        }
    }

    /** Converts the typed value, shows the result and saves it to the database. */
    void convert() {
        TemperatureUnit from = fromBox.getValue();
        TemperatureUnit to = toBox.getValue();
        String text = valueField.getText() == null ? "" : valueField.getText().trim();

        double value;
        try {
            value = calculator.parseTemperature(text);
        } catch (NumberFormatException e) {
            showError(text.isEmpty() ? "Enter a temperature first" : "Not a number: " + text);
            return;
        }
        if (calculator.isBelowAbsoluteZero(value, from.getCode())) {
            showError(formatValue(value, from) + " is below absolute zero");
            return;
        }

        double result = calculator.convert(value, from.getCode(), to.getCode());
        resultLabel.setText(formatValue(value, from) + " = " + formatValue(result, to));

        try {
            recordDao.save(new TempRecord(value, from, result, to));
            refreshHistory();
        } catch (SQLException e) {
            showError("Could not save to the database: " + e.getMessage());
            return;
        }

        boolean extreme = calculator.isExtremeTemperature(calculator.convert(value, from.getCode(), "C"));
        showInfo(extreme ? "Saved. Careful, that is an extreme temperature!" : "Saved to the database");
    }

    /** Swaps the "from" and "to" units. */
    void swapUnits() {
        TemperatureUnit from = fromBox.getValue();
        fromBox.setValue(toBox.getValue());
        toBox.setValue(from);
    }

    /** Deletes all saved conversions. */
    void clearHistory() {
        try {
            int deleted = recordDao.deleteAll();
            refreshHistory();
            showInfo("Deleted " + deleted + " saved conversion(s)");
        } catch (SQLException e) {
            showError("Could not clear the history: " + e.getMessage());
        }
    }

    private void refreshHistory() throws SQLException {
        historyTable.getItems().setAll(recordDao.findAll());
    }

    private void showInfo(String message) {
        messageLabel.setStyle("-fx-text-fill: #1a7f37;");
        messageLabel.setText(message);
    }

    private void showError(String message) {
        messageLabel.setStyle("-fx-text-fill: #cf222e;");
        messageLabel.setText(message);
    }

    static String formatValue(double value, TemperatureUnit unit) {
        return String.format(Locale.ROOT, "%.2f %s", value, unit.getSymbol());
    }

    private static TableColumn<TempRecord, String> column(String title, Function<TempRecord, String> text) {
        TableColumn<TempRecord, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(text.apply(cell.getValue())));
        return column;
    }
}
