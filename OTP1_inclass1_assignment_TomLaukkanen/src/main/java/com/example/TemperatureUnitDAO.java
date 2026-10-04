package com.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Reads temperature units from the temperature_unit table.
 */
public class TemperatureUnitDAO {

    private final DBConnection database;

    public TemperatureUnitDAO(DBConnection database) {
        this.database = database;
    }

    /** All units in the order they were added: Celsius, Fahrenheit, Kelvin. */
    public List<TemperatureUnit> findAll() throws SQLException {
        String sql = "SELECT id, code, name, symbol FROM temperature_unit ORDER BY id";
        List<TemperatureUnit> units = new ArrayList<>();
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                units.add(fromRow(rs));
            }
        }
        return units;
    }

    /** The unit with the given code ("C", "F" or "K"), or empty if there is none. */
    public Optional<TemperatureUnit> findByCode(String code) throws SQLException {
        return findOne("SELECT id, code, name, symbol FROM temperature_unit WHERE code = ?", code);
    }

    /** The unit with the given id, or empty if there is none. */
    public Optional<TemperatureUnit> findById(int id) throws SQLException {
        return findOne("SELECT id, code, name, symbol FROM temperature_unit WHERE id = ?", id);
    }

    private Optional<TemperatureUnit> findOne(String sql, Object key) throws SQLException {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, key);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(fromRow(rs)) : Optional.empty();
            }
        }
    }

    private static TemperatureUnit fromRow(ResultSet rs) throws SQLException {
        return new TemperatureUnit(rs.getInt("id"), rs.getString("code"),
                rs.getString("name"), rs.getString("symbol"));
    }
}
