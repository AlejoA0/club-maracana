-- Agregar la columna motivo_cambio_estado a la tabla cancha
ALTER TABLE cancha ADD COLUMN motivo_cambio_estado VARCHAR(255);

-- Actualizar las canchas existentes con un mensaje predeterminado para las que no están disponibles
UPDATE cancha SET motivo_cambio_estado = 'Configuración inicial del sistema' 
WHERE estado != 'DISPONIBLE' AND (motivo_cambio_estado IS NULL OR motivo_cambio_estado = ''); 