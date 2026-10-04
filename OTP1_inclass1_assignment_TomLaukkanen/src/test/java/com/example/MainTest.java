package com.example;

import javafx.application.Platform;
import javafx.scene.control.TableColumn;
import javafx.stage.Stage;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives the JavaFX window the way a user would: types a value, presses the buttons
 * and checks what the window shows and what ends up in the database.
 */
class MainTest {

    private DBConnection database;
    private TempRecordDAO recordDao;
    private Main app;

    @BeforeAll
    static void startJavaFx() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException alreadyRunning) {
            // Started by an earlier test, nothing to do
        } catch (RuntimeException noDisplay) {
            // E.g. a Linux machine without a display: skip instead of failing
            Assumptions.abort("JavaFX cannot start here: " + noDisplay);
        }
        Platform.setImplicitExit(false);
    }

    @BeforeEach
    void setUp() throws Exception {
        // An empty database: the app has to create the tables itself
        database = TestDatabase.empty();
        recordDao = new TempRecordDAO(database);
        app = new Main(database);
        runFx(app::buildRoot);
    }

    @Test
    @DisplayName("On start the unit lists come from the database, Celsius to Fahrenheit selected")
    void loadsUnitsFromDatabase() throws Exception {
        runFx(() -> {
            assertEquals(3, app.fromBox.getItems().size());
            assertEquals(3, app.toBox.getItems().size());
            assertEquals("C", app.fromBox.getValue().getCode());
            assertEquals("F", app.toBox.getValue().getCode());
            assertTrue(app.historyTable.getItems().isEmpty());
            assertFalse(app.convertButton.isDisabled());
        });
    }

    @Test
    @DisplayName("Convert shows the result and saves it to the database")
    void convertsAndSaves() throws Exception {
        convert("25");

        runFx(() -> {
            assertEquals("25.00 °C = 77.00 °F", app.resultLabel.getText());
            assertEquals("Saved to the database", app.messageLabel.getText());
            assertEquals(1, app.historyTable.getItems().size());
        });

        List<TempRecord> saved = recordDao.findAll();
        assertEquals(1, saved.size());
        assertEquals(25, saved.get(0).getInputValue());
        assertEquals("C", saved.get(0).getFromUnit().getCode());
        assertEquals(77, saved.get(0).getResultValue(), 1e-9);
        assertEquals("F", saved.get(0).getToUnit().getCode());
    }

    @Test
    @DisplayName("Any pair of units can be chosen")
    void convertsKelvinToCelsius() throws Exception {
        runFx(() -> {
            app.fromBox.getSelectionModel().select(2);
            app.toBox.getSelectionModel().select(0);
        });
        convert("300");
        runFx(() -> assertEquals("300.00 K = 26.85 °C", app.resultLabel.getText()));
    }

    @Test
    @DisplayName("A comma works as the decimal separator")
    void acceptsCommaDecimal() throws Exception {
        convert("37,5");
        runFx(() -> assertEquals("37.50 °C = 99.50 °F", app.resultLabel.getText()));
    }

    @Test
    @DisplayName("Extreme temperatures are pointed out")
    void warnsAboutExtremeTemperature() throws Exception {
        convert("60");
        runFx(() -> assertTrue(app.messageLabel.getText().contains("extreme"), app.messageLabel.getText()));
    }

    @Test
    @DisplayName("Text that is not a number is reported and nothing is saved")
    void rejectsNonNumber() throws Exception {
        convert("hot");
        runFx(() -> assertEquals("Not a number: hot", app.messageLabel.getText()));
        assertTrue(recordDao.findAll().isEmpty());
    }

    @Test
    @DisplayName("An empty field asks for a temperature")
    void rejectsEmptyInput() throws Exception {
        convert("   ");
        runFx(() -> assertEquals("Enter a temperature first", app.messageLabel.getText()));
        convert(null);
        runFx(() -> assertEquals("Enter a temperature first", app.messageLabel.getText()));
        assertTrue(recordDao.findAll().isEmpty());
    }

    @Test
    @DisplayName("A temperature below absolute zero is rejected and nothing is saved")
    void rejectsBelowAbsoluteZero() throws Exception {
        convert("-300");
        runFx(() -> assertEquals("-300.00 °C is below absolute zero", app.messageLabel.getText()));
        assertTrue(recordDao.findAll().isEmpty());
    }

    @Test
    @DisplayName("Swap exchanges the from and to units")
    void swapsUnits() throws Exception {
        runFx(() -> {
            app.swapButton.fire();
            assertEquals("F", app.fromBox.getValue().getCode());
            assertEquals("C", app.toBox.getValue().getCode());
        });
    }

    @Test
    @DisplayName("The history table shows the time, input and result of each saved conversion")
    void historyColumns() throws Exception {
        convert("0");
        runFx(() -> {
            TempRecord row = app.historyTable.getItems().get(0);
            List<TableColumn<TempRecord, ?>> columns = app.historyTable.getColumns();
            assertEquals("Time", columns.get(0).getText());
            assertTrue(String.valueOf(columns.get(0).getCellObservableValue(row).getValue())
                    .matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
            assertEquals("0.00 °C", columns.get(1).getCellObservableValue(row).getValue());
            assertEquals("32.00 °F", columns.get(2).getCellObservableValue(row).getValue());
        });
    }

    @Test
    @DisplayName("Clear history deletes the saved conversions")
    void clearsHistory() throws Exception {
        convert("10");
        convert("20");
        runFx(() -> {
            app.clearButton.fire();
            assertTrue(app.historyTable.getItems().isEmpty());
            assertEquals("Deleted 2 saved conversion(s)", app.messageLabel.getText());
        });
        assertTrue(recordDao.findAll().isEmpty());
    }

    @Test
    @DisplayName("A failed save is reported but the result is still shown")
    void reportsSaveFailure() throws Exception {
        TestDatabase.execute(database, "DROP TABLE temp_record");
        convert("100");
        runFx(() -> {
            assertEquals("100.00 °C = 212.00 °F", app.resultLabel.getText());
            assertTrue(app.messageLabel.getText().startsWith("Could not save to the database"));
        });
    }

    @Test
    @DisplayName("A failed clear is reported")
    void reportsClearFailure() throws Exception {
        TestDatabase.execute(database, "DROP TABLE temp_record");
        runFx(() -> {
            app.clearButton.fire();
            assertTrue(app.messageLabel.getText().startsWith("Could not clear the history"));
        });
    }

    @Test
    @DisplayName("Without a database the window explains the problem and disables the form")
    void databaseUnavailable() throws Exception {
        Main offline = new Main(new DBConnection("jdbc:nosuchdriver://localhost/none", "user", "pw"));
        runFx(() -> {
            offline.buildRoot();
            assertTrue(offline.messageLabel.getText().startsWith("Cannot connect to the database"));
            assertTrue(offline.convertButton.isDisabled());
            assertTrue(offline.valueField.isDisabled());
            assertTrue(offline.fromBox.getItems().isEmpty());
        });
    }

    @Test
    @DisplayName("start opens the window")
    void startOpensWindow() throws Exception {
        Main windowApp = new Main(TestDatabase.empty());
        runFx(() -> {
            Stage stage = new Stage();
            windowApp.start(stage);
            try {
                assertTrue(stage.isShowing());
                assertEquals("Temperature Converter", stage.getTitle());
            } finally {
                stage.close();
            }
        });
    }

    @Test
    @DisplayName("JavaFX can create the app without connecting to the database yet")
    void publicConstructorDoesNotConnect() {
        assertDoesNotThrow(() -> new Main());
    }

    @Test
    @DisplayName("Values are shown with two decimals and the unit symbol")
    void formatsValues() {
        TemperatureUnit celsius = new TemperatureUnit(1, "C", "Celsius", "°C");
        assertEquals("26.85 °C", Main.formatValue(26.849999, celsius));
        assertEquals("-40.00 °C", Main.formatValue(-40, celsius));
    }

    /** Types the text into the value field and presses Convert. */
    private void convert(String text) throws Exception {
        runFx(() -> {
            app.valueField.setText(text);
            app.convertButton.fire();
        });
    }

    /** Runs the code on the JavaFX thread and waits for it, passing on any failure. */
    private static void runFx(ThrowingRunnable action) throws Exception {
        CompletableFuture<Void> done = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                action.run();
                done.complete(null);
            } catch (Throwable t) {
                done.completeExceptionally(t);
            }
        });
        try {
            done.get(10, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof Error error) {
                throw error;
            }
            throw (Exception) e.getCause();
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
