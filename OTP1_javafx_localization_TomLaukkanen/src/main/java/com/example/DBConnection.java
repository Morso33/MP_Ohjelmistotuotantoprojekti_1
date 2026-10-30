package com.example;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Opens connections to the database and creates the tables.
 * <p>
 * The connection settings come from the environment variables DB_URL, DB_USER and DB_PASSWORD.
 * Without them the app uses the local MariaDB set up by database/create_database.sql.
 */
public class DBConnection {

    public static final String DEFAULT_URL = "jdbc:mariadb://localhost:3306/temperature_converter";
    public static final String DEFAULT_USER = "temp_app";
    public static final String DEFAULT_PASSWORD = "temp_app_pw";

    /** Classpath location of the CREATE TABLE statements. */
    static final String SCHEMA_RESOURCE = "/db/schema.sql";

    private final String url;
    private final String user;
    private final String password;

    public DBConnection(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    /** Builds the connection settings from the process environment variables. */
    public static DBConnection fromEnvironment() {
        return fromEnvironment(System.getenv());
    }

    /**
     * Builds the connection settings from the given variables, using the defaults
     * for the ones that are missing or blank.
     */
    static DBConnection fromEnvironment(Map<String, String> env) {
        return new DBConnection(
                valueOrDefault(env.get("DB_URL"), DEFAULT_URL),
                valueOrDefault(env.get("DB_USER"), DEFAULT_USER),
                valueOrDefault(env.get("DB_PASSWORD"), DEFAULT_PASSWORD));
    }

    private static String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    /**
     * Opens a new connection. The caller closes it, preferably with try-with-resources.
     *
     * @throws SQLException if the database cannot be reached
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * Creates the tables if they are missing and adds the temperature units.
     *
     * @throws SQLException if a statement fails
     */
    public void initializeSchema() throws SQLException {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            for (String sql : readSchemaStatements()) {
                statement.execute(sql);
            }
        }
    }

    /** Reads schema.sql and splits it into single statements. */
    static List<String> readSchemaStatements() {
        String script;
        try (InputStream in = DBConnection.class.getResourceAsStream(SCHEMA_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Missing resource " + SCHEMA_RESOURCE);
            }
            script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        StringBuilder withoutComments = new StringBuilder();
        for (String line : script.split("\\R")) {
            if (!line.trim().startsWith("--")) {
                withoutComments.append(line).append('\n');
            }
        }

        List<String> statements = new ArrayList<>();
        for (String sql : withoutComments.toString().split(";")) {
            if (!sql.isBlank()) {
                statements.add(sql.trim());
            }
        }
        return statements;
    }

    public String getUrl() {
        return url;
    }

    public String getUser() {
        return user;
    }
}
