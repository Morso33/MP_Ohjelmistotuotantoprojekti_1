-- Creates the database and the user the Temperature Converter connects with.
-- Run once as root, for example:
--   "C:\Program Files\MariaDB 11.7\bin\mariadb.exe" -u root -p < database\create_database.sql
-- The app creates its tables itself on start (src/main/resources/db/schema.sql).

CREATE DATABASE IF NOT EXISTS temperature_converter
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 'localhost' for the app running on Windows, '%' for the app running in Docker,
-- which connects through host.docker.internal
CREATE USER IF NOT EXISTS 'temp_app'@'localhost' IDENTIFIED BY 'temp_app_pw';
CREATE USER IF NOT EXISTS 'temp_app'@'%' IDENTIFIED BY 'temp_app_pw';

GRANT ALL PRIVILEGES ON temperature_converter.* TO 'temp_app'@'localhost';
GRANT ALL PRIVILEGES ON temperature_converter.* TO 'temp_app'@'%';
FLUSH PRIVILEGES;
