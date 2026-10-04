package com.example;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TempRecordTest {

    private final TemperatureUnit celsius = new TemperatureUnit(1, "C", "Celsius", "°C");
    private final TemperatureUnit fahrenheit = new TemperatureUnit(2, "F", "Fahrenheit", "°F");

    @Test
    @DisplayName("A new record has no id yet and is stamped with the current time")
    void newRecord() {
        LocalDateTime before = LocalDateTime.now().withNano(0);
        TempRecord record = new TempRecord(100, celsius, 212, fahrenheit);
        LocalDateTime after = LocalDateTime.now();

        assertEquals(0, record.getId());
        assertEquals(100, record.getInputValue());
        assertSame(celsius, record.getFromUnit());
        assertEquals(212, record.getResultValue());
        assertSame(fahrenheit, record.getToUnit());
        assertFalse(record.getCreatedAt().isBefore(before));
        assertFalse(record.getCreatedAt().isAfter(after));
        assertEquals(0, record.getCreatedAt().getNano(), "stored to whole seconds like the DATETIME column");
    }

    @Test
    @DisplayName("A record read from the database keeps all its values")
    void recordFromDatabase() {
        LocalDateTime time = LocalDateTime.of(2026, 10, 4, 12, 30, 15);
        TempRecord record = new TempRecord(7, 32, fahrenheit, 0, celsius, time);

        assertEquals(7, record.getId());
        assertEquals(32, record.getInputValue());
        assertSame(fahrenheit, record.getFromUnit());
        assertEquals(0, record.getResultValue());
        assertSame(celsius, record.getToUnit());
        assertEquals(time, record.getCreatedAt());
    }

    @Test
    @DisplayName("withId only changes the id")
    void withId() {
        TempRecord unsaved = new TempRecord(100, celsius, 212, fahrenheit);
        TempRecord saved = unsaved.withId(42);

        assertEquals(42, saved.getId());
        assertEquals(0, unsaved.getId(), "the original record is not modified");
        assertEquals(unsaved.getInputValue(), saved.getInputValue());
        assertSame(unsaved.getFromUnit(), saved.getFromUnit());
        assertEquals(unsaved.getResultValue(), saved.getResultValue());
        assertSame(unsaved.getToUnit(), saved.getToUnit());
        assertEquals(unsaved.getCreatedAt(), saved.getCreatedAt());
    }

    @Test
    @DisplayName("toString names the id, values and unit codes")
    void toStringDescribesRecord() {
        TempRecord record = new TempRecord(3, 100, celsius, 212, fahrenheit, LocalDateTime.of(2026, 10, 4, 9, 0));
        String text = record.toString();
        assertTrue(text.contains("id=3"), text);
        assertTrue(text.contains("100.0 C -> 212.0 F"), text);
        assertTrue(text.contains("2026-10-04T09:00"), text);
    }
}
