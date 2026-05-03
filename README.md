# Auth Service - Sistema de Reserva de Hoteles

Microservicio de autenticación y autorización con **JWT firmado en RSA (RS256)**. Gestiona usuarios, roles, login, registro, reset de contraseña, y emite tokens técnicos (OAuth2 `client_credentials`) para los demás microservicios.

## Información del Servicio

| Propiedad | Valor |
|-----------|-------|
| Puerto | 8081 |
| Java | 21 |
| Spring Boot | 3.4.0 |
| Spring Cloud | 2024.0.1 |
| Context Path | `/api/v1` |
| Base de Datos | MySQL |
| Algoritmo JWT | **RS256 (RSA SHA-256)** |
| Firma | Clave privada RSA en este servicio; clave pública distribuida al gateway y a los servicios validadores |

## Estructura del Proyecto (Hexagonal)

```
ms-auth-service/
├── pom.xml
├── Dockerfile
├── env.example                ← copialo a .env y completalo en DEV
├── contracts/
│   └── auth-service-api.yaml
└── src/main/
    ├── java/com/hotel/auth/
    │   ├── AuthServiceApplication.java
    │   ├── application/
    │   │   ├── mapper/AuthMapper.java
    │   │   └── service/ (AuthServiceImpl, TokenServiceImpl, UserServiceImpl)
    │   ├── domain/
    │   │   ├── model/ (User, Role, ServiceClient, PasswordResetToken)
    │   │   ├── repository/
    │   │   └── service/
    │   ├── infrastructure/
    │   │   ├── config/ (SecurityConfig, RabbitConfig, EncoderConfig)
    │   │   ├── controllers/ (AuthController, ServiceTokenController)
    │   │   ├── events/ (UserRegisteredEvent, UserLoginEvent, PasswordResetEvent, EventPublisher)
    │   │   └── filters/JwtAuthenticationFilter.java
    │   └── helpers/
    │       ├── DataInit.java
    │       └── exceptions/ (GlobalExceptionHandler, BusinessException, ...)
    └── resources/
        └── application.yml    ← bootstrap mínimo (config-server lo hidrata)
```

## Endpoints

### Autenticación de Usuarios (públicos)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/v1/auth/register` | Registrar nuevo usuario |
| POST | `/api/v1/auth/login` | Iniciar sesión (devuelve access + refresh token) |
| POST | `/api/v1/auth/refresh` | Refrescar token |
| POST | `/api/v1/auth/validate` | Validar token (uso interno) |
| POST | `/api/v1/auth/password/forgot` | Solicitar reset de contraseña |
| POST | `/api/v1/auth/password/reset` | Confirmar reset con token |

### Tokens de Servicio (OAuth2 client_credentials)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/v1/oauth/token` | Emitir token técnico (`grant_type=client_credentials`) para otros microservicios |

### Usuarios (protegidos por JWT)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/v1/users/me` | Obtener usuario actual |
| GET | `/api/v1/users/{id}` | Obtener usuario por ID |

## Variables de Entorno

Las variables se inyectan desde un archivo `.env` en DEV (cargado por el IDE) y desde `.env.prod` en PROD (consumido por `docker-compose.prod.yml`).

