package com.example;

import java.time.LocalDateTime;

/**
 * One saved conversion, a row of the temp_record table. Both units point to rows
 * of the temperature_unit table.
 */
public class TempRecord {

    private final int id;
    private final double inputValue;
    private final TemperatureUnit fromUnit;
    private final double resultValue;
    private final TemperatureUnit toUnit;
    private final LocalDateTime createdAt;

    /**
     * Creates a record that has not been saved yet. The id is 0 and the time is now.
     */
    public TempRecord(double inputValue, TemperatureUnit fromUnit, double resultValue, TemperatureUnit toUnit) {
        this(0, inputValue, fromUnit, resultValue, toUnit, LocalDateTime.now().withNano(0));
    }

    /**
     * Creates a record as read from the database.
     */
    public TempRecord(int id, double inputValue, TemperatureUnit fromUnit, double resultValue,
                      TemperatureUnit toUnit, LocalDateTime createdAt) {
        this.id = id;
        this.inputValue = inputValue;
        this.fromUnit = fromUnit;
        this.resultValue = resultValue;
        this.toUnit = toUnit;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /** The same record with the id the database gave it. */
    public TempRecord withId(int newId) {
        return new TempRecord(newId, inputValue, fromUnit, resultValue, toUnit, createdAt);
    }

    @Override
    public String toString() {
        return "TempRecord{id=" + id + ", " + inputValue + " " + fromUnit.getCode()
                + " -> " + resultValue + " " + toUnit.getCode() + ", createdAt=" + createdAt + "}";
    }
}
