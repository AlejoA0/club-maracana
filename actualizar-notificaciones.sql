-- Script para actualizar la tabla de notificaciones sin perder datos
USE maracana;

-- Ver la estructura actual de la tabla
DESCRIBE notificacion;

-- Ver los datos actuales
SELECT * FROM notificacion;

-- Asegurarse de que el tipo de datos de las columnas sea el correcto
-- Si ya está correcto (como parece), esto no hará nada
ALTER TABLE notificacion MODIFY COLUMN tipo ENUM('RESERVA_NUEVA', 'RESERVA_CANCELADA', 'USUARIO_NUEVO', 'SISTEMA_INFO') NOT NULL;
ALTER TABLE notificacion MODIFY COLUMN fecha_creacion DATETIME(6) NOT NULL;
ALTER TABLE notificacion MODIFY COLUMN referencia_tipo VARCHAR(255);

-- Crear índices si no existen
-- MySQL 8+ soporta CREATE INDEX IF NOT EXISTS
CREATE INDEX IF NOT EXISTS idx_notificacion_leida ON notificacion(leida);
CREATE INDEX IF NOT EXISTS idx_notificacion_fecha ON notificacion(fecha_creacion);

-- Verificar que todo esté bien
SELECT * FROM notificacion; 