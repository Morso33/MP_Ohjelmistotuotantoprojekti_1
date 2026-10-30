package com.example;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

/**
 * Creates throwaway in-memory H2 databases that behave like MariaDB, so the
 * database tests run anywhere (also in Jenkins) without a MariaDB server.
 */
final class TestDatabase {

    private TestDatabase() {
    }

    /** A fresh empty database without tables. */
    static DBConnection empty() {
        String url = "jdbc:h2:mem:" + UUID.randomUUID()
                + ";MODE=MariaDB;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        return new DBConnection(url, "sa", "");
    }

    /** A fresh database with the tables and units from schema.sql. */
    static DBConnection withSchema() throws SQLException {
        DBConnection database = empty();
        database.initializeSchema();
        return database;
    }

    /** Runs one SQL statement, e.g. to break the database on purpose. */
    static void execute(DBConnection database, String sql) throws SQLException {
        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
