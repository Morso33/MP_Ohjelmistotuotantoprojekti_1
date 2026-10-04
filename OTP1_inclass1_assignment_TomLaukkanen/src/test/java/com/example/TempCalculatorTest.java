package com.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TempCalculatorTest {

    /** Tolerance for comparing doubles. Taken from StackOverflow: https://stackoverflow.com/questions/19280468/good-tolerance-for-double-comparison */
    private static final double DELTA = 0.0001;

    private TempCalculator converter;

    @BeforeEach
    void setUp() {
        converter = new TempCalculator();
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

    // ---------- kelvinToCelsius ----------

    @Test
    @DisplayName("300 K is 26.85 C (the example from the assignment)")
    void kelvinToCelsius_assignmentExample() {
        assertEquals(26.85, converter.kelvinToCelsius(300), DELTA);
    }

    @Test
    @DisplayName("0 K is absolute zero, -273.15 C")
    void kelvinToCelsius_absoluteZero() {
        assertEquals(-273.15, converter.kelvinToCelsius(0), DELTA);
    }

    @Test
    @DisplayName("273.15 K is the freezing point, 0 C")
    void kelvinToCelsius_freezingPoint() {
        assertEquals(0.0, converter.kelvinToCelsius(273.15), DELTA);
    }

    @Test
    @DisplayName("373.15 K is the boiling point, 100 C")
    void kelvinToCelsius_boilingPoint() {
        assertEquals(100.0, converter.kelvinToCelsius(373.15), DELTA);
    }

    @ParameterizedTest(name = "{0} K -> {1} C")
    @CsvSource({
            "0,       -273.15",
            "273.15,  0.0",
            "300,     26.85",
            "310.15,  37.0",
            "373.15,  100.0",
            "233.15,  -40.0",
            "1000,    726.85"
    })
    void kelvinToCelsius_multipleInputs(double kelvin, double expectedCelsius) {
        assertEquals(expectedCelsius, converter.kelvinToCelsius(kelvin), DELTA);
    }

    @Test
    @DisplayName("Kelvin can be chained through Celsius to Fahrenheit")
    void kelvinToCelsius_chainsWithCelsiusToFahrenheit() {
        assertEquals(212.0, converter.celsiusToFahrenheit(converter.kelvinToCelsius(373.15)), DELTA);
        assertEquals(32.0, converter.celsiusToFahrenheit(converter.kelvinToCelsius(273.15)), DELTA);
    }

    @Test
    @DisplayName("Kelvin readings feed into the extreme temperature check")
    void kelvinToCelsius_worksWithIsExtremeTemperature() {
        assertTrue(converter.isExtremeTemperature(converter.kelvinToCelsius(0)), "0 K is extremely cold");
        assertTrue(converter.isExtremeTemperature(converter.kelvinToCelsius(400)), "400 K is 126.85 C");
        assertFalse(converter.isExtremeTemperature(converter.kelvinToCelsius(300)), "300 K is a mild 26.85 C");
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

    // ---------- celsiusToKelvin ----------

    @ParameterizedTest(name = "{0} C -> {1} K")
    @CsvSource({
            "-273.15, 0.0",
            "0,       273.15",
            "26.85,   300.0",
            "100,     373.15"
    })
    void celsiusToKelvin_multipleInputs(double celsius, double expectedKelvin) {
        assertEquals(expectedKelvin, converter.celsiusToKelvin(celsius), DELTA);
    }

    // ---------- convert ----------

    @ParameterizedTest(name = "{0} {1} -> {3} {2}")
    @CsvSource({
            "100,     C, F, 212.0",
            "100,     C, K, 373.15",
            "100,     C, C, 100.0",
            "212,     F, C, 100.0",
            "32,      F, K, 273.15",
            "-459.67, F, K, 0.0",
            "98.6,    F, F, 98.6",
            "300,     K, C, 26.85",
            "0,       K, F, -459.67",
            "373.15,  K, K, 373.15"
    })
    void convert_everyPairOfUnits(double value, String from, String to, double expected) {
        assertEquals(expected, converter.convert(value, from, to), DELTA);
    }

    @Test
    @DisplayName("Converting there and back gives the original value")
    void convert_roundTrip() {
        double kelvin = converter.convert(-12.5, "F", "K");
        assertEquals(-12.5, converter.convert(kelvin, "K", "F"), DELTA);
    }

    @Test
    @DisplayName("An unknown unit code is rejected")
    void convert_unknownUnit() {
        IllegalArgumentException fromError = assertThrows(IllegalArgumentException.class,
                () -> converter.convert(1, "X", "C"));
        assertEquals("Unknown temperature unit: X", fromError.getMessage());
        assertThrows(IllegalArgumentException.class, () -> converter.convert(1, "C", "R"));
    }

    // ---------- isBelowAbsoluteZero ----------

    @ParameterizedTest(name = "{0} {1} below absolute zero? {2}")
    @CsvSource({
            "-273.15, C, false",
            "-273.16, C, true",
            "-459.67, F, false",
            "-460,    F, true",
            "0,       K, false",
            "-0.01,   K, true",
            "20,      C, false"
    })
    void isBelowAbsoluteZero_edgeCases(double value, String code, boolean expected) {
        assertEquals(expected, converter.isBelowAbsoluteZero(value, code));
    }

    // ---------- parseTemperature ----------

    @ParameterizedTest(name = "\"{0}\" -> {1}")
    @CsvSource({
            "'21.5',    21.5",
            "'37,5',    37.5",
            "'  -40  ', -40.0",
            "'1e2',     100.0"
    })
    void parseTemperature_validText(String text, double expected) {
        assertEquals(expected, converter.parseTemperature(text), DELTA);
    }

    @ParameterizedTest(name = "\"{0}\" is rejected")
    @ValueSource(strings = {"", "   ", "hot", "12abc", "NaN", "Infinity", "-Infinity"})
    void parseTemperature_invalidText(String text) {
        assertThrows(NumberFormatException.class, () -> converter.parseTemperature(text));
    }

    @Test
    @DisplayName("Missing text is rejected with a helpful message")
    void parseTemperature_null() {
        NumberFormatException e = assertThrows(NumberFormatException.class, () -> converter.parseTemperature(null));
        assertEquals("Enter a temperature", e.getMessage());
    }
}
