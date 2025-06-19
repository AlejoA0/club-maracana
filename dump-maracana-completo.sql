-- MySQL dump 10.13  Distrib 8.0.42, for Win64 (x86_64)
--
-- Host: localhost    Database: maracana
-- ------------------------------------------------------
-- Server version	9.3.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `cancha`
--

DROP TABLE IF EXISTS `cancha`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cancha` (
  `id` varchar(10) COLLATE utf8mb4_general_ci NOT NULL,
  `codigo` enum('CANCHA_1','CANCHA_10','CANCHA_11','CANCHA_12','CANCHA_13','CANCHA_14','CANCHA_16','CANCHA_17','CANCHA_18','CANCHA_2A','CANCHA_2B','CANCHA_3','CANCHA_4','CANCHA_5','CANCHA_6','CANCHA_7','CANCHA_8','CANCHA_9','CANCHA_BABY_1','CANCHA_BABY_2','FUTBOL_8_1','FUTBOL_8_2','FUTBOL_8_3','FUTBOL_8_4','TRANSICION_1','TRANSICION_2','TRANSICION_3') COLLATE utf8mb4_general_ci NOT NULL,
  `tipo` enum('FUTBOL_11','FUTBOL_8','INFANTIL') COLLATE utf8mb4_general_ci NOT NULL,
  `estado` varchar(50) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'DISPONIBLE',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cancha`
--

LOCK TABLES `cancha` WRITE;
/*!40000 ALTER TABLE `cancha` DISABLE KEYS */;
INSERT INTO `cancha` VALUES ('C1','CANCHA_1','FUTBOL_11','DISPONIBLE'),('C10','CANCHA_10','FUTBOL_11','DISPONIBLE'),('C11','CANCHA_11','FUTBOL_11','EN_MANTENIMIENTO'),('C12','CANCHA_12','FUTBOL_11','DISPONIBLE'),('C13','CANCHA_13','FUTBOL_11','FUERA_DE_SERVICIO'),('C14','CANCHA_14','FUTBOL_11','DISPONIBLE'),('C16','CANCHA_16','FUTBOL_11','DISPONIBLE'),('C17','CANCHA_17','FUTBOL_11','DISPONIBLE'),('C18','CANCHA_18','FUTBOL_11','DISPONIBLE'),('C2A','CANCHA_2A','INFANTIL','DISPONIBLE'),('C2B','CANCHA_2B','INFANTIL','DISPONIBLE'),('C3','CANCHA_3','FUTBOL_11','DISPONIBLE'),('C4','CANCHA_4','FUTBOL_11','DISPONIBLE'),('C5','CANCHA_5','FUTBOL_11','DISPONIBLE'),('C6','CANCHA_6','FUTBOL_11','DISPONIBLE'),('C7','CANCHA_7','FUTBOL_11','DISPONIBLE'),('C8','CANCHA_8','FUTBOL_11','DISPONIBLE'),('C9','CANCHA_9','FUTBOL_11','DISPONIBLE'),('CAN_29','CANCHA_11','FUTBOL_11','DISPONIBLE'),('CB1','CANCHA_BABY_1','INFANTIL','DISPONIBLE'),('CB2','CANCHA_BABY_2','INFANTIL','DISPONIBLE'),('F8_1','FUTBOL_8_1','FUTBOL_8','DISPONIBLE'),('F8_2','FUTBOL_8_2','FUTBOL_8','DISPONIBLE'),('F8_3','FUTBOL_8_3','FUTBOL_8','DISPONIBLE'),('F8_4','FUTBOL_8_4','FUTBOL_8','DISPONIBLE'),('TR1','TRANSICION_1','INFANTIL','DISPONIBLE'),('TR2','TRANSICION_2','INFANTIL','DISPONIBLE'),('TR3','TRANSICION_3','INFANTIL','DISPONIBLE');
/*!40000 ALTER TABLE `cancha` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `equipo`
--

DROP TABLE IF EXISTS `equipo`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `equipo` (
  `id` int NOT NULL AUTO_INCREMENT,
  `nombre` varchar(100) COLLATE utf8mb4_general_ci NOT NULL,
  `categoria` enum('FUTBOL_11','FUTBOL_8','INFANTIL') COLLATE utf8mb4_general_ci NOT NULL,
  `logo` mediumblob,
  `director_tecnico_id` varchar(20) COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `nombre` (`nombre`),
  KEY `director_tecnico_id` (`director_tecnico_id`),
  CONSTRAINT `equipo_ibfk_1` FOREIGN KEY (`director_tecnico_id`) REFERENCES `usuario` (`numero_documento`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `equipo`
--

LOCK TABLES `equipo` WRITE;
/*!40000 ALTER TABLE `equipo` DISABLE KEYS */;
/*!40000 ALTER TABLE `equipo` ENABLE KEYS */;
UNLOCK TABLES;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `tr_valida_dt_unico` BEFORE INSERT ON `equipo` FOR EACH ROW BEGIN
    IF EXISTS(SELECT 1 FROM equipo WHERE director_tecnico_id = NEW.director_tecnico_id) THEN -- Nombre completo en una línea
        SIGNAL SQLSTATE '45000' 
        SET MESSAGE_TEXT = 'Un director técnico no puede tener más de un equipo';
    END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `flyway_schema_history_backup`
