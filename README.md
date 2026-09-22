# Club Maracaná 🏟️

Sistema de gestión y reservas para el Club Social y Deportivo Maracaná: una plataforma web donde los jugadores reservan canchas y pagan en línea, y los administradores gestionan usuarios, canchas, reservas y reportes.

## Funcionalidades

Jugadores
- Registro e inicio de sesión.
- Creación de reservas (fecha, hora y cancha disponible) y pago en línea.
- Visualización y cancelación de reservas propias.

Administradores
- Gestión de usuarios (crear, editar, eliminar, bloquear).
- Gestión de canchas y disponibilidad.
- Gestión y filtrado de reservas (por fecha, estado o cancha).
- Generación de reportes y estadísticas de uso.
- Notificaciones por correo.

## Stack técnico

- Java 21 + Spring Boot 3.4
- Spring Data JPA / Hibernate — persistencia
- Spring Security — autenticación y roles (jugador / administrador)
- Thymeleaf + Bootstrap — vistas del lado del servidor
- MySQL + Flyway — base de datos y control de migraciones
- Spring Mail — notificaciones por correo
- iText / Apache POI / OpenCSV — exportación de reportes a PDF, Excel y CSV

## Cómo correr el proyecto

Requisitos: JDK 21, MySQL corriendo localmente, Maven (o usar el wrapper incluido).

```
git clone https://github.com/AlejoA0/club-maracana.git
cd club-maracana
./mvnw spring-boot:run
```

Las migraciones de base de datos se aplican automáticamente con Flyway al iniciar la aplicación.

## Documentación

El repositorio incluye un [manual de usuario](manual-usuario.md) completo con las guías paso a paso tanto para jugadores como para administradores.
