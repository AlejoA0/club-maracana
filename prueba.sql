/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;

-- ------------------------------------------------------
-- Paso 1: Eliminar objetos existentes (orden seguro)
-- ------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_crear_reserva;
DROP PROCEDURE IF EXISTS sp_listar_usuarios_paginados;
DROP PROCEDURE IF EXISTS sp_eliminar_reserva;
DROP TRIGGER IF EXISTS tr_valida_dt_unico;
DROP TRIGGER IF EXISTS tr_valida_puede_jugar;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS pago, reserva, solicitud_union, jugador, equipo, cancha, usuario_rol, usuario, rol;
SET FOREIGN_KEY_CHECKS = 1;

-- ------------------------------------------------------
-- Paso 2: Crear base de datos y seleccionarla
-- ------------------------------------------------------

CREATE DATABASE IF NOT EXISTS maracana 
    CHARACTER SET utf8mb4 
    COLLATE utf8mb4_unicode_ci;

USE maracana;

-- ------------------------------------------------------
-- Paso 3: Crear tablas (orden: sin dependencias primero)
-- ------------------------------------------------------
-- Tabla rol
CREATE TABLE rol (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre ENUM('ROLE_ADMIN', 'ROLE_JUGADOR', 'ROLE_DIRECTOR_TECNICO') NOT NULL UNIQUE
) ENGINE=InnoDB;

-- Tabla usuario
CREATE TABLE usuario (
    numero_documento VARCHAR(20) PRIMARY KEY,
    tipo_documento ENUM('CEDULA_CIUDADANIA','CEDULA_EXTRANJERIA','PERMISO_PROTECCION_TEMPORAL','REGISTRO_CIVIL','TARJETA_IDENTIDAD') NOT NULL,
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    fecha_nacimiento DATE,
    eps VARCHAR(100),
    telefono VARCHAR(20),
    puede_jugar BIT(1) DEFAULT 0,
    activo BIT(1) DEFAULT 1
) ENGINE=InnoDB;

-- Tabla cancha
CREATE TABLE cancha (
    id VARCHAR(10) PRIMARY KEY,
    codigo ENUM('CANCHA_1','CANCHA_10','CANCHA_11','CANCHA_12','CANCHA_13','CANCHA_14','CANCHA_16','CANCHA_17','CANCHA_18','CANCHA_2A','CANCHA_2B','CANCHA_3','CANCHA_4','CANCHA_5','CANCHA_6','CANCHA_7','CANCHA_8','CANCHA_9','CANCHA_BABY_1','CANCHA_BABY_2','FUTBOL_8_1','FUTBOL_8_2','FUTBOL_8_3','FUTBOL_8_4','TRANSICION_1','TRANSICION_2','TRANSICION_3') NOT NULL,
    tipo ENUM('FUTBOL_11','FUTBOL_8','INFANTIL') NOT NULL
) ENGINE=InnoDB;

-- Tabla equipo
CREATE TABLE equipo (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    categoria ENUM('FUTBOL_11','FUTBOL_8','INFANTIL') NOT NULL,
    logo MEDIUMBLOB,
    director_tecnico_id VARCHAR(20) NOT NULL,
    FOREIGN KEY (director_tecnico_id) REFERENCES usuario(numero_documento)
) ENGINE=InnoDB;

-- Tabla jugador
CREATE TABLE jugador (
    id_equipo INT,
    numero_documento VARCHAR(20) PRIMARY KEY,
    categoria ENUM('FUTBOL_11','FUTBOL_8','INFANTIL') NOT NULL,
    FOREIGN KEY (id_equipo) REFERENCES equipo(id) ON DELETE SET NULL,
    FOREIGN KEY (numero_documento) REFERENCES usuario(numero_documento)
) ENGINE=InnoDB;

CREATE TABLE solicitud_union (
    id INT AUTO_INCREMENT PRIMARY KEY,
    equipo_id INT NOT NULL,
    jugador_id VARCHAR(20) NOT NULL,
    estado ENUM('PENDIENTE','APROBADA','RECHAZADA') DEFAULT 'PENDIENTE',
    fecha_solicitud DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (equipo_id) REFERENCES equipo(id) ON DELETE CASCADE,
    FOREIGN KEY (jugador_id) REFERENCES usuario(numero_documento) ON DELETE CASCADE,
    UNIQUE KEY uk_solicitud (equipo_id, jugador_id) -- Evita solicitudes duplicadas
) ENGINE=InnoDB;

-- Tabla reserva
CREATE TABLE reserva (
    id INT AUTO_INCREMENT PRIMARY KEY,
    fecha_reserva DATE NOT NULL,
    hora_reserva ENUM('07:00:00','09:00:00','11:00:00','13:00:00','15:00:00') NOT NULL,
    estado_reserva ENUM('CANCELADA','CONFIRMADA','PENDIENTE') NOT NULL,
    cancha_id VARCHAR(10) NOT NULL,
    jugador_id VARCHAR(20) NOT NULL,
    FOREIGN KEY (cancha_id) REFERENCES cancha(id),
    FOREIGN KEY (jugador_id) REFERENCES usuario(numero_documento)
) ENGINE=InnoDB;

