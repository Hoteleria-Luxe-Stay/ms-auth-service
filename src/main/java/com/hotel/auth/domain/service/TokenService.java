package com.hotel.auth.domain.service;

import org.springframework.security.core.Authentication;

public interface TokenService {

    String generateToken(Authentication authentication);

    String generateRefreshToken(Authentication authentication);

    String generateServiceToken(String clientId, String scope);

    String getUserFromToken(String token);

    boolean validateToken(String token);
}
