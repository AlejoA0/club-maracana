-- Añadir nuevos campos a la tabla notificacion
ALTER TABLE notificacion ADD COLUMN tipo_destinatario ENUM('ADMIN', 'USUARIO') NOT NULL DEFAULT 'ADMIN';
ALTER TABLE notificacion ADD COLUMN destinatario_id VARCHAR(100) NULL;

-- Actualizar las notificaciones existentes para que sean para administradores
UPDATE notificacion SET tipo_destinatario = 'ADMIN' WHERE tipo_destinatario IS NULL;

-- Crear índice para mejorar las búsquedas
CREATE INDEX idx_notificacion_tipo_destinatario ON notificacion(tipo_destinatario);

-- Añadir índice para mejorar el rendimiento de búsquedas por destinatario
CREATE INDEX idx_notificacion_destinatario_id ON notificacion(destinatario_id); 