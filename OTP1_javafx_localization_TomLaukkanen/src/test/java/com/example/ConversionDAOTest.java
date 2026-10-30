package com.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversionDAOTest {

    private ConversionDAO dao;
    private TemperatureUnit celsius;
    private TemperatureUnit fahrenheit;
    private TemperatureUnit kelvin;

    @BeforeEach
    void setUp() throws SQLException {
        DBConnection database = TestDatabase.withSchema();
        dao = new ConversionDAO(database);
        TemperatureUnitDAO unitDao = new TemperatureUnitDAO(database);
        celsius = unitDao.findByCode("C").orElseThrow();
        fahrenheit = unitDao.findByCode("F").orElseThrow();
        kelvin = unitDao.findByCode("K").orElseThrow();
    }

    @Test
    @DisplayName("save gives the conversion an id")
    void saveAssignsId() throws SQLException {
        Conversion saved = dao.save(new Conversion(100, celsius, 212, fahrenheit, "en_US"));
        assertTrue(saved.getId() > 0);
    }

    @Test
    @DisplayName("A saved conversion is read back with both units and the language joined in")
    void saveAndFindAll() throws SQLException {
        LocalDateTime time = LocalDateTime.of(2026, 10, 30, 18, 45, 30);
        Conversion saved = dao.save(new Conversion(0, 300, kelvin, 26.85, celsius, "ja_JP", time));

        List<Conversion> conversions = dao.findAll();

        assertEquals(1, conversions.size());
        Conversion read = conversions.get(0);
        assertEquals(saved.getId(), read.getId());
        assertEquals(300, read.getInputValue());
        assertEquals(kelvin, read.getFromUnit());
        assertEquals("Kelvin", read.getFromUnit().getName());
        assertEquals(26.85, read.getResultValue());
        assertEquals(celsius, read.getToUnit());
        assertEquals("°C", read.getToUnit().getSymbol());
        assertEquals("ja_JP", read.getLanguageCode());
        assertEquals(time, read.getCreatedAt());
    }

    @ParameterizedTest(name = "a conversion made in {0} can be saved")
    @EnumSource(Language.class)
    void everyLanguageCanBeSaved(Language language) throws SQLException {
        dao.save(new Conversion(20, celsius, 68, fahrenheit, language.getCode()));
        assertEquals(language.getCode(), dao.findAll().get(0).getLanguageCode());
    }

    @Test
    @DisplayName("findAll lists the newest conversion first")
    void newestFirst() throws SQLException {
        Conversion first = dao.save(new Conversion(0, celsius, 32, fahrenheit, "en_US"));
        Conversion second = dao.save(new Conversion(212, fahrenheit, 100, celsius, "fi_FI"));
        Conversion third = dao.save(new Conversion(0, kelvin, -273.15, celsius, "sv_SE"));

        List<Integer> ids = dao.findAll().stream().map(Conversion::getId).toList();

        assertEquals(List.of(third.getId(), second.getId(), first.getId()), ids);
    }

    @Test
    @DisplayName("findAll is empty when nothing is saved")
    void emptyHistory() throws SQLException {
        assertTrue(dao.findAll().isEmpty());
    }

    @Test
    @DisplayName("deleteAll removes every conversion and reports how many")
    void deleteAll() throws SQLException {
        dao.save(new Conversion(1, celsius, 33.8, fahrenheit, "en_US"));
        dao.save(new Conversion(2, celsius, 35.6, fahrenheit, "ar_SA"));

        assertEquals(2, dao.deleteAll());
        assertTrue(dao.findAll().isEmpty());
        assertEquals(0, dao.deleteAll());
    }

    @Test
    @DisplayName("The foreign keys reject a unit that is not in temperature_unit")
    void unknownUnitRejected() {
        TemperatureUnit rankine = new TemperatureUnit(99, "R", "Rankine", "°R");
        assertThrows(SQLException.class, () -> dao.save(new Conversion(1, rankine, 1, celsius, "en_US")));
        assertThrows(SQLException.class, () -> dao.save(new Conversion(1, celsius, 1, rankine, "en_US")));
    }

    @Test
    @DisplayName("A language that is not in ui_language is rejected")
    void unknownLanguageRejected() throws SQLException {
        assertThrows(SQLException.class, () -> dao.save(new Conversion(1, celsius, 33.8, fahrenheit, "de_DE")));
        assertTrue(dao.findAll().isEmpty());
    }

    @Test
    @DisplayName("Every method fails with SQLException when the table is missing")
    void missingTable() {
        ConversionDAO daoWithoutTables = new ConversionDAO(TestDatabase.empty());
        assertThrows(SQLException.class, () -> daoWithoutTables.save(new Conversion(1, celsius, 33.8, fahrenheit, "en_US")));
        assertThrows(SQLException.class, daoWithoutTables::findAll);
        assertThrows(SQLException.class, daoWithoutTables::deleteAll);
    }
}
