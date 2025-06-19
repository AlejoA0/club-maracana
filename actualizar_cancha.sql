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