| Variable | Obligatoria | Descripción | Ejemplo (DEV) |
|----------|-------------|-------------|---------------|
| `CONFIG_IMPORT` | No | Import de Spring Cloud Config | `optional:configserver:http://localhost:8888` |
| `CONFIG_FAIL_FAST` | No | Falla rápido si config-server no responde | `false` (DEV) / `true` (PROD) |
| `SERVER_PORT` | No | Puerto HTTP (default 8081) | `8081` |
| `EUREKA_URL` | No | URL de Eureka (default `http://discovery-service:8761/eureka`) | `http://localhost:8761/eureka` |
| `SPRING_DATASOURCE_URL` | **Sí** | JDBC URL de MySQL | `jdbc:mysql://localhost:3307/auth_db` |
| `SPRING_DATASOURCE_USERNAME` | **Sí** | Usuario MySQL | - |
| `SPRING_DATASOURCE_PASSWORD` | **Sí** | Contraseña MySQL | - |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | No | Default `validate` (PROD-safe) | `update` (DEV) |
| `SPRING_JPA_SHOW_SQL` | No | Default `false` (PROD-safe) | `true` (DEV) |
| `SPRING_RABBITMQ_HOST` | **Sí** | Host RabbitMQ | `localhost` |
| `SPRING_RABBITMQ_PORT` | **Sí** | Puerto RabbitMQ | `5672` |
| `SPRING_RABBITMQ_USERNAME` | **Sí** | Usuario RabbitMQ | - |
| `SPRING_RABBITMQ_PASSWORD` | **Sí** | Contraseña RabbitMQ | - |
| `JWT_PRIVATE_KEY` | **Sí** | Clave privada RSA en PEM (1 línea con `\n`) | - |
| `JWT_PUBLIC_KEY` | **Sí** | Clave pública RSA en PEM (1 línea con `\n`) | - |
| `JWT_EXPIRATION` | No | Expiración del access token en segundos (default 3600) | `3600` |
| `JWT_REFRESH_EXPIRATION` | No | Expiración del refresh token en segundos (default 86400) | `86400` |
| `PASSWORD_RESET_EXPIRATION_MINUTES` | No | Default 15 | `15` |
| `PASSWORD_RESET_MAX_ATTEMPTS` | No | Default 3 | `3` |
| `PASSWORD_RESET_BLOCK_MINUTES` | No | Default 15 | `15` |
| `HOTEL_SERVICE_CLIENT_ID` | **Sí*** | Client ID del hotel-service para OAuth2 c.c. | - |
| `HOTEL_SERVICE_CLIENT_SECRET` | **Sí*** | Client secret del hotel-service | - |
| `RESERVA_SERVICE_CLIENT_ID` | **Sí*** | Client ID del reserva-service | - |
| `RESERVA_SERVICE_CLIENT_SECRET` | **Sí*** | Client secret del reserva-service | - |
| `NOTIFICACION_SERVICE_CLIENT_ID` | **Sí*** | Client ID del notificacion-service | - |
| `NOTIFICACION_SERVICE_CLIENT_SECRET` | **Sí*** | Client secret del notificacion-service | - |
| `CORS_ALLOWED_ORIGINS` | **Sí** | Origen permitido para CORS | `http://localhost:4200` |

\* Si quedan vacíos, `DataInit` no siembra el `ServiceClient` correspondiente y el resto de microservicios no podrán autenticarse service-to-service.

### Generar el keypair RSA

```bash
openssl genrsa -out private.pem 2048
openssl rsa -pubout -in private.pem -out public.pem
```

Convertí cada PEM a una sola línea reemplazando los saltos por `\n` literal antes de inyectarlo como env var.

## Eventos RabbitMQ (publisher)

El servicio publica en el `TopicExchange` `hotel.events` (configurable via `RABBITMQ_SESION_EXCHANGE`):

| Evento | Routing Key | Trigger |
|--------|-------------|---------|
| `UserRegisteredEvent` | `user.registered` | Nuevo registro |
| `UserLoginEvent` | `user.login` | Login exitoso |
| `PasswordResetEvent` | `user.password.reset` | Solicitud de reset de contraseña |

El `notificacion-service` consume estos eventos para enviar correos.

## Datos Iniciales (al primer arranque)

`DataInit.java` ejecuta al iniciar:

1. **Roles**: crea `USER` y `ADMIN` si no existen.
2. **Service Clients**: siembra los registros para que `hotel-service`, `reserva-service` y `notificacion-service` puedan autenticarse contra `/oauth/token`. Si los `*_CLIENT_ID/SECRET` no están seteados, el seeding se omite con un warning.

