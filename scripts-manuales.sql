-- SCRIPTS MANUALES PARA LA BASE DE DATOS
-- Este archivo contiene todas las migraciones en orden para ejecutarlas manualmente
-- después de desactivar Flyway

-- ========================================================================
-- V1.0__baseline.sql
-- ========================================================================
-- Este archivo está vacío intencionalmente.
-- Se usa para marcar el baseline de la migración de Flyway.

-- ========================================================================
-- V1.1__set_initial_schema.sql
-- ========================================================================
-- Este archivo está diseñado para ser ejecutado después de que la
-- estructura inicial de tablas ya existe en la base de datos.

-- ========================================================================
-- V1.2__add_notificaciones.sql
-- ========================================================================
-- Crear la tabla de notificaciones

CREATE TABLE IF NOT EXISTS notificacion (
    id INT AUTO_INCREMENT PRIMARY KEY,
    mensaje VARCHAR(255) NOT NULL,
    tipo ENUM('RESERVA_NUEVA', 'RESERVA_CANCELADA', 'USUARIO_NUEVO', 'SISTEMA_INFO') NOT NULL,
    fecha_creacion DATETIME(6) NOT NULL,
    leida BOOLEAN NOT NULL DEFAULT 0,
    referencia_id INT NULL,
    referencia_tipo VARCHAR(255) NULL
);

-- Crear índices para búsquedas rápidas
CREATE INDEX idx_notificacion_leida ON notificacion(leida);
CREATE INDEX idx_notificacion_fecha ON notificacion(fecha_creacion);

-- ========================================================================
-- V1.3__add_cancha_estado.sql
-- ========================================================================
-- Agregar columna estado a la tabla cancha

ALTER TABLE cancha
ADD COLUMN estado VARCHAR(50) NOT NULL DEFAULT 'DISPONIBLE';

-- ========================================================================
-- V1.4__fix_notificacion_table.sql
-- ========================================================================
-- Actualizar la tabla de notificaciones para arreglar los errores de no nulidad

-- Primero verificar si la tabla existe
SET @existe_tabla = (SELECT COUNT(*) FROM information_schema.tables 
                    WHERE table_schema = DATABASE() AND table_name = 'notificacion');

-- Solo ejecutar si la tabla existe
SET @alter_statement = IF(@existe_tabla > 0, 
                       'ALTER TABLE notificacion MODIFY COLUMN referencia_id INT NULL', 
                       'SELECT "Tabla notificacion no encontrada"');
PREPARE stmt FROM @alter_statement;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Crear índices para mejorar el rendimiento si la tabla existe
SET @index_statement = IF(@existe_tabla > 0, 
                      'CREATE INDEX IF NOT EXISTS idx_notificacion_leida ON notificacion(leida)', 
                      'SELECT "No se crearon índices, tabla notificacion no existe"');
PREPARE stmt FROM @index_statement;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_fecha = IF(@existe_tabla > 0, 
                  'CREATE INDEX IF NOT EXISTS idx_notificacion_fecha ON notificacion(fecha_creacion)', 
                  'SELECT "No se crearon índices, tabla notificacion no existe"');
PREPARE stmt FROM @index_fecha;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ========================================================================
-- V1.5__update_notificacion_table.sql
-- ========================================================================
-- Añadir índices adicionales para la tabla de notificaciones

-- Verificar primero si la tabla existe
SET @existe_tabla = (SELECT COUNT(*) FROM information_schema.tables 
                    WHERE table_schema = DATABASE() AND table_name = 'notificacion');

-- Crear índice solo si la tabla existe
SET @index_statement = IF(@existe_tabla > 0, 
                      'ALTER TABLE notificacion ADD INDEX idx_notificacion_tipo (tipo)', 
                      'SELECT "No se puede crear índice, tabla no existe"');
PREPARE stmt FROM @index_statement;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ========================================================================
-- V1.6__add_destinatario_to_notificacion.sql
-- ========================================================================
-- Añadir nuevos campos a la tabla notificacion
ALTER TABLE notificacion ADD COLUMN tipo_destinatario ENUM('ADMIN', 'USUARIO') NOT NULL DEFAULT 'ADMIN';
ALTER TABLE notificacion ADD COLUMN destinatario_id VARCHAR(100) NULL;

-- Actualizar las notificaciones existentes para que sean para administradores
UPDATE notificacion SET tipo_destinatario = 'ADMIN' WHERE tipo_destinatario IS NULL;

-- Crear índice para mejorar las búsquedas
CREATE INDEX idx_notificacion_tipo_destinatario ON notificacion(tipo_destinatario);

-- Añadir índice para mejorar el rendimiento de búsquedas por destinatario
CREATE INDEX idx_notificacion_destinatario_id ON notificacion(destinatario_id);

-- ========================================================================
-- V1.7__update_notificacion_tipos.sql
-- ========================================================================
-- Actualizar la tabla de reserva para admitir el estado VENCIDA
ALTER TABLE reserva 
MODIFY COLUMN estado_reserva ENUM('CANCELADA', 'CONFIRMADA', 'PENDIENTE', 'VENCIDA') NOT NULL;

-- Actualizar la tabla de notificacion para admitir los nuevos tipos
ALTER TABLE notificacion
MODIFY COLUMN tipo ENUM(
    'RESERVA_NUEVA', 
    'RESERVA_CANCELADA', 
    'USUARIO_NUEVO', 
    'SISTEMA_INFO', 
    'CANCHA_ESTADO_CAMBIO', 
    'RESERVA_CANCELADA_ADMIN'
) NOT NULL;

-- Convertir notificaciones existentes de cancelación por admin al nuevo tipo
UPDATE notificacion 
SET tipo = 'RESERVA_CANCELADA_ADMIN' 
WHERE tipo = 'RESERVA_CANCELADA' 
AND tipo_destinatario = 'USUARIO';

-- Marcar las reservas pasadas como VENCIDAS
UPDATE reserva 
SET estado_reserva = 'VENCIDA'
WHERE fecha_reserva < CURDATE() 
AND estado_reserva = 'CONFIRMADA';

-- Agregar columna motivo_bloqueo a la tabla usuario
ALTER TABLE usuario ADD COLUMN motivo_bloqueo VARCHAR(255) DEFAULT NULL;