-- Tabla pago
CREATE TABLE pago (
    id INT AUTO_INCREMENT PRIMARY KEY,
    reserva_id INT NOT NULL UNIQUE,
    monto DECIMAL(10,2) NOT NULL,
    metodo_pago ENUM('EFECTIVO','TARJETA','TRANSFERENCIA') NOT NULL,
    fecha_pago DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (reserva_id) REFERENCES reserva(id)
) ENGINE=InnoDB;

-- Tabla usuario_rol
CREATE TABLE usuario_rol (
    usuario_id VARCHAR(20) NOT NULL,
    rol_id INT NOT NULL,
    PRIMARY KEY (usuario_id, rol_id),
    FOREIGN KEY (usuario_id) REFERENCES usuario(numero_documento),
    FOREIGN KEY (rol_id) REFERENCES rol(id)
) ENGINE=InnoDB;

-- ------------------------------------------------------
-- Paso 4: Triggers (sintaxis corregida)
-- ------------------------------------------------------
DELIMITER //

CREATE TRIGGER tr_valida_dt_unico
BEFORE INSERT ON equipo
FOR EACH ROW
BEGIN
    IF EXISTS(SELECT 1 FROM equipo WHERE director_tecnico_id = NEW.director_tecnico_id) THEN -- Nombre completo en una línea
        SIGNAL SQLSTATE '45000' 
        SET MESSAGE_TEXT = 'Un director técnico no puede tener más de un equipo';
    END IF;
END//

CREATE TRIGGER tr_valida_puede_jugar
BEFORE UPDATE ON usuario
FOR EACH ROW
BEGIN
    IF NEW.puede_jugar = 1 THEN
        IF NOT EXISTS (
            SELECT 1 FROM usuario_rol ur 
            JOIN rol r ON ur.rol_id = r.id 
            WHERE ur.usuario_id = NEW.numero_documento 
            AND r.nombre = 'ROLE_DIRECTOR_TECNICO'
        ) THEN
            SIGNAL SQLSTATE '45000' 
            SET MESSAGE_TEXT = 'Solo los directores técnicos pueden tener "puede_jugar = 1"';
        END IF;
    END IF;
END//

DELIMITER ;

-- ------------------------------------------------------
-- Paso 5: Procedimientos almacenados (sintaxis corregida)
-- ------------------------------------------------------
DELIMITER //

-- Procedimiento 1: Crear reserva
CREATE PROCEDURE sp_crear_reserva(
    IN p_jugador_id VARCHAR(20),
    IN p_cancha_id VARCHAR(10),
    IN p_fecha DATE,
    IN p_hora TIME,
    OUT p_reserva_id INT,
    OUT p_mensaje VARCHAR(255)
)
BEGIN
    DECLARE v_reservas_activas INT;
    DECLARE v_disponible BOOLEAN;

    SELECT COUNT(*) INTO v_reservas_activas 
    FROM reserva 
    WHERE jugador_id = p_jugador_id 
    AND estado_reserva = 'CONFIRMADA';

    SELECT NOT EXISTS (
        SELECT 1 FROM reserva 
        WHERE cancha_id = p_cancha_id 
        AND fecha_reserva = p_fecha 
        AND hora_reserva = p_hora 
        AND estado_reserva != 'CANCELADA'
    ) INTO v_disponible;

    IF v_reservas_activas >= 3 THEN
        SET p_mensaje = 'Error: Límite de 3 reservas activas alcanzado';
    ELSEIF NOT v_disponible THEN
        SET p_mensaje = 'Error: Cancha no disponible en el horario seleccionado';
    ELSE
        INSERT INTO reserva (fecha_reserva, hora_reserva, estado_reserva, cancha_id, jugador_id)
        VALUES (p_fecha, p_hora, 'CONFIRMADA', p_cancha_id, p_jugador_id);
        SET p_reserva_id = LAST_INSERT_ID();
        SET p_mensaje = 'Reserva creada exitosamente';
    END IF;
END//

-- Procedimiento 2: Listar usuarios paginados
CREATE PROCEDURE sp_listar_usuarios_paginados(
    IN p_pagina INT,
    IN p_tamano_pagina INT,
    IN p_filtro VARCHAR(100))
BEGIN
    DECLARE v_offset INT;
    SET v_offset = (p_pagina - 1) * p_tamano_pagina;

    SELECT * FROM usuario
    WHERE 
        nombres LIKE CONCAT('%', p_filtro, '%') OR
        apellidos LIKE CONCAT('%', p_filtro, '%') OR
        numero_documento LIKE CONCAT('%', p_filtro, '%')
    LIMIT v_offset, p_tamano_pagina;
END//

-- Procedimiento 3: Eliminar reserva
CREATE PROCEDURE sp_eliminar_reserva(
    IN p_reserva_id INT,
    OUT p_mensaje VARCHAR(255))
BEGIN
    DECLARE v_estado VARCHAR(20);

    SELECT estado_reserva INTO v_estado 
    FROM reserva 
    WHERE id = p_reserva_id;

    IF v_estado IS NULL THEN
        SET p_mensaje = 'Error: Reserva no encontrada';
    ELSEIF v_estado = 'CANCELADA' THEN
        SET p_mensaje = 'Error: La reserva ya está cancelada';
    ELSE
        UPDATE reserva 
        SET estado_reserva = 'CANCELADA' 
        WHERE id = p_reserva_id;
        SET p_mensaje = 'Reserva cancelada exitosamente';
    END IF;
END//

DELIMITER ;

/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;