-- Tables of the localized Temperature Converter. DBConnection.initializeSchema() runs this file
-- every time the app starts, so every statement must be safe to run again.

-- The units a temperature can be given in
CREATE TABLE IF NOT EXISTS temperature_unit (
    id     INT AUTO_INCREMENT PRIMARY KEY,
    code   VARCHAR(1)  NOT NULL UNIQUE,
    name   VARCHAR(30) NOT NULL,
    symbol VARCHAR(5)  NOT NULL
);

-- The languages of the user interface. The code is the Java locale, e.g. fi_FI.
CREATE TABLE IF NOT EXISTS ui_language (
    id   INT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(10) NOT NULL UNIQUE,
    name VARCHAR(30) NOT NULL
);

-- Every conversion the user saves. Both units refer to temperature_unit and the language
-- the window was in refers to ui_language (all many-to-one).
CREATE TABLE IF NOT EXISTS conversion (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    input_value  DOUBLE   NOT NULL,
    from_unit_id INT      NOT NULL,
    result_value DOUBLE   NOT NULL,
    to_unit_id   INT      NOT NULL,
    language_id  INT      NOT NULL,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_conversion_from_unit FOREIGN KEY (from_unit_id) REFERENCES temperature_unit (id),
    CONSTRAINT fk_conversion_to_unit FOREIGN KEY (to_unit_id) REFERENCES temperature_unit (id),
    CONSTRAINT fk_conversion_language FOREIGN KEY (language_id) REFERENCES ui_language (id)
);

INSERT IGNORE INTO temperature_unit (code, name, symbol) VALUES
    ('C', 'Celsius', '°C'),
    ('F', 'Fahrenheit', '°F'),
    ('K', 'Kelvin', 'K');

INSERT IGNORE INTO ui_language (code, name) VALUES
    ('en_US', 'English'),
    ('fi_FI', 'Suomi'),
    ('sv_SE', 'Svenska'),
    ('ja_JP', '日本語'),
    ('ar_SA', 'العربية');
