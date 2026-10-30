package com.example;

import javafx.application.Platform;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.TableColumn;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives the JavaFX window the way a user would: picks a language, types a value, presses
 * the buttons and checks what the window shows and what ends up in the database.
 */
class MainTest {

    private DBConnection database;
    private ConversionDAO conversionDao;
    private Main app;
    private Stage stage;

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
        conversionDao = new ConversionDAO(database);
        app = new Main(database);
        runFx(() -> {
            stage = new Stage();
            app.start(stage);
        });
    }

    @AfterEach
    void closeWindow() throws Exception {
        runFx(stage::close);
    }

    // --- start ---

    @Test
    @DisplayName("The window opens in English with the units from the database, Celsius to Fahrenheit selected")
    void startsInEnglish() throws Exception {
        runFx(() -> {
            assertTrue(stage.isShowing());
            assertEquals("Temperature Converter", stage.getTitle());
            assertEquals(Language.ENGLISH, ui().languageBox.getValue());
            assertEquals(List.of(Language.values()), ui().languageBox.getItems());
            assertEquals(NodeOrientation.LEFT_TO_RIGHT, stage.getScene().getRoot().getNodeOrientation());

            assertEquals(3, ui().fromBox.getItems().size());
            assertEquals(3, ui().toBox.getItems().size());
            assertEquals("C", ui().fromBox.getValue().getCode());
            assertEquals("F", ui().toBox.getValue().getCode());
            assertEquals("Celsius (°C)", unitText(ui().fromBox.getValue()));

            assertEquals("Type a temperature and press Enter", ui().resultLabel.getText());
            assertTrue(ui().historyTable.getItems().isEmpty());
            assertFalse(ui().convertButton.isDisabled());
            assertEquals("Database: " + database.getUrl(), ui().databaseLabel.getText());
        });
    }

    // --- converting in English ---

    @Test
    @DisplayName("Convert shows the result and saves it to the database with the language")
    void convertsAndSaves() throws Exception {
        convert("25");

        runFx(() -> {
            assertEquals("25.00 °C = 77.00 °F", ui().resultLabel.getText());
            assertEquals("Saved to the database", ui().messageLabel.getText());
            assertEquals(1, ui().historyTable.getItems().size());
        });

        List<Conversion> saved = conversionDao.findAll();
        assertEquals(1, saved.size());
        assertEquals(25, saved.get(0).getInputValue());
        assertEquals("C", saved.get(0).getFromUnit().getCode());
        assertEquals(77, saved.get(0).getResultValue(), 1e-9);
        assertEquals("F", saved.get(0).getToUnit().getCode());
        assertEquals("en_US", saved.get(0).getLanguageCode());
    }

    @Test
    @DisplayName("Any pair of units can be chosen")
    void convertsKelvinToCelsius() throws Exception {
        runFx(() -> {
            ui().fromBox.getSelectionModel().select(2);
            ui().toBox.getSelectionModel().select(0);
        });
        convert("300");
        runFx(() -> assertEquals("300.00 K = 26.85 °C", ui().resultLabel.getText()));
    }

    @Test
    @DisplayName("A comma works as the decimal separator")
    void acceptsCommaDecimal() throws Exception {
        convert("37,5");
        runFx(() -> assertEquals("37.50 °C = 99.50 °F", ui().resultLabel.getText()));
    }

    @Test
    @DisplayName("Extreme temperatures are pointed out")
    void warnsAboutExtremeTemperature() throws Exception {
        convert("60");
        runFx(() -> assertEquals("Saved. Careful, that is an extreme temperature!", ui().messageLabel.getText()));
    }

    @Test
    @DisplayName("Text that is not a number is reported and nothing is saved")
    void rejectsNonNumber() throws Exception {
        convert("hot");
        runFx(() -> assertEquals("Not a number: hot", ui().messageLabel.getText()));
        assertTrue(conversionDao.findAll().isEmpty());
    }

    @Test
    @DisplayName("An empty field asks for a temperature")
    void rejectsEmptyInput() throws Exception {
        convert("   ");
        runFx(() -> assertEquals("Enter a temperature first", ui().messageLabel.getText()));
        convert(null);
        runFx(() -> assertEquals("Enter a temperature first", ui().messageLabel.getText()));
        assertTrue(conversionDao.findAll().isEmpty());
    }

    @Test
    @DisplayName("A temperature below absolute zero is rejected and nothing is saved")
    void rejectsBelowAbsoluteZero() throws Exception {
        convert("-300");
        runFx(() -> assertEquals("-300.00 °C is below absolute zero", ui().messageLabel.getText()));
        assertTrue(conversionDao.findAll().isEmpty());
    }

    @Test
    @DisplayName("Swap exchanges the from and to units")
    void swapsUnits() throws Exception {
        runFx(() -> {
            ui().swapButton.fire();
            assertEquals("F", ui().fromBox.getValue().getCode());
            assertEquals("C", ui().toBox.getValue().getCode());
        });
    }

    @Test
    @DisplayName("The history table shows the time, input, result and language of each saved conversion")
    void historyColumns() throws Exception {
        convert("0");
        runFx(() -> {
            Conversion row = ui().historyTable.getItems().get(0);
            List<TableColumn<Conversion, ?>> columns = ui().historyTable.getColumns();
            assertEquals(List.of("Time", "From", "To", "Language"), columns.stream().map(TableColumn::getText).toList());
            assertEquals(MainController.formatTime(row.getCreatedAt(), Locale.of("en", "US")), cell(columns.get(0), row));
            assertEquals("0.00 °C", cell(columns.get(1), row));
            assertEquals("32.00 °F", cell(columns.get(2), row));
            assertEquals("English", cell(columns.get(3), row));
        });
    }

    @Test
    @DisplayName("Clear history deletes the saved conversions")
    void clearsHistory() throws Exception {
        convert("10");
        convert("20");
        runFx(() -> {
            ui().clearButton.fire();
            assertTrue(ui().historyTable.getItems().isEmpty());
            assertEquals("Deleted saved conversions: 2", ui().messageLabel.getText());
        });
        assertTrue(conversionDao.findAll().isEmpty());
    }

    @Test
    @DisplayName("A failed save is reported but the result is still shown")
    void reportsSaveFailure() throws Exception {
        TestDatabase.execute(database, "DROP TABLE conversion");
        convert("100");
        runFx(() -> {
            assertEquals("100.00 °C = 212.00 °F", ui().resultLabel.getText());
            assertTrue(ui().messageLabel.getText().startsWith("Could not save to the database: "));
        });
    }

    @Test
    @DisplayName("A failed clear is reported")
    void reportsClearFailure() throws Exception {
        TestDatabase.execute(database, "DROP TABLE conversion");
        runFx(() -> {
            ui().clearButton.fire();
            assertTrue(ui().messageLabel.getText().startsWith("Could not clear the history: "));
        });
    }

    // --- switching the language ---

    @ParameterizedTest(name = "{0}: \"{1}\", \"{2}\"")
    @CsvSource({
            "FINNISH,  Lämpötilamuunnin,     Muunna ja tallenna, Tyhjennä historia",
            "SWEDISH,  Temperaturomvandlare, Omvandla och spara, Rensa historiken",
            "JAPANESE, 温度変換ツール,         変換して保存,         履歴を消去",
            "ARABIC,   محول درجات الحرارة,    تحويل وحفظ,          مسح السجل",
            "ENGLISH,  Temperature Converter, Convert and save,  Clear history"
    })
    void switchingLanguageTranslatesTheWindow(Language language, String title, String convert, String clear)
            throws Exception {
        selectLanguage(language == Language.ENGLISH ? Language.FINNISH : language);
        selectLanguage(language);
        runFx(() -> {
            assertEquals(title, stage.getTitle());
            assertEquals(title, ui().titleLabel.getText());
            assertEquals(convert, ui().convertButton.getText());
            assertEquals(clear, ui().clearButton.getText());
            assertEquals(language, ui().languageBox.getValue());
            assertEquals(language.getBundle().getString("result.initial"), ui().resultLabel.getText());
            assertEquals(language.getBundle().getString("column.time"), ui().timeColumn.getText());
        });
    }

    @Test
    @DisplayName("Arabic mirrors the window right to left, the other languages put it back")
    void arabicIsRightToLeft() throws Exception {
        selectLanguage(Language.ARABIC);
        runFx(() -> assertEquals(NodeOrientation.RIGHT_TO_LEFT, stage.getScene().getRoot().getNodeOrientation()));
        selectLanguage(Language.JAPANESE);
        runFx(() -> assertEquals(NodeOrientation.LEFT_TO_RIGHT, stage.getScene().getRoot().getNodeOrientation()));
    }

    @Test
    @DisplayName("Choosing the language that is already shown does not reload the window")
    void sameLanguageDoesNotReload() throws Exception {
        MainController before = app.getController();
        runFx(() -> {
            ui().languageBox.setValue(null);
            ui().languageBox.setValue(Language.ENGLISH);
        });
        runFx(() -> assertSame(before, app.getController()));
    }

    @Test
    @DisplayName("The typed value, the units and the last result are kept when the language changes")
    void switchingLanguageKeepsWhatTheUserDid() throws Exception {
        runFx(() -> {
            ui().fromBox.getSelectionModel().select(2);
            ui().toBox.getSelectionModel().select(0);
        });
        convert("300");
        MainController english = app.getController();

        selectLanguage(Language.FINNISH);

        runFx(() -> {
            assertNotSame(english, app.getController());
            assertEquals("300", ui().valueField.getText());
            assertEquals("K", ui().fromBox.getValue().getCode());
            assertEquals("C", ui().toBox.getValue().getCode());
            assertEquals("300,00 K = 26,85 °C", ui().resultLabel.getText());
            assertEquals(1, ui().historyTable.getItems().size());
        });
    }

    @Test
    @DisplayName("Finnish: decimal comma, Finnish messages and the language saved as fi_FI")
    void finnishConversion() throws Exception {
        selectLanguage(Language.FINNISH);
        convert("37,5");
        runFx(() -> {
            assertEquals("37,50 °C = 99,50 °F", ui().resultLabel.getText());
            assertEquals("Tallennettu tietokantaan", ui().messageLabel.getText());
            assertEquals("Suomi", cell(ui().languageColumn, ui().historyTable.getItems().get(0)));
        });
        assertEquals("fi_FI", conversionDao.findAll().get(0).getLanguageCode());
    }

    @Test
    @DisplayName("Swedish: negative numbers are written with the minus sign −")
    void swedishMinusSign() throws Exception {
        selectLanguage(Language.SWEDISH);
        convert("-40");
        runFx(() -> {
            assertEquals("−40,00 °C = −40,00 °F", ui().resultLabel.getText());
            assertEquals("Sparad i databasen", ui().messageLabel.getText());
        });
        // The way Swedish writes it can be typed back in
        convert("−40,00");
        runFx(() -> assertEquals(2, ui().historyTable.getItems().size()));
    }

    @Test
    @DisplayName("Japanese: unit names, messages and full-width input")
    void japaneseConversion() throws Exception {
        selectLanguage(Language.JAPANESE);
        runFx(() -> {
            assertEquals("摂氏 (°C)", unitText(ui().fromBox.getValue()));
            assertEquals("華氏 (°F)", unitText(ui().toBox.getValue()));
            assertEquals("ケルビン (K)", unitText(ui().fromBox.getItems().get(2)));
        });
        convert("２５");
        runFx(() -> {
            assertEquals("25.00 °C = 77.00 °F", ui().resultLabel.getText());
            assertEquals("データベースに保存しました", ui().messageLabel.getText());
        });
        convert("");
        runFx(() -> assertEquals("先に温度を入力してください", ui().messageLabel.getText()));
    }

    @Test
    @DisplayName("Arabic: Arabic-Indic digits in the result and Arabic messages")
    void arabicConversion() throws Exception {
        selectLanguage(Language.ARABIC);
        convert("25");
        runFx(() -> {
            assertEquals("٢٥٫٠٠ ‎°C‎ = ٧٧٫٠٠ ‎°F‎", ui().resultLabel.getText());
            assertEquals("تم الحفظ في قاعدة البيانات", ui().messageLabel.getText());
            assertEquals("العربية", cell(ui().languageColumn, ui().historyTable.getItems().get(0)));
        });
        // Arabic-Indic digits can be typed too
        convert("١٠٠");
        runFx(() -> {
            assertEquals("١٠٠٫٠٠ ‎°C‎ = ٢١٢٫٠٠ ‎°F‎", ui().resultLabel.getText());
            ui().clearButton.fire();
            assertEquals("عدد التحويلات المحذوفة: ٢", ui().messageLabel.getText());
        });
    }

    @Test
    @DisplayName("The history keeps conversions made in every language and shows them in the current one")
    void historyFromSeveralLanguages() throws Exception {
        convert("0");
        selectLanguage(Language.JAPANESE);
        convert("100");
        selectLanguage(Language.SWEDISH);

        runFx(() -> {
            List<Conversion> rows = ui().historyTable.getItems();
            assertEquals(2, rows.size());
            assertEquals("日本語", cell(ui().languageColumn, rows.get(0)));
            assertEquals("English", cell(ui().languageColumn, rows.get(1)));
            assertEquals("100,00 °C", cell(ui().fromColumn, rows.get(0)));
            assertEquals(MainController.formatTime(rows.get(0).getCreatedAt(), Locale.of("sv", "SE")),
                    cell(ui().timeColumn, rows.get(0)));
        });
    }

    // --- without a database ---

    @Test
    @DisplayName("Without a database the window explains the problem in the chosen language and disables the form")
    void databaseUnavailable() throws Exception {
        Main offline = new Main(new DBConnection("jdbc:nosuchdriver://localhost/none", "user", "pw"));
        Stage offlineStage = runFx(() -> {
            Stage window = new Stage();
            offline.start(window);
            return window;
        });
        try {
            runFx(() -> {
                MainController ui = offline.getController();
                assertTrue(ui.messageLabel.getText().startsWith("Cannot connect to the database: "));
                assertTrue(ui.convertButton.isDisabled());
                assertTrue(ui.valueField.isDisabled());
                assertTrue(ui.fromBox.getItems().isEmpty());
                ui.languageBox.setValue(Language.FINNISH);
            });
            runFx(() -> assertTrue(offline.getController().messageLabel.getText()
                    .startsWith("Tietokantaan ei saada yhteyttä: ")));
        } finally {
            runFx(offlineStage::close);
        }
    }

    // --- helpers of the controller ---

    @Test
    @DisplayName("A unit missing from the bundle is shown with its name from the database")
    void unitWithoutTranslation() throws Exception {
        TemperatureUnit rankine = new TemperatureUnit(9, "R", "Rankine", "°R");
        runFx(() -> {
            assertEquals("Rankine", ui().unitName(rankine));
            assertEquals("", unitText(null));
            assertThrows(UnsupportedOperationException.class, () -> ui().fromBox.getConverter().fromString("x"));
        });
    }

    @ParameterizedTest(name = "{0}: {1}")
    @CsvSource({
            "fi-FI, '30.10.2026 14.05.09'",
            "sv-SE, '2026-10-30 14:05:09'",
            "ja-JP, '2026/10/30 14:05:09'"
    })
    void timesAreLocalized(String languageTag, String expected) {
        assertEquals(expected, MainController.formatTime(LocalDateTime.of(2026, 10, 30, 14, 5, 9),
                Locale.forLanguageTag(languageTag)));
    }

    @Test
    @DisplayName("Arabic times use Arabic-Indic digits")
    void arabicTimes() {
        String time = MainController.formatTime(LocalDateTime.of(2026, 10, 30, 14, 5, 9), Locale.of("ar", "SA"));
        assertTrue(time.startsWith("٣٠/١٠/٢٠٢٦"), time);
        assertFalse(time.contains("‏"), "no right-to-left marks, Linux fonts show them as boxes");
    }

    @Test
    @DisplayName("Values are shown with two decimals and the unit symbol, the way the locale writes numbers")
    void formatsValues() {
        TemperatureUnit celsius = new TemperatureUnit(1, "C", "Celsius", "°C");
        assertEquals("26.85 °C", MainController.formatValue(26.849999, celsius, Language.ENGLISH));
        assertEquals("1234.50 °C", MainController.formatValue(1234.5, celsius, Language.ENGLISH));
        assertEquals("−40,00 °C", MainController.formatValue(-40, celsius, Language.FINNISH));
        assertEquals("٢٦٫٨٥ ‎°C‎", MainController.formatValue(26.85, celsius, Language.ARABIC));
        // Java writes the Arabic minus as U+061C + "-"; the mark is removed
        assertEquals("-٤٠٫٠٠ ‎°C‎", MainController.formatValue(-40, celsius, Language.ARABIC));
    }

    @Test
    @DisplayName("Latin text is marked left to right only in Arabic, so \"°C\" is not shown as \"C°\"")
    void leftToRightMarks() throws Exception {
        assertEquals("°C", MainController.leftToRight("°C", Language.JAPANESE));
        assertEquals("‎°C‎", MainController.leftToRight("°C", Language.ARABIC));
        selectLanguage(Language.ARABIC);
        runFx(() -> {
            assertEquals("قاعدة البيانات: ‎" + database.getUrl() + "‎", ui().databaseLabel.getText());
            assertEquals("درجة مئوية (‎°C‎)", unitText(ui().fromBox.getValue()));
        });
    }

    @Test
    @DisplayName("JavaFX can create the app without connecting to the database yet")
    void publicConstructorDoesNotConnect() {
        assertDoesNotThrow(() -> new Main());
    }

    // --- test helpers ---

    private MainController ui() {
        return app.getController();
    }

    /** Types the text into the value field and presses Convert. */
    private void convert(String text) throws Exception {
        runFx(() -> {
            ui().valueField.setText(text);
            ui().convertButton.fire();
        });
    }

    /** Picks a language from the drop-down like a user and waits until the window is reloaded. */
    private void selectLanguage(Language language) throws Exception {
        runFx(() -> ui().languageBox.setValue(language));
        // The reload was queued with Platform.runLater, so it has run once this empty task runs
        runFx(() -> { });
    }

    /** The text the unit drop-downs show for a unit. */
    private String unitText(TemperatureUnit unit) {
        return ui().fromBox.getConverter().toString(unit);
    }

    private static String cell(TableColumn<Conversion, ?> column, Conversion row) {
        return String.valueOf(column.getCellObservableValue(row).getValue());
    }

    /** Runs the code on the JavaFX thread and waits for it, passing on any failure. */
    private static void runFx(ThrowingRunnable action) throws Exception {
        runFx(() -> {
            action.run();
            return null;
        });
    }

    /** Runs the code on the JavaFX thread and returns its result, passing on any failure. */
    private static <T> T runFx(ThrowingSupplier<T> action) throws Exception {
        CompletableFuture<T> done = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                done.complete(action.get());
            } catch (Throwable t) {
                done.completeExceptionally(t);
            }
        });
        try {
            return done.get(10, TimeUnit.SECONDS);
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

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}
