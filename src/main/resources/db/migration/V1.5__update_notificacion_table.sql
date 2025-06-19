-- Actualizar la estructura de la tabla notificacion si existe
ALTER TABLE notificacion MODIFY COLUMN tipo ENUM('RESERVA_NUEVA', 'RESERVA_CANCELADA', 'USUARIO_NUEVO', 'SISTEMA_INFO') NOT NULL;
ALTER TABLE notificacion MODIFY COLUMN fecha_creacion DATETIME(6) NOT NULL;

-- Asegurarse que los índices existen usando procedimiento seguro
DROP PROCEDURE IF EXISTS create_index_if_not_exists;

DELIMITER //
CREATE PROCEDURE create_index_if_not_exists()
BEGIN
    DECLARE index1_exists INT;
    DECLARE index2_exists INT;
    
    -- Verificar si el índice idx_notificacion_leida ya existe
    SELECT COUNT(*) INTO index1_exists
    FROM information_schema.statistics 
    WHERE table_schema = DATABASE()
    AND table_name = 'notificacion' 
    AND index_name = 'idx_notificacion_leida';
    
    -- Verificar si el índice idx_notificacion_fecha ya existe
    SELECT COUNT(*) INTO index2_exists
    FROM information_schema.statistics 
    WHERE table_schema = DATABASE()
    AND table_name = 'notificacion' 
    AND index_name = 'idx_notificacion_fecha';
    
    -- Crear índice idx_notificacion_leida si no existe
    IF index1_exists = 0 THEN
        CREATE INDEX idx_notificacion_leida ON notificacion(leida);
    END IF;
    
    -- Crear índice idx_notificacion_fecha si no existe
    IF index2_exists = 0 THEN
        CREATE INDEX idx_notificacion_fecha ON notificacion(fecha_creacion);
    END IF;
END //
DELIMITER ;

-- Ejecutar el procedimiento
CALL create_index_if_not_exists();

-- Eliminar el procedimiento temporal
DROP PROCEDURE IF EXISTS create_index_if_not_exists; 