--

DROP TABLE IF EXISTS `flyway_schema_history_backup`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flyway_schema_history_backup` (
  `installed_rank` int NOT NULL,
  `version` varchar(50) DEFAULT NULL,
  `description` varchar(200) NOT NULL,
  `type` varchar(20) NOT NULL,
  `script` varchar(1000) NOT NULL,
  `checksum` int DEFAULT NULL,
  `installed_by` varchar(100) NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int NOT NULL,
  `success` tinyint(1) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flyway_schema_history_backup`
--

LOCK TABLES `flyway_schema_history_backup` WRITE;
/*!40000 ALTER TABLE `flyway_schema_history_backup` DISABLE KEYS */;
INSERT INTO `flyway_schema_history_backup` VALUES (1,'0','<< Flyway Baseline >>','BASELINE','<< Flyway Baseline >>',NULL,'root','2025-06-16 20:15:40',0,1),(2,'1.0','baseline','SQL','V1.0__baseline.sql',-1462144239,'root','2025-06-16 20:15:40',10,1),(3,'1.1','set initial schema','SQL','V1.1__set_initial_schema.sql',-1480258100,'root','2025-06-16 20:15:40',10,1),(4,'1.2','add notificaciones','SQL','V1.2__add_notificaciones.sql',1556556053,'root','2025-06-16 20:15:41',788,1),(5,'1.3','add cancha estado','SQL','V1.3__add_cancha_estado.sql',602591179,'root','2025-06-16 20:15:41',85,1),(6,'1.4','fix notificacion table','SQL','V1.4__fix_notificacion_table.sql',-1833633568,'root','2025-06-16 20:35:24',116,1),(7,'1.5','update notificacion table','SQL','V1.5__update_notificacion_table.sql',-751209461,'root','2025-06-18 02:47:32',944,0);
/*!40000 ALTER TABLE `flyway_schema_history_backup` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `jugador`
--

DROP TABLE IF EXISTS `jugador`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `jugador` (
  `id_equipo` int DEFAULT NULL,
  `numero_documento` varchar(20) COLLATE utf8mb4_general_ci NOT NULL,
  `categoria` enum('FUTBOL_11','FUTBOL_8','INFANTIL') COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`numero_documento`),
  KEY `id_equipo` (`id_equipo`),
  CONSTRAINT `jugador_ibfk_1` FOREIGN KEY (`id_equipo`) REFERENCES `equipo` (`id`) ON DELETE SET NULL,
  CONSTRAINT `jugador_ibfk_2` FOREIGN KEY (`numero_documento`) REFERENCES `usuario` (`numero_documento`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `jugador`
--

LOCK TABLES `jugador` WRITE;
/*!40000 ALTER TABLE `jugador` DISABLE KEYS */;
/*!40000 ALTER TABLE `jugador` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `notificacion`
--

DROP TABLE IF EXISTS `notificacion`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notificacion` (
  `id` int NOT NULL AUTO_INCREMENT,
  `mensaje` varchar(255) NOT NULL,
  `tipo` enum('RESERVA_NUEVA','RESERVA_CANCELADA','USUARIO_NUEVO','SISTEMA_INFO','CANCHA_ESTADO_CAMBIO','RESERVA_CANCELADA_ADMIN') NOT NULL,
  `fecha_creacion` datetime(6) NOT NULL,
  `leida` tinyint(1) NOT NULL DEFAULT '0',
  `referencia_id` int DEFAULT NULL,
  `referencia_tipo` varchar(255) DEFAULT NULL,
  `tipo_destinatario` enum('ADMIN','USUARIO') NOT NULL DEFAULT 'ADMIN',
  `destinatario_id` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_notificacion_leida` (`leida`),
  KEY `idx_notificacion_fecha` (`fecha_creacion`),
  KEY `idx_notificacion_tipo_destinatario` (`tipo_destinatario`),
  KEY `idx_notificacion_destinatario_id` (`destinatario_id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notificacion`
--

LOCK TABLES `notificacion` WRITE;
/*!40000 ALTER TABLE `notificacion` DISABLE KEYS */;
INSERT INTO `notificacion` VALUES (1,'Reserva cancelada por Alejandro Poveda Acero para la cancha C1 el dia 2025-04-12 a las 07:00:00','RESERVA_CANCELADA','2025-06-17 12:00:24.000000',0,4,'RESERVA','ADMIN',NULL),(2,'Nueva reserva creada por Alejandro Poveda Acero para la cancha C1 el dia 2025-06-20 a las 09:00:00','RESERVA_NUEVA','2025-06-17 14:34:54.000000',0,15,'RESERVA','ADMIN',NULL),(3,'Un administrador ha cancelado tu reserva para la cancha C1 el día 2025-06-19 a las 09:00:00','RESERVA_CANCELADA','2025-06-18 05:35:06.113896',0,13,'RESERVA','ADMIN',NULL),(4,'Reserva cancelada por Alejandro Poveda Acero para la cancha C1 el dia 2025-06-19 a las 09:00:00','RESERVA_CANCELADA','2025-06-18 05:35:06.294783',0,13,'RESERVA','ADMIN',NULL),(5,'Un administrador ha cancelado tu reserva para la cancha C1 el día 2025-06-20 a las 07:00:00','RESERVA_CANCELADA','2025-06-18 05:35:12.777069',0,14,'RESERVA','ADMIN',NULL),(6,'Reserva cancelada por Alejandro Poveda Acero para la cancha C1 el dia 2025-06-20 a las 07:00:00','RESERVA_CANCELADA','2025-06-18 05:35:12.864015',0,14,'RESERVA','ADMIN',NULL);
/*!40000 ALTER TABLE `notificacion` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pago`
--

DROP TABLE IF EXISTS `pago`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pago` (
  `id` int NOT NULL AUTO_INCREMENT,
  `reserva_id` int NOT NULL,
  `monto` decimal(10,2) NOT NULL,
  `metodo_pago` enum('EFECTIVO','TARJETA','TRANSFERENCIA') COLLATE utf8mb4_general_ci NOT NULL,
  `fecha_pago` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `reserva_id` (`reserva_id`),
  CONSTRAINT `pago_ibfk_1` FOREIGN KEY (`reserva_id`) REFERENCES `reserva` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pago`
--

LOCK TABLES `pago` WRITE;
/*!40000 ALTER TABLE `pago` DISABLE KEYS */;
INSERT INTO `pago` VALUES (1,13,160000.00,'TARJETA','2025-06-16 12:43:34'),(2,14,160000.00,'TARJETA','2025-06-16 12:49:24'),(3,15,160000.00,'EFECTIVO','2025-06-17 14:34:54');
/*!40000 ALTER TABLE `pago` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `reserva`
--

DROP TABLE IF EXISTS `reserva`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reserva` (
  `id` int NOT NULL AUTO_INCREMENT,
  `fecha_reserva` date NOT NULL,
  `hora_reserva` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `estado_reserva` enum('CANCELADA','CONFIRMADA','PENDIENTE','VENCIDA') COLLATE utf8mb4_general_ci NOT NULL,
  `cancha_id` varchar(10) COLLATE utf8mb4_general_ci NOT NULL,
  `jugador_id` varchar(20) COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `cancha_id` (`cancha_id`),
  KEY `jugador_id` (`jugador_id`),
  CONSTRAINT `reserva_ibfk_1` FOREIGN KEY (`cancha_id`) REFERENCES `cancha` (`id`),
  CONSTRAINT `reserva_ibfk_2` FOREIGN KEY (`jugador_id`) REFERENCES `usuario` (`numero_documento`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reserva`
--

LOCK TABLES `reserva` WRITE;
/*!40000 ALTER TABLE `reserva` DISABLE KEYS */;
INSERT INTO `reserva` VALUES (1,'2025-04-12','07:00:00','CANCELADA','C1','1016594699'),(2,'2025-04-12','07:00:00','CANCELADA','C10','1016594699'),(3,'2025-04-12','07:00:00','CANCELADA','C11','1016594699'),(4,'2025-04-12','07:00:00','CANCELADA','C1','1016594699'),(5,'2025-04-12','15:00:00','VENCIDA','F8_2','5696448'),(11,'2025-04-12','07:00:00','CANCELADA','C9','101659469'),(12,'2025-04-25','09:00:00','VENCIDA','C16','101659469'),(13,'2025-06-19','09:00:00','CANCELADA','C1','1016594699'),(14,'2025-06-20','07:00:00','CANCELADA','C1','1016594699'),(15,'2025-06-20','09:00:00','CONFIRMADA','C1','1016594699');
/*!40000 ALTER TABLE `reserva` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rol`
--

DROP TABLE IF EXISTS `rol`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rol` (
  `id` int NOT NULL AUTO_INCREMENT,
  `nombre` enum('ROLE_ADMIN','ROLE_JUGADOR','ROLE_DIRECTOR_TECNICO') COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `nombre` (`nombre`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rol`
--

LOCK TABLES `rol` WRITE;
/*!40000 ALTER TABLE `rol` DISABLE KEYS */;
INSERT INTO `rol` VALUES (1,'ROLE_ADMIN'),(2,'ROLE_JUGADOR'),(3,'ROLE_DIRECTOR_TECNICO');
/*!40000 ALTER TABLE `rol` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `solicitud_union`
--

DROP TABLE IF EXISTS `solicitud_union`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `solicitud_union` (
  `id` int NOT NULL AUTO_INCREMENT,
  `equipo_id` int NOT NULL,
  `jugador_id` varchar(20) COLLATE utf8mb4_general_ci NOT NULL,
  `estado` enum('PENDIENTE','APROBADA','RECHAZADA') COLLATE utf8mb4_general_ci DEFAULT 'PENDIENTE',
  `fecha_solicitud` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_solicitud` (`equipo_id`,`jugador_id`),
  KEY `FKwfrh4hku24r2v64h8ajht6qt` (`jugador_id`),
  CONSTRAINT `FKwfrh4hku24r2v64h8ajht6qt` FOREIGN KEY (`jugador_id`) REFERENCES `jugador` (`numero_documento`),
  CONSTRAINT `solicitud_union_ibfk_1` FOREIGN KEY (`equipo_id`) REFERENCES `equipo` (`id`) ON DELETE CASCADE,
  CONSTRAINT `solicitud_union_ibfk_2` FOREIGN KEY (`jugador_id`) REFERENCES `usuario` (`numero_documento`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `solicitud_union`
--

LOCK TABLES `solicitud_union` WRITE;
/*!40000 ALTER TABLE `solicitud_union` DISABLE KEYS */;
/*!40000 ALTER TABLE `solicitud_union` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `usuario`
--

DROP TABLE IF EXISTS `usuario`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `usuario` (
  `numero_documento` varchar(20) COLLATE utf8mb4_general_ci NOT NULL,
  `tipo_documento` enum('CEDULA_CIUDADANIA','CEDULA_EXTRANJERIA','PERMISO_PROTECCION_TEMPORAL','REGISTRO_CIVIL','TARJETA_IDENTIDAD') COLLATE utf8mb4_general_ci NOT NULL,
  `nombres` varchar(100) COLLATE utf8mb4_general_ci NOT NULL,
  `apellidos` varchar(100) COLLATE utf8mb4_general_ci NOT NULL,
  `email` varchar(100) COLLATE utf8mb4_general_ci NOT NULL,
  `password` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `fecha_nacimiento` date DEFAULT NULL,
  `eps` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `telefono` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `puede_jugar` bit(1) DEFAULT b'0',
  `activo` bit(1) DEFAULT b'1',
  `ultima_actualizacion_nombre` date DEFAULT NULL,
  PRIMARY KEY (`numero_documento`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `usuario`
--

LOCK TABLES `usuario` WRITE;
/*!40000 ALTER TABLE `usuario` DISABLE KEYS */;
INSERT INTO `usuario` VALUES ('1000000001','CEDULA_CIUDADANIA','Administrador','Sistema','admin@maracana.com','admin123',NULL,NULL,NULL,_binary '\0',_binary '',NULL),('101659469','REGISTRO_CIVIL','ALEJANDRO','ACERO','alpovedaac7d@gmail.com','alejo123','2005-07-05','Sanitas','3194785205',_binary '\0',_binary '',NULL),('1016594699','CEDULA_CIUDADANIA','Alejandro','Poveda Acero','alejandropovedaacero@gmail.com','alejo123','2005-07-05','Sanitas','3196147520',_binary '\0',_binary '','2025-06-16'),('1019023278','CEDULA_CIUDADANIA','Catherine','Acero','catherineacerog@gmail.com','cathe123','1987-03-23','Sanitas','3195277425',_binary '\0',_binary '\0',NULL),('312','CEDULA_CIUDADANIA','Daniel','Gonzalez','danielcorreo@gmail.com','daniel123','2005-07-05','Sanitas','90312',_binary '\0',_binary '',NULL),('5696448','PERMISO_PROTECCION_TEMPORAL','Anni','Alvins','annnjjk1@gmail.com','anni123','2006-01-24','Salud Total','3125037798',_binary '\0',_binary '',NULL);
/*!40000 ALTER TABLE `usuario` ENABLE KEYS */;
UNLOCK TABLES;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `tr_valida_puede_jugar` BEFORE UPDATE ON `usuario` FOR EACH ROW BEGIN
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
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `usuario_rol`
--

