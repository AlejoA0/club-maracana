-- Verificar si la columna estado existe en la tabla cancha
SELECT COLUMN_NAME 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'maracana' 
  AND TABLE_NAME = 'cancha' 
  AND COLUMN_NAME = 'estado';
  
-- Si la columna no existe, agregarla
ALTER TABLE cancha
ADD COLUMN IF NOT EXISTS estado VARCHAR(50) NOT NULL DEFAULT 'DISPONIBLE';

-- Actualizar todas las canchas existentes a estado DISPONIBLE
UPDATE cancha SET estado = 'DISPONIBLE' WHERE estado IS NULL OR estado = '';

-- Verificar la estructura de la tabla cancha
DESCRIBE cancha;

-- Agregar la columna motivo_cambio_estado a la tabla cancha
ALTER TABLE cancha ADD COLUMN motivo_cambio_estado VARCHAR(255);

-- Actualizar las canchas existentes con un mensaje predeterminado para las que no están disponibles
UPDATE cancha SET motivo_cambio_estado = 'Configuración inicial del sistema' 
WHERE estado != 'DISPONIBLE' AND (motivo_cambio_estado IS NULL OR motivo_cambio_estado = '');

-- Verificar que la columna se ha agregado correctamente
SELECT id, codigo, estado, motivo_cambio_estado FROM cancha; 