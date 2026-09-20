package com.example;

/**
 * Converts temperatures between Fahrenheit, Celsius and Kelvin and
 * tells whether a Celsius reading counts as extreme.
 */
public class TemperatureConverter {

    /** Offset between the Kelvin and Celsius scales: 0 K is -273.15 C. */
    public static final double KELVIN_OFFSET = 273.15;

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
     * Tells whether a Celsius temperature is extreme.
     *
     * @param celsius temperature in degrees Celsius
     * @return true if the temperature is below -40 C or above 50 C
     */
    public boolean isExtremeTemperature(double celsius) {
        return celsius < -40 || celsius > 50;
    }
}
