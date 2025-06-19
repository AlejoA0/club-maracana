-- Creación de la tabla de notificaciones si no existe
CREATE TABLE IF NOT EXISTS notificacion (
    id INT AUTO_INCREMENT PRIMARY KEY,
    titulo VARCHAR(255) NOT NULL,
    contenido TEXT NOT NULL,
    fecha_creacion DATETIME NOT NULL,
    leida BOOLEAN DEFAULT FALSE,
    tipo VARCHAR(50) NOT NULL
);

-- No hacemos nada más ya que la tabla podría ya existir
-- debido a la configuración JPA con hibernate.ddl-auto=update 