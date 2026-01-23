package com.hotel.auth.application.service;

import com.hotel.auth.domain.model.User;
import com.hotel.auth.domain.service.TokenService;
import com.hotel.auth.helpers.exceptions.TokenExpiredException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

@Service
public class TokenServiceImpl implements TokenService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenServiceImpl.class);

    @Value("${application.security.jwt.expiration}")
    private int jwtExpiration;

    @Value("${application.security.jwt.refresh-expiration}")
    private int jwtRefreshExpiration;

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;

    public TokenServiceImpl(JwtEncoder jwtEncoder, JwtDecoder jwtDecoder) {
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public String generateToken(Authentication authentication) {
        Instant now = Instant.now();
        String roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(" "));

        User currentUser = (User) authentication.getPrincipal();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(currentUser.getEmail())
                .issuedAt(now)
                .expiresAt(now.plus(jwtExpiration, ChronoUnit.SECONDS))
                .claim("roles", roles)
                .build();

        JwtEncoderParameters jwtEncoderParameters = JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                claims
        );

        return jwtEncoder.encode(jwtEncoderParameters).getTokenValue();
    }

    @Override
    public String generateRefreshToken(Authentication authentication) {
        Instant now = Instant.now();
        User currentUser = (User) authentication.getPrincipal();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(currentUser.getEmail())
                .issuedAt(now)
                .expiresAt(now.plus(jwtRefreshExpiration, ChronoUnit.SECONDS))
                .claim("type", "refresh")
                .build();

        JwtEncoderParameters jwtEncoderParameters = JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                claims
        );

        return jwtEncoder.encode(jwtEncoderParameters).getTokenValue();
    }

    @Override
    public String getUserFromToken(String token) {
        Jwt jwtToken = jwtDecoder.decode(token);
        return jwtToken.getSubject();
    }

    @Override
    public boolean validateToken(String token) {
        try {
            jwtDecoder.decode(token);
            return true;
        } catch (JwtException exception) {
            LOGGER.warn("Token inválido o expirado: {}", exception.getMessage());
            throw new TokenExpiredException("Error while trying to validate token");
        }
    }
}