DROP TABLE IF EXISTS `usuario_rol`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `usuario_rol` (
  `usuario_id` varchar(20) COLLATE utf8mb4_general_ci NOT NULL,
  `rol_id` int NOT NULL,
  PRIMARY KEY (`usuario_id`,`rol_id`),
  KEY `rol_id` (`rol_id`),
  CONSTRAINT `usuario_rol_ibfk_1` FOREIGN KEY (`usuario_id`) REFERENCES `usuario` (`numero_documento`),
  CONSTRAINT `usuario_rol_ibfk_2` FOREIGN KEY (`rol_id`) REFERENCES `rol` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `usuario_rol`
--

LOCK TABLES `usuario_rol` WRITE;
/*!40000 ALTER TABLE `usuario_rol` DISABLE KEYS */;
INSERT INTO `usuario_rol` VALUES ('1000000001',1),('101659469',2),('1016594699',2),('1019023278',2),('312',2),('5696448',2);
/*!40000 ALTER TABLE `usuario_rol` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping events for database 'maracana'
--

--
-- Dumping routines for database 'maracana'
--
/*!50003 DROP PROCEDURE IF EXISTS `sp_crear_reserva` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_crear_reserva`(
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
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `sp_eliminar_reserva` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_eliminar_reserva`(
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
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `sp_listar_usuarios_paginados` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_listar_usuarios_paginados`(
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
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-06-18 19:55:51
