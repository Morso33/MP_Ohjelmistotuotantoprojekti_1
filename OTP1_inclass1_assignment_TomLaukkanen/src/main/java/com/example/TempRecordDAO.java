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
 * Saves and reads conversions in the temp_record table. Reading joins the
 * temperature_unit table twice to get both units of a record.
 */
public class TempRecordDAO {

    private static final String SELECT_WITH_UNITS = """
            SELECT r.id, r.input_value, r.result_value, r.created_at,
                   f.id AS from_id, f.code AS from_code, f.name AS from_name, f.symbol AS from_symbol,
                   t.id AS to_id, t.code AS to_code, t.name AS to_name, t.symbol AS to_symbol
            FROM temp_record r
            JOIN temperature_unit f ON r.from_unit_id = f.id
            JOIN temperature_unit t ON r.to_unit_id = t.id
            """;

    private final DBConnection database;

    public TempRecordDAO(DBConnection database) {
        this.database = database;
    }

    /**
     * Inserts a record.
     *
     * @return the same record with the id the database gave it
     * @throws SQLException if the insert fails, e.g. a unit is not in temperature_unit
     */
    public TempRecord save(TempRecord record) throws SQLException {
        String sql = "INSERT INTO temp_record (input_value, from_unit_id, result_value, to_unit_id, created_at) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setDouble(1, record.getInputValue());
            statement.setInt(2, record.getFromUnit().getId());
            statement.setDouble(3, record.getResultValue());
            statement.setInt(4, record.getToUnit().getId());
            statement.setTimestamp(5, Timestamp.valueOf(record.getCreatedAt()));
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("The database did not return an id for the new record");
                }
                return record.withId(keys.getInt(1));
            }
        }
    }

    /** All records, newest first. */
    public List<TempRecord> findAll() throws SQLException {
        List<TempRecord> records = new ArrayList<>();
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_WITH_UNITS + "ORDER BY r.id DESC");
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                records.add(fromRow(rs));
            }
        }
        return records;
    }

    /**
     * Deletes every record. The units stay.
     *
     * @return how many records were deleted
     */
    public int deleteAll() throws SQLException {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM temp_record")) {
            return statement.executeUpdate();
        }
    }

    private static TempRecord fromRow(ResultSet rs) throws SQLException {
        TemperatureUnit from = new TemperatureUnit(rs.getInt("from_id"), rs.getString("from_code"),
                rs.getString("from_name"), rs.getString("from_symbol"));
        TemperatureUnit to = new TemperatureUnit(rs.getInt("to_id"), rs.getString("to_code"),
                rs.getString("to_name"), rs.getString("to_symbol"));
        return new TempRecord(rs.getInt("id"), rs.getDouble("input_value"), from,
                rs.getDouble("result_value"), to, rs.getTimestamp("created_at").toLocalDateTime());
    }
}
