package com.example;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class TemperatureUnitTest {

    private final TemperatureUnit celsius = new TemperatureUnit(1, "C", "Celsius", "°C");

    @Test
    @DisplayName("Getters return the constructor values")
    void getters() {
        assertEquals(1, celsius.getId());
        assertEquals("C", celsius.getCode());
        assertEquals("Celsius", celsius.getName());
        assertEquals("°C", celsius.getSymbol());
    }

    @Test
    @DisplayName("toString is the text shown in the unit drop-downs")
    void toStringShowsNameAndSymbol() {
        assertEquals("Celsius (°C)", celsius.toString());
        assertEquals("Kelvin (K)", new TemperatureUnit(3, "K", "Kelvin", "K").toString());
    }

    @Test
    @DisplayName("Units with the same id and code are equal")
    void equalUnits() {
        TemperatureUnit sameRow = new TemperatureUnit(1, "C", "Celsius", "°C");
        assertEquals(celsius, celsius);
        assertEquals(celsius, sameRow);
        assertEquals(celsius.hashCode(), sameRow.hashCode());
    }

    @Test
    @DisplayName("Units with a different id or code are not equal")
    void differentUnits() {
        assertNotEquals(celsius, new TemperatureUnit(2, "C", "Celsius", "°C"));
        assertNotEquals(celsius, new TemperatureUnit(1, "F", "Fahrenheit", "°F"));
        assertNotEquals(celsius, null);
        assertNotEquals(celsius, "C");
    }
}
