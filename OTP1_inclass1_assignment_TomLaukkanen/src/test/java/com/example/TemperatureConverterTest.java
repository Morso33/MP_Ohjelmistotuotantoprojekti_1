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

    /** Tolerance for comparing doubles. Taken from StackOverflow: https://stackoverflow.com/questions/19280468/good-tolerance-for-double-comparison */
    private static final double DELTA = 0.0001;

    private TemperatureConverter converter;

    @BeforeEach
    void setUp() {
        converter = new TemperatureConverter();
    }

    // ---------- fahrenheitToCelsius ----------

    @Test
    @DisplayName("32 F is the freezing point, 0 C")
    void fahrenheitToCelsius_freezingPoint() {
        assertEquals(0.0, converter.fahrenheitToCelsius(32), DELTA);
    }

    @Test
    @DisplayName("212 F is the boiling point, 100 C")
    void fahrenheitToCelsius_boilingPoint() {
        assertEquals(100.0, converter.fahrenheitToCelsius(212), DELTA);
    }

    @Test
    @DisplayName("-40 F and -40 C are the same temperature")
    void fahrenheitToCelsius_scalesMeetAtMinus40() {
        assertEquals(-40.0, converter.fahrenheitToCelsius(-40), DELTA);
    }

    @ParameterizedTest(name = "{0} F -> {1} C")
    @CsvSource({
            "32,    0.0",
            "212,   100.0",
            "-40,   -40.0",
            "98.6,  37.0",
            "0,     -17.7778",
            "50,    10.0"
    })
    void fahrenheitToCelsius_multipleInputs(double fahrenheit, double expectedCelsius) {
        assertEquals(expectedCelsius, converter.fahrenheitToCelsius(fahrenheit), DELTA);
    }

    // ---------- celsiusToFahrenheit ----------

    @Test
    @DisplayName("0 C is the freezing point, 32 F")
    void celsiusToFahrenheit_freezingPoint() {
        assertEquals(32.0, converter.celsiusToFahrenheit(0), DELTA);
    }

    @Test
    @DisplayName("100 C is the boiling point, 212 F")
    void celsiusToFahrenheit_boilingPoint() {
        assertEquals(212.0, converter.celsiusToFahrenheit(100), DELTA);
    }

    @ParameterizedTest(name = "{0} C -> {1} F")
    @CsvSource({
            "0,      32.0",
            "100,    212.0",
            "-40,    -40.0",
            "37,     98.6",
            "-17.78, -0.004",
            "25,     77.0"
    })
    void celsiusToFahrenheit_multipleInputs(double celsius, double expectedFahrenheit) {
        assertEquals(expectedFahrenheit, converter.celsiusToFahrenheit(celsius), DELTA);
    }

    @ParameterizedTest(name = "round trip at {0} C")
    @ValueSource(doubles = {-273.15, -40, 0, 21.5, 37, 100})
    void conversionsAreInverseOfEachOther(double celsius) {
        double roundTripped = converter.fahrenheitToCelsius(converter.celsiusToFahrenheit(celsius));
        assertEquals(celsius, roundTripped, DELTA);
    }

    // ---------- isExtremeTemperature ----------

    @Test
    @DisplayName("Clearly cold and clearly hot values are extreme")
    void isExtremeTemperature_outsideRange() {
        assertTrue(converter.isExtremeTemperature(-41));
        assertTrue(converter.isExtremeTemperature(-100));
        assertTrue(converter.isExtremeTemperature(51));
        assertTrue(converter.isExtremeTemperature(120));
    }

    @Test
    @DisplayName("Normal temperatures are not extreme")
    void isExtremeTemperature_insideRange() {
        assertFalse(converter.isExtremeTemperature(0));
        assertFalse(converter.isExtremeTemperature(21.5));
        assertFalse(converter.isExtremeTemperature(-39.9));
        assertFalse(converter.isExtremeTemperature(49.9));
    }

    @Test
    @DisplayName("The boundaries -40 C and 50 C themselves are not extreme")
    void isExtremeTemperature_boundariesAreInclusive() {
        assertFalse(converter.isExtremeTemperature(-40), "-40 is not below -40");
        assertFalse(converter.isExtremeTemperature(50), "50 is not above 50");
    }

    @Test
    @DisplayName("Just past the boundaries the temperature becomes extreme")
    void isExtremeTemperature_justPastBoundaries() {
        assertTrue(converter.isExtremeTemperature(-40.0001));
        assertTrue(converter.isExtremeTemperature(50.0001));
    }

    @ParameterizedTest(name = "{0} C extreme? {1}")
    @CsvSource({
            "-100,     true",
            "-40.0001, true",
            "-40,      false",
            "-39.9,    false",
            "0,        false",
            "50,       false",
            "50.0001,  true",
            "100,      true"
    })
    void isExtremeTemperature_edgeCases(double celsius, boolean expected) {
        assertEquals(expected, converter.isExtremeTemperature(celsius));
    }
}
