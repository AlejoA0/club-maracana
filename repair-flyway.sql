-- Script para reparar la migración problemática
USE maracana;

-- Borrar la entrada problemática de versión 1.4
DELETE FROM flyway_schema_history WHERE version='1.4';

-- Actualizar la notificacion table para asegurarnos de que tenga la estructura correcta
DROP TABLE IF EXISTS notificacion;

CREATE TABLE notificacion (
    id INT AUTO_INCREMENT PRIMARY KEY,
    mensaje VARCHAR(255) NOT NULL,
    tipo ENUM('RESERVA_NUEVA', 'RESERVA_CANCELADA', 'USUARIO_NUEVO', 'SISTEMA_INFO') NOT NULL,
    fecha_creacion DATETIME(6) NOT NULL,
    leida BOOLEAN NOT NULL DEFAULT FALSE,
    referencia_id INT,
    referencia_tipo VARCHAR(50)
);

CREATE INDEX idx_notificacion_leida ON notificacion(leida);
CREATE INDEX idx_notificacion_fecha ON notificacion(fecha_creacion); 