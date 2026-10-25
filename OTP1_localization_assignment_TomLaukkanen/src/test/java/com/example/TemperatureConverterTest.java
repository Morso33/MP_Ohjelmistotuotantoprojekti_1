package com.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemperatureConverterTest {

    private static final double DELTA = 0.0001;

    private TemperatureConverter converter;

    @BeforeEach
    void setUp() {
        converter = new TemperatureConverter();
    }

    // --- fahrenheitToCelsius ---

    @ParameterizedTest(name = "{0} °F = {1} °C")
    @CsvSource({
            "32, 0",          // water freezes
            "212, 100",       // water boils
            "98.6, 37",       // body temperature
            "-40, -40",       // the scales meet
            "0, -17.7778",
            "50, 10",
            "-459.67, -273.15" // absolute zero
    })
    void fahrenheitToCelsius(double fahrenheit, double expectedCelsius) {
        assertEquals(expectedCelsius, converter.fahrenheitToCelsius(fahrenheit), DELTA);
    }

    // --- celsiusToFahrenheit ---

    @ParameterizedTest(name = "{0} °C = {1} °F")
    @CsvSource({
            "0, 32",
            "100, 212",
            "37, 98.6",
            "-40, -40",
            "25, 77",
            "-17.7778, 0",
            "-273.15, -459.67"
    })
    void celsiusToFahrenheit(double celsius, double expectedFahrenheit) {
        assertEquals(expectedFahrenheit, converter.celsiusToFahrenheit(celsius), DELTA);
    }

    @ParameterizedTest(name = "{0} °C survives a round trip")
    @ValueSource(doubles = {-100, -40, 0, 21.5, 50, 1000})
    void roundTripGivesOriginalValue(double celsius) {
        double fahrenheit = converter.celsiusToFahrenheit(celsius);
        assertEquals(celsius, converter.fahrenheitToCelsius(fahrenheit), DELTA);
    }

    // --- isExtremeTemperature: boundaries and edge cases ---

    @Test
    @DisplayName("-40 °C is the cold limit and is not extreme")
    void minus40IsNotExtreme() {
        assertFalse(converter.isExtremeTemperature(-40));
    }

    @Test
    @DisplayName("50 °C is the heat limit and is not extreme")
    void plus50IsNotExtreme() {
        assertFalse(converter.isExtremeTemperature(50));
    }

    @Test
    @DisplayName("-41 °C is extreme cold")
    void minus41IsExtreme() {
        assertTrue(converter.isExtremeTemperature(-41));
    }

    @Test
    @DisplayName("51 °C is extreme heat")
    void plus51IsExtreme() {
        assertTrue(converter.isExtremeTemperature(51));
    }

    @ParameterizedTest(name = "{0} °C is extreme")
    @ValueSource(doubles = {-40.01, -273.15, 50.01, 100, 1000})
    void valuesJustOutsideTheLimitsAreExtreme(double celsius) {
        assertTrue(converter.isExtremeTemperature(celsius));
    }

    @ParameterizedTest(name = "{0} °C is not extreme")
    @ValueSource(doubles = {-39.99, -20, 0, 20, 37, 49.99})
    void valuesInsideTheLimitsAreNotExtreme(double celsius) {
        assertFalse(converter.isExtremeTemperature(celsius));
    }
}
