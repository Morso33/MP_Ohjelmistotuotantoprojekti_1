package com.example;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DBConnectionTest {

    @Test
    @DisplayName("Without environment variables the local MariaDB is used")
    void defaultsWhenVariablesMissing() {
        DBConnection database = DBConnection.fromEnvironment(Map.of());
        assertEquals(DBConnection.DEFAULT_URL, database.getUrl());
        assertEquals(DBConnection.DEFAULT_USER, database.getUser());
    }

    @Test
    @DisplayName("DB_URL and DB_USER override the defaults")
    void variablesOverrideDefaults() {
        DBConnection database = DBConnection.fromEnvironment(Map.of(
                "DB_URL", "jdbc:mariadb://host.docker.internal:3306/temperature_converter",
                "DB_USER", "someone",
                "DB_PASSWORD", "secret"));
        assertEquals("jdbc:mariadb://host.docker.internal:3306/temperature_converter", database.getUrl());
        assertEquals("someone", database.getUser());
    }

    @Test
    @DisplayName("Blank variables count as missing")
    void blankVariablesUseDefaults() {
        DBConnection database = DBConnection.fromEnvironment(Map.of("DB_URL", "  ", "DB_USER", ""));
        assertEquals(DBConnection.DEFAULT_URL, database.getUrl());
        assertEquals(DBConnection.DEFAULT_USER, database.getUser());
    }

    @Test
    @DisplayName("The real process environment can be read")
    void readsProcessEnvironment() {
        DBConnection database = DBConnection.fromEnvironment();
        assertNotNull(database.getUrl());
        assertNotNull(database.getUser());
    }

    @Test
    @DisplayName("getConnection opens a working connection")
    void opensConnection() throws SQLException {
        DBConnection database = TestDatabase.empty();
        try (Connection connection = database.getConnection()) {
            assertTrue(connection.isValid(2));
        }
    }

    @Test
    @DisplayName("getConnection fails with SQLException when the database cannot be reached")
    void connectionFailure() {
        DBConnection database = new DBConnection("jdbc:nosuchdriver://localhost/none", "user", "pw");
        assertThrows(SQLException.class, database::getConnection);
        assertThrows(SQLException.class, database::initializeSchema);
    }

    @Test
    @DisplayName("schema.sql is split into its three statements without the comments")
    void readsSchemaStatements() {
        List<String> statements = DBConnection.readSchemaStatements();
        assertEquals(3, statements.size());
        assertTrue(statements.get(0).startsWith("CREATE TABLE IF NOT EXISTS temperature_unit"));
        assertTrue(statements.get(1).startsWith("CREATE TABLE IF NOT EXISTS temp_record"));
        assertTrue(statements.get(2).startsWith("INSERT IGNORE INTO temperature_unit"));
        statements.forEach(sql -> assertFalse(sql.contains("--"), sql));
    }

    @Test
    @DisplayName("initializeSchema creates both tables and the three units")
    void createsTablesAndUnits() throws SQLException {
        DBConnection database = TestDatabase.withSchema();
        assertEquals(3, count(database, "temperature_unit"));
        assertEquals(0, count(database, "temp_record"));
    }

    @Test
    @DisplayName("initializeSchema can run again without duplicating units or losing records")
    void initializeSchemaTwice() throws SQLException {
        DBConnection database = TestDatabase.withSchema();
        TestDatabase.execute(database,
                "INSERT INTO temp_record (input_value, from_unit_id, result_value, to_unit_id) VALUES (0, 1, 32, 2)");

        database.initializeSchema();

        assertEquals(3, count(database, "temperature_unit"));
        assertEquals(1, count(database, "temp_record"));
    }

    private static int count(DBConnection database, String table) throws SQLException {
        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
