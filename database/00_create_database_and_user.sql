-- =====================================================================
-- Script de preparación (se ejecuta UNA vez con el usuario root de MySQL)
-- Crea la base de datos y un usuario propio para la aplicación.
-- Las TABLAS no se crean aquí: las crea Flyway al arrancar la app.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS renta_herramientas
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

-- Base aparte para las pruebas de integración (mvn verify -Pintegracion): nunca tocan los datos de desarrollo
CREATE DATABASE IF NOT EXISTS renta_herramientas_test
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

-- Cambia 'cambia_esta_clave' por la misma clave que pongas en DB_PASSWORD del .env
CREATE USER IF NOT EXISTS 'renta_app'@'localhost' IDENTIFIED BY 'cambia_esta_clave';

-- CREATE USER IF NOT EXISTS no cambia la clave si el usuario ya existía,
-- por eso se fuerza con ALTER USER (evita el error de "Access denied").
ALTER USER 'renta_app'@'localhost' IDENTIFIED BY 'cambia_esta_clave';

GRANT ALL PRIVILEGES ON renta_herramientas.* TO 'renta_app'@'localhost';
GRANT ALL PRIVILEGES ON renta_herramientas_test.* TO 'renta_app'@'localhost';
FLUSH PRIVILEGES;
