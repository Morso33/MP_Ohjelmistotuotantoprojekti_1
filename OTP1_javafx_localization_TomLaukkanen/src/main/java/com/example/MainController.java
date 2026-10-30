package com.example;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DecimalStyle;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Controller of main-view.fxml. The FXML file takes its texts from the resource bundle;
 * this class fills in the units and the history from the database, converts the value the
 * user types, saves every conversion and writes all messages and numbers in the chosen language.
 */
public class MainController {

    /** What the user has typed and chosen, carried over when the window is reloaded in another language. */
    record State(String value, TemperatureUnit from, TemperatureUnit to, Conversion lastConversion) {
    }

    /** Invisible Unicode character that makes the text next to it run left to right. */
    static final String LEFT_TO_RIGHT_MARK = "‎";

    private final TempCalculator calculator = new TempCalculator();
    private final DBConnection database;
    private final TemperatureUnitDAO unitDao;
    private final ConversionDAO conversionDao;
    private final Language language;
    private final Consumer<Language> languageSwitcher;

    /** The conversion shown in the result label, so it can be shown again in another language. */
    private Conversion lastConversion;

    // Injected by the FXMLLoader. Package-private so the tests can use the form like a user.
    @FXML
    ResourceBundle resources;
    @FXML
    Label titleLabel;
    @FXML
    ComboBox<Language> languageBox;
    @FXML
    TextField valueField;
    @FXML
    ComboBox<TemperatureUnit> fromBox;
    @FXML
    ComboBox<TemperatureUnit> toBox;
    @FXML
    Button swapButton;
    @FXML
    Button convertButton;
    @FXML
    Label resultLabel;
    @FXML
    Label messageLabel;
    @FXML
    TableView<Conversion> historyTable;
    @FXML
    TableColumn<Conversion, String> timeColumn;
    @FXML
    TableColumn<Conversion, String> fromColumn;
    @FXML
    TableColumn<Conversion, String> toColumn;
    @FXML
    TableColumn<Conversion, String> languageColumn;
    @FXML
    Button clearButton;
    @FXML
    Label databaseLabel;

    /**
     * @param database         where the conversions are saved
     * @param language         the language the window is shown in
     * @param languageSwitcher called when the user picks another language
     */
    MainController(DBConnection database, Language language, Consumer<Language> languageSwitcher) {
        this.database = database;
        this.unitDao = new TemperatureUnitDAO(database);
        this.conversionDao = new ConversionDAO(database);
        this.language = language;
        this.languageSwitcher = languageSwitcher;
    }

