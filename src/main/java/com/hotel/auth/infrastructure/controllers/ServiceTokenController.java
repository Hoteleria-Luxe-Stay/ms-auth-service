package com.hotel.auth.infrastructure.controllers;

import com.hotel.auth.domain.model.ServiceClient;
import com.hotel.auth.domain.repository.ServiceClientRepository;
import com.hotel.auth.domain.service.TokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ServiceTokenController {

    private final ServiceClientRepository serviceClientRepository;
    private final TokenService tokenService;

    @Value("${application.security.jwt.expiration}")
    private int jwtExpiration;

    public ServiceTokenController(ServiceClientRepository serviceClientRepository, TokenService tokenService) {
        this.serviceClientRepository = serviceClientRepository;
        this.tokenService = tokenService;
    }

    @PostMapping(value = "/oauth/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Map<String, Object>> token(
            @RequestParam("grant_type") String grantType,
            @RequestParam("client_id") String clientId,
            @RequestParam("client_secret") String clientSecret,
            @RequestParam(value = "scope", required = false) String scope
    ) {
        if (!"client_credentials".equals(grantType)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "unsupported_grant_type"));
        }

        ServiceClient client = serviceClientRepository.findByClientIdAndEnabledTrue(clientId)
                .orElse(null);

        if (client == null || !clientSecret.equals(client.getClientSecret())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "invalid_client"));
        }

        String token = tokenService.generateServiceToken(client.getClientId(), client.getScope());

        return ResponseEntity.ok(Map.of(
                "access_token", token,
                "token_type", "bearer",
                "expires_in", jwtExpiration,
                "scope", client.getScope()
        ));
    }
}
