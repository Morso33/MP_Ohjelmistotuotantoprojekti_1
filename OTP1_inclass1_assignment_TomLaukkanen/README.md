# Temperature Converter (JavaFX + MariaDB)

A JavaFX app that converts temperatures between Celsius, Fahrenheit and Kelvin.
Every conversion is saved to a MariaDB database and listed in the window.

## Database

Two related tables (created by the app on start from `src/main/resources/db/schema.sql`):

| Table              | Columns                                                                                         |
|--------------------|-------------------------------------------------------------------------------------------------|
| `temperature_unit` | `id` (PK), `code` (C/F/K, unique), `name`, `symbol`                                             |
| `temp_record`      | `id` (PK), `input_value`, `from_unit_id` (FK), `result_value`, `to_unit_id` (FK), `created_at` |

`temp_record.from_unit_id` and `temp_record.to_unit_id` both reference `temperature_unit.id`
(one unit, many records).

One-time setup as MariaDB root (creates the database and the `temp_app` user):

```
"C:\Program Files\MariaDB 11.7\bin\mariadb.exe" -u root -p < database\create_database.sql
```

The connection can be changed with the environment variables `DB_URL`, `DB_USER` and `DB_PASSWORD`
(defaults: `jdbc:mariadb://localhost:3306/temperature_converter`, `temp_app`).

## Classes

| Class                | Purpose                                                       |
|----------------------|---------------------------------------------------------------|
| `Main`               | JavaFX window                                                 |
| `Launcher`           | Main class of the runnable jar, starts `Main`                 |
| `TempCalculator`     | Conversion formulas, input parsing, absolute zero check       |
| `DBConnection`       | Opens connections, creates the tables                         |
| `TemperatureUnit`    | One row of `temperature_unit`                                 |
| `TempRecord`         | One row of `temp_record`                                      |
| `TemperatureUnitDAO` | Reads units                                                   |
| `TempRecordDAO`      | Saves, lists (JOIN with both units) and deletes conversions   |

## Build, test and coverage

```
mvnw.cmd clean package
```

Runs the unit tests (the database tests use an in-memory H2 database, so no MariaDB is needed)
and writes the JaCoCo report to `target/site/jacoco/index.html`.

## Run locally

```
mvnw.cmd javafx:run
```

or `java -jar target\temperature-converter.jar` after packaging.

## Run with Docker

1. Start **Xming** with *XLaunch*: Multiple windows, display number 0, Start no client,
   tick **No Access Control**.
2. Build and run:

```
docker build -t tomlaukkanen/temperature-converter-javafx .
docker run --rm tomlaukkanen/temperature-converter-javafx
```

The image sets `DISPLAY=host.docker.internal:0.0` (Xming on Windows) and
`DB_URL=jdbc:mariadb://host.docker.internal:3306/temperature_converter` (MariaDB on Windows).

Or pull the published image: `docker pull tomlaukkanen/temperature-converter-javafx`

## Jenkins

The `Jenkinsfile` builds, tests, publishes the test and JaCoCo reports, builds the Docker image and
pushes it to Docker Hub as `tomlaukkanen/temperature-converter-javafx` (`latest` and the build number).
