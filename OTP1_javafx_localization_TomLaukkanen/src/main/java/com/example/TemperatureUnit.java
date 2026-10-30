package com.example;

import java.util.Objects;

/**
 * A temperature unit, one row of the temperature_unit table, e.g. Celsius with code "C" and symbol "°C".
 */
public class TemperatureUnit {

    private final int id;
    private final String code;
    private final String name;
    private final String symbol;

    /**
     * @param id     primary key in the temperature_unit table
     * @param code   short code used by {@link TempCalculator}: "C", "F" or "K"
     * @param name   name shown to the user, e.g. "Celsius"
     * @param symbol symbol printed after a value, e.g. "°C"
     */
    public TemperatureUnit(int id, String code, String name, String symbol) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.symbol = symbol;
    }

    public int getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getSymbol() {
        return symbol;
    }

    /** Shown in the unit drop-downs, e.g. "Celsius (°C)". */
    @Override
    public String toString() {
        return name + " (" + symbol + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TemperatureUnit other)) {
            return false;
        }
        return id == other.id && Objects.equals(code, other.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, code);
    }
}
