-- =========================================================
-- ms-auth-service: Baseline schema
-- =========================================================
-- Refleja el estado actual de las entidades JPA:
--   User, Role, ServiceClient, PasswordResetToken
-- A partir de aquí, cada cambio de schema = nuevo script V{n}__descripcion.sql
-- =========================================================

CREATE TABLE roles (
    id        BIGINT       NOT NULL AUTO_INCREMENT,
    rolename  VARCHAR(255) NOT NULL,
    CONSTRAINT pk_roles PRIMARY KEY (id),
    CONSTRAINT uk_roles_rolename UNIQUE (rolename)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE users (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    username        VARCHAR(255) NOT NULL,
    email           VARCHAR(255) NOT NULL,
    password        VARCHAR(255) NOT NULL,
    role_id         BIGINT,
    telefono        VARCHAR(255),
    activo          BIT(1)       DEFAULT b'1',
    fecha_creacion  DATETIME(6),
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE service_clients (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    client_id      VARCHAR(255) NOT NULL,
    client_secret  VARCHAR(255) NOT NULL,
    scope          VARCHAR(255) NOT NULL,
    enabled        BIT(1)       DEFAULT b'1',
    CONSTRAINT pk_service_clients PRIMARY KEY (id),
    CONSTRAINT uk_service_clients_client_id UNIQUE (client_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE password_reset_tokens (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    code        VARCHAR(255),
    expires_at  DATETIME(6),
    used_at     DATETIME(6),
    created_at  DATETIME(6),
    CONSTRAINT pk_password_reset_tokens PRIMARY KEY (id),
    CONSTRAINT fk_prt_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_prt_user_id ON password_reset_tokens (user_id);
CREATE INDEX idx_prt_code    ON password_reset_tokens (code);
