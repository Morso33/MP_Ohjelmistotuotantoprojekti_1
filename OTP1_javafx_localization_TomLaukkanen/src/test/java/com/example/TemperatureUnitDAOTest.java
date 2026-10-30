package com.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemperatureUnitDAOTest {

    private TemperatureUnitDAO dao;

    @BeforeEach
    void setUp() throws SQLException {
        dao = new TemperatureUnitDAO(TestDatabase.withSchema());
    }

    @Test
    @DisplayName("findAll returns Celsius, Fahrenheit and Kelvin in that order")
    void findAll() throws SQLException {
        List<TemperatureUnit> units = dao.findAll();

        assertEquals(3, units.size());
        assertEquals(List.of("C", "F", "K"), units.stream().map(TemperatureUnit::getCode).toList());
        assertEquals(List.of("Celsius", "Fahrenheit", "Kelvin"), units.stream().map(TemperatureUnit::getName).toList());
        assertEquals(List.of("°C", "°F", "K"), units.stream().map(TemperatureUnit::getSymbol).toList());
    }

    @Test
    @DisplayName("findByCode finds a unit by its code")
    void findByCode() throws SQLException {
        TemperatureUnit kelvin = dao.findByCode("K").orElseThrow();
        assertEquals("Kelvin", kelvin.getName());
        assertTrue(kelvin.getId() > 0);
    }

    @Test
    @DisplayName("findByCode is empty for an unknown code")
    void findByUnknownCode() throws SQLException {
        assertEquals(Optional.empty(), dao.findByCode("X"));
    }

    @Test
    @DisplayName("findById returns the same unit as findByCode")
    void findById() throws SQLException {
        TemperatureUnit fahrenheit = dao.findByCode("F").orElseThrow();
        assertEquals(Optional.of(fahrenheit), dao.findById(fahrenheit.getId()));
    }

    @Test
    @DisplayName("findById is empty for an id that does not exist")
    void findByUnknownId() throws SQLException {
        assertEquals(Optional.empty(), dao.findById(999));
    }

    @Test
    @DisplayName("Reading fails with SQLException when the table is missing")
    void missingTable() {
        TemperatureUnitDAO daoWithoutTables = new TemperatureUnitDAO(TestDatabase.empty());
        assertThrows(SQLException.class, daoWithoutTables::findAll);
        assertThrows(SQLException.class, () -> daoWithoutTables.findByCode("C"));
    }
}
