package com.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Saves and reads conversions in the conversion table. Reading joins the temperature_unit
 * table twice to get both units, and the ui_language table to get the language code.
 */
public class ConversionDAO {

    private static final String SELECT_WITH_UNITS = """
            SELECT c.id, c.input_value, c.result_value, c.created_at, l.code AS language_code,
                   f.id AS from_id, f.code AS from_code, f.name AS from_name, f.symbol AS from_symbol,
                   t.id AS to_id, t.code AS to_code, t.name AS to_name, t.symbol AS to_symbol
            FROM conversion c
            JOIN temperature_unit f ON c.from_unit_id = f.id
            JOIN temperature_unit t ON c.to_unit_id = t.id
            JOIN ui_language l ON c.language_id = l.id
            """;

    private final DBConnection database;

    public ConversionDAO(DBConnection database) {
        this.database = database;
    }

    /**
     * Inserts a conversion. The language is looked up from ui_language by its code.
     *
     * @return the same conversion with the id the database gave it
     * @throws SQLException if the insert fails, e.g. a unit or the language is not in its table
     */
    public Conversion save(Conversion conversion) throws SQLException {
        String sql = "INSERT INTO conversion (input_value, from_unit_id, result_value, to_unit_id, language_id, created_at) "
                + "VALUES (?, ?, ?, ?, (SELECT id FROM ui_language WHERE code = ?), ?)";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setDouble(1, conversion.getInputValue());
            statement.setInt(2, conversion.getFromUnit().getId());
            statement.setDouble(3, conversion.getResultValue());
            statement.setInt(4, conversion.getToUnit().getId());
            statement.setString(5, conversion.getLanguageCode());
            statement.setTimestamp(6, Timestamp.valueOf(conversion.getCreatedAt()));
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("The database did not return an id for the new conversion");
                }
                return conversion.withId(keys.getInt(1));
            }
        }
    }

    /** All conversions, newest first. */
    public List<Conversion> findAll() throws SQLException {
        List<Conversion> conversions = new ArrayList<>();
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_WITH_UNITS + "ORDER BY c.id DESC");
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                conversions.add(fromRow(rs));
            }
        }
        return conversions;
    }

    /**
     * Deletes every conversion. The units and languages stay.
     *
     * @return how many conversions were deleted
     */
    public int deleteAll() throws SQLException {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM conversion")) {
            return statement.executeUpdate();
        }
    }

    private static Conversion fromRow(ResultSet rs) throws SQLException {
        TemperatureUnit from = new TemperatureUnit(rs.getInt("from_id"), rs.getString("from_code"),
                rs.getString("from_name"), rs.getString("from_symbol"));
        TemperatureUnit to = new TemperatureUnit(rs.getInt("to_id"), rs.getString("to_code"),
                rs.getString("to_name"), rs.getString("to_symbol"));
        return new Conversion(rs.getInt("id"), rs.getDouble("input_value"), from,
                rs.getDouble("result_value"), to, rs.getString("language_code"),
                rs.getTimestamp("created_at").toLocalDateTime());
    }
}