    /** Called by the FXMLLoader once the fields above are filled in. */
    @FXML
    void initialize() {
        languageBox.getItems().setAll(Language.values());
        languageBox.setValue(language);
        languageBox.setOnAction(e -> {
            Language selected = languageBox.getValue();
            if (selected != null && selected != language) {
                // Reloading replaces this window's contents, so let the combo box finish first
                Platform.runLater(() -> languageSwitcher.accept(selected));
            }
        });

        StringConverter<TemperatureUnit> unitNames = new StringConverter<>() {
            @Override
            public String toString(TemperatureUnit unit) {
                return unit == null ? "" : unitName(unit) + " (" + leftToRight(unit.getSymbol(), language) + ")";
            }

            @Override
            public TemperatureUnit fromString(String text) {
                throw new UnsupportedOperationException("The unit lists are not editable");
            }
        };
        fromBox.setConverter(unitNames);
        toBox.setConverter(unitNames);

        setCellText(timeColumn, c -> formatTime(c.getCreatedAt(), language.getLocale()));
        setCellText(fromColumn, c -> formatValue(c.getInputValue(), c.getFromUnit()));
        setCellText(toColumn, c -> formatValue(c.getResultValue(), c.getToUnit()));
        setCellText(languageColumn, c -> Language.fromCode(c.getLanguageCode()).getNativeName());

        databaseLabel.setText(message("label.database", leftToRight(database.getUrl(), language)));
        connectDatabase();
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
            showError(message("error.connect", leftToRight(e.getMessage(), language)));
            valueField.setDisable(true);
            convertButton.setDisable(true);
            swapButton.setDisable(true);
            clearButton.setDisable(true);
        }
    }

    /** Converts the typed value, shows the result and saves it to the database. */
    @FXML
    void convert() {
        TemperatureUnit from = fromBox.getValue();
        TemperatureUnit to = toBox.getValue();
        String text = valueField.getText() == null ? "" : valueField.getText().strip();

        double value;
        try {
            value = calculator.parseTemperature(text);
        } catch (NumberFormatException e) {
            showError(text.isEmpty() ? message("error.empty") : message("error.number", text));
            return;
        }
        if (calculator.isBelowAbsoluteZero(value, from.getCode())) {
            showError(message("error.absoluteZero", formatValue(value, from)));
            return;
        }

        double result = calculator.convert(value, from.getCode(), to.getCode());
        Conversion conversion = new Conversion(value, from, result, to, language.getCode());
        showResult(conversion);

        try {
            conversionDao.save(conversion);
            refreshHistory();
        } catch (SQLException e) {
            showError(message("error.save", leftToRight(e.getMessage(), language)));
            return;
        }

        boolean extreme = calculator.isExtremeTemperature(calculator.convert(value, from.getCode(), "C"));
        showInfo(message(extreme ? "message.saved.extreme" : "message.saved"));
    }

    /** Swaps the "from" and "to" units. */
    @FXML
    void swapUnits() {
        TemperatureUnit from = fromBox.getValue();
        fromBox.setValue(toBox.getValue());
        toBox.setValue(from);
    }

    /** Deletes all saved conversions. */
    @FXML
    void clearHistory() {
        try {
            int deleted = conversionDao.deleteAll();
            refreshHistory();
            showInfo(message("message.deleted", deleted));
        } catch (SQLException e) {
            showError(message("error.clear", leftToRight(e.getMessage(), language)));
        }
    }

    State getState() {
        return new State(valueField.getText(), fromBox.getValue(), toBox.getValue(), lastConversion);
    }

    /** Puts back what the user had typed and chosen before the language was switched. */
    void restoreState(State state) {
        valueField.setText(state.value());
        if (fromBox.getItems().contains(state.from()) && toBox.getItems().contains(state.to())) {
            fromBox.setValue(state.from());
            toBox.setValue(state.to());
        }
        if (state.lastConversion() != null) {
            showResult(state.lastConversion());
        }
    }

    private void showResult(Conversion conversion) {
        lastConversion = conversion;
        resultLabel.setText(message("result.text",
                formatValue(conversion.getInputValue(), conversion.getFromUnit()),
                formatValue(conversion.getResultValue(), conversion.getToUnit())));
    }

    private void refreshHistory() throws SQLException {
        historyTable.getItems().setAll(conversionDao.findAll());
    }

    private void showInfo(String message) {
        messageLabel.setStyle("-fx-text-fill: #1a7f37;");
        messageLabel.setText(message);
    }

    private void showError(String message) {
        messageLabel.setStyle("-fx-text-fill: #cf222e;");
        messageLabel.setText(message);
    }

    /** The unit's name in the window's language, or the name from the database if the bundle has none. */
    String unitName(TemperatureUnit unit) {
        String key = "unit." + unit.getCode();
        return resources.containsKey(key) ? resources.getString(key) : unit.getName();
    }

    /** A text from the bundle, with the arguments formatted the way the window's language writes them. */
    String message(String key, Object... args) {
        return new MessageFormat(resources.getString(key), language.getLocale()).format(args);
    }

    /** A temperature with two decimals and the unit symbol, e.g. "21,50 °C" in Finnish. */
    String formatValue(double value, TemperatureUnit unit) {
        return formatValue(value, unit, language);
    }

    static String formatValue(double value, TemperatureUnit unit, Language language) {
        NumberFormat number = NumberFormat.getNumberInstance(language.getLocale());
        number.setMinimumFractionDigits(2);
        number.setMaximumFractionDigits(2);
        number.setGroupingUsed(false);
        return withoutRightToLeftMarks(number.format(value)) + " " + leftToRight(unit.getSymbol(), language);
    }

    /**
     * Removes the marks Java puts into Arabic numbers and dates: U+061C before a minus sign and
     * U+200F between the parts of a date. The Linux fonts in the Docker image draw them as boxes,
     * and the minus sign and the date parts are placed right without them.
     */
    static String withoutRightToLeftMarks(String text) {
        return text.replace("؜", "").replace("‏", "");
    }

    /**
     * Marks Latin text such as "°C" or a database URL as left to right when the window is in a
     * right-to-left language. Without the marks Arabic text around it would show "°C" as "C°".
     */
    static String leftToRight(String text, Language language) {
        return language.isRightToLeft() ? LEFT_TO_RIGHT_MARK + text + LEFT_TO_RIGHT_MARK : text;
    }

    /** A date and time written the way the locale writes them, with the locale's own digits. */
    static String formatTime(LocalDateTime time, Locale locale) {
        return withoutRightToLeftMarks(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.MEDIUM)
                .withLocale(locale)
                .withDecimalStyle(DecimalStyle.of(locale))
                .format(time));
    }

    private static void setCellText(TableColumn<Conversion, String> column, Function<Conversion, String> text) {
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(text.apply(cell.getValue())));
    }
}
