-- Eliminar la tabla anterior si existe
DROP TABLE IF EXISTS notificacion;

-- Crear la tabla notificacion con la estructura correcta
CREATE TABLE notificacion (
    id INT AUTO_INCREMENT PRIMARY KEY,
    mensaje VARCHAR(255) NOT NULL,
    tipo VARCHAR(50) NOT NULL,
    fecha_creacion DATETIME NOT NULL,
    leida BOOLEAN NOT NULL DEFAULT FALSE,
    referencia_id INT,
    referencia_tipo VARCHAR(50)
);

-- Crear algunos índices para mejorar el rendimiento
CREATE INDEX idx_notificacion_leida ON notificacion(leida);
CREATE INDEX idx_notificacion_fecha ON notificacion(fecha_creacion); 