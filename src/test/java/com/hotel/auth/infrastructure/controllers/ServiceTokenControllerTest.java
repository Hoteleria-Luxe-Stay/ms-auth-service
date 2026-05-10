package com.hotel.auth.infrastructure.controllers;

import com.hotel.auth.domain.model.ServiceClient;
import com.hotel.auth.domain.repository.ServiceClientRepository;
import com.hotel.auth.domain.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceTokenControllerTest {

    @Mock private ServiceClientRepository serviceClientRepository;
    @Mock private TokenService tokenService;

    @InjectMocks
    private ServiceTokenController controller;

    private ServiceClient client;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "jwtExpiration", 3600);

        client = ServiceClient.builder()
                .id(1L)
                .clientId("hotel-client")
                .clientSecret("secret-x")
                .scope("service:hotel")
                .enabled(true)
                .build();
    }

    @Test
    void tokenReturnsBadRequestWhenGrantTypeUnsupported() {
        ResponseEntity<Map<String, Object>> response =
                controller.token("password", "hotel-client", "secret-x", "service:hotel");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "unsupported_grant_type");
    }

    @Test
    void tokenReturnsUnauthorizedWhenClientNotFound() {
        when(serviceClientRepository.findByClientIdAndEnabledTrue("missing-client"))
                .thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> response =
                controller.token("client_credentials", "missing-client", "any", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).containsEntry("error", "invalid_client");
    }

    @Test
    void tokenReturnsUnauthorizedWhenSecretMismatch() {
        when(serviceClientRepository.findByClientIdAndEnabledTrue("hotel-client"))
                .thenReturn(Optional.of(client));

        ResponseEntity<Map<String, Object>> response =
                controller.token("client_credentials", "hotel-client", "wrong-secret", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).containsEntry("error", "invalid_client");
    }

    @Test
    void tokenReturnsOkWithAccessTokenWhenCredentialsValid() {
        when(serviceClientRepository.findByClientIdAndEnabledTrue("hotel-client"))
                .thenReturn(Optional.of(client));
        when(tokenService.generateServiceToken("hotel-client", "service:hotel"))
                .thenReturn("service-token-x");

        ResponseEntity<Map<String, Object>> response =
                controller.token("client_credentials", "hotel-client", "secret-x", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("access_token", "service-token-x");
        assertThat(response.getBody()).containsEntry("token_type", "bearer");
        assertThat(response.getBody()).containsEntry("scope", "service:hotel");
        assertThat(response.getBody()).containsEntry("expires_in", 3600);
    }
}