## Seguridad

- **Algoritmo JWT**: RS256 (RSA SHA-256).
- **Password Encoding**: BCrypt (`EncoderConfig`).
- **Sesiones**: STATELESS — el servicio no mantiene estado de sesión.
- **Filter chain**: `JwtAuthenticationFilter` valida el access token con la clave pública.
- **CORS**: deshabilitado en el servicio (lo maneja el `api-gateway`).
- **Rutas públicas**: `/auth/**`, `/oauth/token`, `/api-docs/**`, `/swagger-ui/**`, `/actuator/**`.

## Schema Migrations (Flyway)

El schema está versionado con **Flyway**. Cada cambio = nuevo script en `src/main/resources/db/migration/` con naming `V{n}__descripcion.sql`.

- `V1__init_schema.sql` — estado inicial: `roles`, `users`, `service_clients`, `password_reset_tokens` con FKs e índices.
- Cambios futuros: `V2__...sql`, `V3__...sql`. **NUNCA se edita un script ya aplicado** — siempre se agrega uno nuevo.
- Flyway corre **antes** que Hibernate: aplica los scripts pendientes y luego Hibernate valida (`ddl-auto: validate`) que las entidades calzan con el schema.
- Tabla de control: `flyway_schema_history` (la crea Flyway al arrancar).

### Variables relevantes

| Variable | Default | Descripción |
|----------|---------|-------------|
| `SPRING_FLYWAY_ENABLED` | `true` | Activa/desactiva Flyway |
| `SPRING_FLYWAY_BASELINE_ON_MIGRATE` | `false` | `true` solo si la DB ya tenía tablas pre-Flyway |
| `SPRING_FLYWAY_VALIDATE_ON_MIGRATE` | `true` | Valida checksums de scripts ya aplicados |

### Workflow primera vez

1. Crear el schema vacío en MySQL: `CREATE DATABASE auth_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;`
2. Levantar el servicio → Flyway aplica `V1` automáticamente.
3. Verificar con `SELECT version, description, success FROM flyway_schema_history;`.

## Ejecución Local (DEV)

```bash
# 1. Levantar la infra (MySQL + RabbitMQ + Kafka)
docker-compose -f docker-compose.infra.yml up -d

# 2. Copiar template de variables y completarlas
cp env.example .env
# editar .env con los valores reales (especialmente JWT_PRIVATE_KEY/PUBLIC_KEY y credenciales)

# 3. Run desde IntelliJ con plugin EnvFile apuntando a .env,
#    o desde CLI con un loader de envs.
mvn spring-boot:run

# Swagger UI
open http://localhost:8081/api/v1/swagger-ui.html
```

## Ejecución en Docker (PROD)

El servicio se construye con `Dockerfile` (multi-stage build, JRE 21 alpine, usuario no-root, healthcheck en `/api/v1/actuator/health`). Se levanta como parte de `docker-compose.prod.yml` (a definir) consumiendo `.env.prod` con TODAS las variables marcadas como obligatorias.

## Troubleshooting

| Síntoma | Causa probable | Solución |
|---------|----------------|----------|
| `Service client X no configurado. Omitiendo.` | Faltan `*_CLIENT_ID/SECRET` en el `.env` | Setear las 3 parejas de credenciales |
| `Could not resolve placeholder ...` | Falta env var obligatoria | Revisar la tabla, especialmente `CORS_ALLOWED_ORIGINS`, `JWT_PRIVATE_KEY`, `JWT_PUBLIC_KEY` |
| 401 desde otros servicios validando JWT | La clave pública no es pareja de la privada | Regenerar el keypair y propagar a gateway + servicios |
| Config-server timeouts al iniciar | `CONFIG_FAIL_FAST=true` con config-server caído | DEV: usar `CONFIG_FAIL_FAST=false`; PROD: levantar config-server primero |
