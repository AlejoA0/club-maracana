-- IMPORTANTE: Este script debe ejecutarse manualmente en la base de datos
-- Este script desactiva Flyway haciendo un respaldo de la tabla flyway_schema_history
-- y luego la elimina, lo que hace que Flyway piense que nunca se ha ejecutado

-- 1. Verificar si la tabla de historial de Flyway existe
SET @exists := (SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                AND table_name = 'flyway_schema_history');

-- 2. Solo ejecutar el respaldo si la tabla existe
SET @statement = IF(@exists > 0, 
    'CREATE TABLE IF NOT EXISTS flyway_schema_history_backup AS SELECT * FROM flyway_schema_history', 
    'SELECT "Tabla flyway_schema_history no encontrada, no se requiere respaldo"');

PREPARE stmt FROM @statement;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. Eliminar la tabla flyway_schema_history si existe
SET @dropstatement = IF(@exists > 0, 
    'DROP TABLE flyway_schema_history', 
    'SELECT "No hay tabla flyway_schema_history para eliminar"');

PREPARE dropcmd FROM @dropstatement;
EXECUTE dropcmd;
DEALLOCATE PREPARE dropcmd;

-- 4. Mostrar mensaje de éxito
SELECT 'Flyway ha sido desactivado correctamente. El historial de migraciones se ha respaldado en flyway_schema_history_backup' AS 'Mensaje'; 