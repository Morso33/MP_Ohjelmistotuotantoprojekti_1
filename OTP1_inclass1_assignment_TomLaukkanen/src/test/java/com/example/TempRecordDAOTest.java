package com.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TempRecordDAOTest {

    private TempRecordDAO dao;
    private TemperatureUnit celsius;
    private TemperatureUnit fahrenheit;
    private TemperatureUnit kelvin;

    @BeforeEach
    void setUp() throws SQLException {
        DBConnection database = TestDatabase.withSchema();
        dao = new TempRecordDAO(database);
        TemperatureUnitDAO unitDao = new TemperatureUnitDAO(database);
        celsius = unitDao.findByCode("C").orElseThrow();
        fahrenheit = unitDao.findByCode("F").orElseThrow();
        kelvin = unitDao.findByCode("K").orElseThrow();
    }

    @Test
    @DisplayName("save gives the record an id")
    void saveAssignsId() throws SQLException {
        TempRecord saved = dao.save(new TempRecord(100, celsius, 212, fahrenheit));
        assertTrue(saved.getId() > 0);
    }

    @Test
    @DisplayName("A saved record is read back with both units joined in")
    void saveAndFindAll() throws SQLException {
        LocalDateTime time = LocalDateTime.of(2026, 10, 4, 18, 45, 30);
        TempRecord saved = dao.save(new TempRecord(0, 300, kelvin, 26.85, celsius, time));

        List<TempRecord> records = dao.findAll();

        assertEquals(1, records.size());
        TempRecord read = records.get(0);
        assertEquals(saved.getId(), read.getId());
        assertEquals(300, read.getInputValue());
        assertEquals(kelvin, read.getFromUnit());
        assertEquals("Kelvin", read.getFromUnit().getName());
        assertEquals(26.85, read.getResultValue());
        assertEquals(celsius, read.getToUnit());
        assertEquals("°C", read.getToUnit().getSymbol());
        assertEquals(time, read.getCreatedAt());
    }

    @Test
    @DisplayName("findAll lists the newest record first")
    void newestFirst() throws SQLException {
        TempRecord first = dao.save(new TempRecord(0, celsius, 32, fahrenheit));
        TempRecord second = dao.save(new TempRecord(212, fahrenheit, 100, celsius));
        TempRecord third = dao.save(new TempRecord(0, kelvin, -273.15, celsius));

        List<Integer> ids = dao.findAll().stream().map(TempRecord::getId).toList();

        assertEquals(List.of(third.getId(), second.getId(), first.getId()), ids);
    }

    @Test
    @DisplayName("findAll is empty when nothing is saved")
    void emptyHistory() throws SQLException {
        assertTrue(dao.findAll().isEmpty());
    }

    @Test
    @DisplayName("deleteAll removes every record and reports how many")
    void deleteAll() throws SQLException {
        dao.save(new TempRecord(1, celsius, 33.8, fahrenheit));
        dao.save(new TempRecord(2, celsius, 35.6, fahrenheit));

        assertEquals(2, dao.deleteAll());
        assertTrue(dao.findAll().isEmpty());
        assertEquals(0, dao.deleteAll());
    }

    @Test
    @DisplayName("The foreign key rejects a unit that is not in temperature_unit")
    void unknownUnitRejected() {
        TemperatureUnit rankine = new TemperatureUnit(99, "R", "Rankine", "°R");
        assertThrows(SQLException.class, () -> dao.save(new TempRecord(1, rankine, 1, celsius)));
        assertThrows(SQLException.class, () -> dao.save(new TempRecord(1, celsius, 1, rankine)));
    }

    @Test
    @DisplayName("Every method fails with SQLException when the table is missing")
    void missingTable() {
        TempRecordDAO daoWithoutTables = new TempRecordDAO(TestDatabase.empty());
        assertThrows(SQLException.class, () -> daoWithoutTables.save(new TempRecord(1, celsius, 33.8, fahrenheit)));
        assertThrows(SQLException.class, daoWithoutTables::findAll);
        assertThrows(SQLException.class, daoWithoutTables::deleteAll);
    }
}
