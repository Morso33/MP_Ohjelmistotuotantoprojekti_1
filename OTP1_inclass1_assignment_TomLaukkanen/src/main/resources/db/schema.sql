-- Tables of the Temperature Converter. DBConnection.initializeSchema() runs this file
-- every time the app starts, so every statement must be safe to run again.

-- The units a temperature can be given in
CREATE TABLE IF NOT EXISTS temperature_unit (
    id     INT AUTO_INCREMENT PRIMARY KEY,
    code   VARCHAR(1)  NOT NULL UNIQUE,
    name   VARCHAR(30) NOT NULL,
    symbol VARCHAR(5)  NOT NULL
);

-- Every conversion the user makes. Both units refer to temperature_unit (many-to-one).
CREATE TABLE IF NOT EXISTS temp_record (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    input_value  DOUBLE   NOT NULL,
    from_unit_id INT      NOT NULL,
    result_value DOUBLE   NOT NULL,
    to_unit_id   INT      NOT NULL,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_temp_record_from_unit FOREIGN KEY (from_unit_id) REFERENCES temperature_unit (id),
    CONSTRAINT fk_temp_record_to_unit FOREIGN KEY (to_unit_id) REFERENCES temperature_unit (id)
);

INSERT IGNORE INTO temperature_unit (code, name, symbol) VALUES
    ('C', 'Celsius', '°C'),
    ('F', 'Fahrenheit', '°F'),
    ('K', 'Kelvin', 'K');
