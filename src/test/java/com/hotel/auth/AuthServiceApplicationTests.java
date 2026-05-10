package com.hotel.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Smoke tests para Auth Service.
 *
 * Estos tests validan que el classpath del modulo es correcto y que la clase
 * principal de Spring Boot es loadeable. No levantan el ApplicationContext de
 * Spring (eso requeriria mockear Eureka/Config Server/RabbitMQ/MySQL).
 *
 * Tests reales con TestContainers + MockBeans pendientes para sesion futura.
 */
class AuthServiceApplicationTests {

    @Test
    void mainApplicationClassIsLoadable() {
        assertNotNull(AuthServiceApplication.class,
            "AuthServiceApplication.class debe poder cargarse desde el classpath");
    }

    @Test
    void mainMethodExists() {
        assertDoesNotThrow(
            () -> AuthServiceApplication.class.getMethod("main", String[].class),
            "AuthServiceApplication debe exponer un metodo main(String[]) (bootstrap Spring Boot)"
        );
    }
}
