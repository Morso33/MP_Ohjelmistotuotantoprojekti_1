package com.example;

/**
 * Converts temperatures between Celsius, Fahrenheit and Kelvin and
 * tells whether a Celsius reading counts as extreme.
 * Units are identified by the codes stored in the temperature_unit table: "C", "F" and "K".
 */
public class TempCalculator {

    /** Offset between the Kelvin and Celsius scales: 0 K is -273.15 C. */
    public static final double KELVIN_OFFSET = 273.15;

    /** Slack for floating point rounding when checking against absolute zero. */
    private static final double ROUNDING_TOLERANCE = 1e-9;

    /**
     * Converts a Fahrenheit temperature to Celsius.
     *
     * @param fahrenheit temperature in degrees Fahrenheit
     * @return the same temperature in degrees Celsius
     */
    public double fahrenheitToCelsius(double fahrenheit) {
        return (fahrenheit - 32) * 5 / 9;
    }

    /**
     * Converts a Celsius temperature to Fahrenheit.
     *
     * @param celsius temperature in degrees Celsius
     * @return the same temperature in degrees Fahrenheit
     */
    public double celsiusToFahrenheit(double celsius) {
        return (celsius * 9 / 5) + 32;
    }

    /**
     * Converts a Kelvin temperature to Celsius.
     * Formula: C = K - 273.15, e.g. 300 K -> 26.85 C.
     *
     * @param kelvin temperature in kelvin
     * @return the same temperature in degrees Celsius
     */
    public double kelvinToCelsius(double kelvin) {
        return kelvin - KELVIN_OFFSET;
    }

    /**
     * Converts a Celsius temperature to Kelvin.
     *
     * @param celsius temperature in degrees Celsius
     * @return the same temperature in kelvin
     */
    public double celsiusToKelvin(double celsius) {
        return celsius + KELVIN_OFFSET;
    }

    /**
     * Converts a temperature from one unit to another, going through Celsius.
     *
     * @param value    the temperature to convert
     * @param fromCode unit code of {@code value}: "C", "F" or "K"
     * @param toCode   unit code of the result: "C", "F" or "K"
     * @return the converted temperature
     * @throws IllegalArgumentException if a unit code is unknown
     */
    public double convert(double value, String fromCode, String toCode) {
        return fromCelsius(toCelsius(value, fromCode), toCode);
    }

    /**
     * Tells whether a temperature is colder than absolute zero, which cannot exist.
     *
     * @param value the temperature
     * @param code  unit code of {@code value}
     * @return true if the temperature is below 0 K
     */
    public boolean isBelowAbsoluteZero(double value, String code) {
        return toCelsius(value, code) < -KELVIN_OFFSET - ROUNDING_TOLERANCE;
    }

    /**
     * Tells whether a Celsius temperature is extreme.
     *
     * @param celsius temperature in degrees Celsius
     * @return true if the temperature is below -40 C or above 50 C
     */
    public boolean isExtremeTemperature(double celsius) {
        return celsius < -40 || celsius > 50;
    }

    /**
     * Reads a temperature typed by the user. A comma works as the decimal separator.
     *
     * @param text the text from the input field
     * @return the number
     * @throws NumberFormatException if the text is empty or not a finite number
     */
    public double parseTemperature(String text) {
        if (text == null || text.isBlank()) {
            throw new NumberFormatException("Enter a temperature");
        }
        double value = Double.parseDouble(text.trim().replace(',', '.'));
        if (!Double.isFinite(value)) {
            throw new NumberFormatException("Not a finite number: " + text.trim());
        }
        return value;
    }

    private double toCelsius(double value, String code) {
        return switch (code) {
            case "C" -> value;
            case "F" -> fahrenheitToCelsius(value);
            case "K" -> kelvinToCelsius(value);
            default -> throw new IllegalArgumentException("Unknown temperature unit: " + code);
        };
    }

    private double fromCelsius(double celsius, String code) {
        return switch (code) {
            case "C" -> celsius;
            case "F" -> celsiusToFahrenheit(celsius);
            case "K" -> celsiusToKelvin(celsius);
            default -> throw new IllegalArgumentException("Unknown temperature unit: " + code);
        };
    }
}
