package com.example;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversionTest {

    private final TemperatureUnit celsius = new TemperatureUnit(1, "C", "Celsius", "°C");
    private final TemperatureUnit fahrenheit = new TemperatureUnit(2, "F", "Fahrenheit", "°F");

    @Test
    @DisplayName("A new conversion has no id yet and is stamped with the current time")
    void newConversion() {
        LocalDateTime before = LocalDateTime.now().withNano(0);
        Conversion conversion = new Conversion(100, celsius, 212, fahrenheit, "fi_FI");
        LocalDateTime after = LocalDateTime.now();

        assertEquals(0, conversion.getId());
        assertEquals(100, conversion.getInputValue());
        assertSame(celsius, conversion.getFromUnit());
        assertEquals(212, conversion.getResultValue());
        assertSame(fahrenheit, conversion.getToUnit());
        assertEquals("fi_FI", conversion.getLanguageCode());
        assertFalse(conversion.getCreatedAt().isBefore(before));
        assertFalse(conversion.getCreatedAt().isAfter(after));
        assertEquals(0, conversion.getCreatedAt().getNano(), "stored to whole seconds like the DATETIME column");
    }

    @Test
    @DisplayName("A conversion read from the database keeps all its values")
    void conversionFromDatabase() {
        LocalDateTime time = LocalDateTime.of(2026, 10, 30, 12, 30, 15);
        Conversion conversion = new Conversion(7, 32, fahrenheit, 0, celsius, "ja_JP", time);

        assertEquals(7, conversion.getId());
        assertEquals(32, conversion.getInputValue());
        assertSame(fahrenheit, conversion.getFromUnit());
        assertEquals(0, conversion.getResultValue());
        assertSame(celsius, conversion.getToUnit());
        assertEquals("ja_JP", conversion.getLanguageCode());
        assertEquals(time, conversion.getCreatedAt());
    }

    @Test
    @DisplayName("withId only changes the id")
    void withId() {
        Conversion unsaved = new Conversion(100, celsius, 212, fahrenheit, "ar_SA");
        Conversion saved = unsaved.withId(42);

        assertEquals(42, saved.getId());
        assertEquals(0, unsaved.getId(), "the original conversion is not modified");
        assertEquals(unsaved.getInputValue(), saved.getInputValue());
        assertSame(unsaved.getFromUnit(), saved.getFromUnit());
        assertEquals(unsaved.getResultValue(), saved.getResultValue());
        assertSame(unsaved.getToUnit(), saved.getToUnit());
        assertEquals(unsaved.getLanguageCode(), saved.getLanguageCode());
        assertEquals(unsaved.getCreatedAt(), saved.getCreatedAt());
    }

    @Test
    @DisplayName("toString names the id, values, unit codes and language")
    void toStringDescribesConversion() {
        Conversion conversion = new Conversion(3, 100, celsius, 212, fahrenheit, "sv_SE",
                LocalDateTime.of(2026, 10, 30, 9, 0));
        String text = conversion.toString();
        assertTrue(text.contains("id=3"), text);
        assertTrue(text.contains("100.0 C -> 212.0 F"), text);
        assertTrue(text.contains("language=sv_SE"), text);
        assertTrue(text.contains("2026-10-30T09:00"), text);
    }
}
