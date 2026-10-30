package com.example;

import java.time.LocalDateTime;

/**
 * One saved conversion, a row of the conversion table. Both units point to rows of the
 * temperature_unit table and the language code to a row of the ui_language table.
 */
public class Conversion {

    private final int id;
    private final double inputValue;
    private final TemperatureUnit fromUnit;
    private final double resultValue;
    private final TemperatureUnit toUnit;
    private final String languageCode;
    private final LocalDateTime createdAt;

    /**
     * Creates a conversion that has not been saved yet. The id is 0 and the time is now.
     *
     * @param languageCode code of the language the window was in, e.g. "fi_FI"
     */
    public Conversion(double inputValue, TemperatureUnit fromUnit, double resultValue, TemperatureUnit toUnit,
                      String languageCode) {
        this(0, inputValue, fromUnit, resultValue, toUnit, languageCode, LocalDateTime.now().withNano(0));
    }

    /**
     * Creates a conversion as read from the database.
     */
    public Conversion(int id, double inputValue, TemperatureUnit fromUnit, double resultValue,
                      TemperatureUnit toUnit, String languageCode, LocalDateTime createdAt) {
        this.id = id;
        this.inputValue = inputValue;
        this.fromUnit = fromUnit;
        this.resultValue = resultValue;
        this.toUnit = toUnit;
        this.languageCode = languageCode;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public double getInputValue() {
        return inputValue;
    }

    public TemperatureUnit getFromUnit() {
        return fromUnit;
    }

    public double getResultValue() {
        return resultValue;
    }

    public TemperatureUnit getToUnit() {
        return toUnit;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /** The same conversion with the id the database gave it. */
    public Conversion withId(int newId) {
        return new Conversion(newId, inputValue, fromUnit, resultValue, toUnit, languageCode, createdAt);
    }

    @Override
    public String toString() {
        return "Conversion{id=" + id + ", " + inputValue + " " + fromUnit.getCode()
                + " -> " + resultValue + " " + toUnit.getCode() + ", language=" + languageCode
                + ", createdAt=" + createdAt + "}";
    }
}
