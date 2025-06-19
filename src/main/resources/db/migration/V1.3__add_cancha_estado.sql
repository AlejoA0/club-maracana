-- Agregar columna estado a la tabla cancha
ALTER TABLE cancha
ADD COLUMN estado VARCHAR(50) NOT NULL DEFAULT 'DISPONIBLE';

-- Actualizar todas las canchas existentes a estado DISPONIBLE
UPDATE cancha SET estado = 'DISPONIBLE'; 