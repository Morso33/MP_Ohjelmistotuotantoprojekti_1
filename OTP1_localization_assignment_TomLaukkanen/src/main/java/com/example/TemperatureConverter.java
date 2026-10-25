package com.example;

/**
 * Temperature conversion formulas between Fahrenheit and Celsius.
 */
public class TemperatureConverter {

    static final double EXTREME_COLD_CELSIUS = -40;
    static final double EXTREME_HEAT_CELSIUS = 50;

    public double fahrenheitToCelsius(double fahrenheit) {
        return (fahrenheit - 32) * 5 / 9;
    }

    public double celsiusToFahrenheit(double celsius) {
        return (celsius * 9 / 5) + 32;
    }

    /**
     * Returns true below -40 °C or above 50 °C. The limits themselves are not extreme.
     */
    public boolean isExtremeTemperature(double celsius) {
        return celsius < EXTREME_COLD_CELSIUS || celsius > EXTREME_HEAT_CELSIUS;
    }
}
