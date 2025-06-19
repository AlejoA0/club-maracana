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