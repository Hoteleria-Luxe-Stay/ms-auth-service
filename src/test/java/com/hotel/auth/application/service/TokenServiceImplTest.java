package com.hotel.auth.application.service;

import com.hotel.auth.domain.model.Role;
import com.hotel.auth.domain.model.User;
import com.hotel.auth.helpers.exceptions.TokenExpiredException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenServiceImplTest {

    @Mock
    private JwtEncoder jwtEncoder;

    @Mock
    private JwtDecoder jwtDecoder;

    @InjectMocks
    private TokenServiceImpl tokenService;

    private User user;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(tokenService, "jwtExpiration", 3600);
        ReflectionTestUtils.setField(tokenService, "jwtRefreshExpiration", 86400);
        ReflectionTestUtils.setField(tokenService, "jwtIssuer", "luxestay-auth");
        ReflectionTestUtils.setField(tokenService, "jwtAudience", "luxestay-api");

        Role role = new Role();
        role.setRolename("USER");
        user = User.builder()
                .id(1L)
                .username("name")
                .email("test@luxestay.com")
                .role(role)
                .activo(true)
                .build();

        authentication = new UsernamePasswordAuthenticationToken(
                user,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Test
    void generateTokenReturnsEncodedTokenValue() {
        Jwt jwt = stubEncodedJwt("encoded-access-token");
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt);

        String token = tokenService.generateToken(authentication);

        assertThat(token).isEqualTo("encoded-access-token");
    }

    @Test
    void generateRefreshTokenReturnsEncodedTokenValue() {
        Jwt jwt = stubEncodedJwt("encoded-refresh-token");
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt);

        String token = tokenService.generateRefreshToken(authentication);

        assertThat(token).isEqualTo("encoded-refresh-token");
    }

    @Test
    void generateServiceTokenReturnsEncodedTokenValue() {
        Jwt jwt = stubEncodedJwt("encoded-service-token");
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt);

        String token = tokenService.generateServiceToken("hotel-client", "service:hotel");

        assertThat(token).isEqualTo("encoded-service-token");
    }

    @Test
    void getUserFromTokenReturnsSubject() {
        Jwt jwt = Jwt.withTokenValue("token-value")
                .header("alg", "RS256")
                .subject("test@luxestay.com")
                .claim("userId", 1L)
                .build();
        when(jwtDecoder.decode("token-value")).thenReturn(jwt);

        String subject = tokenService.getUserFromToken("token-value");

        assertThat(subject).isEqualTo("test@luxestay.com");
    }

    @Test
    void validateTokenReturnsTrueWhenJwtDecoderSucceeds() {
        Jwt jwt = Jwt.withTokenValue("valid-token")
                .header("alg", "RS256")
                .subject("test@luxestay.com")
                .build();
        when(jwtDecoder.decode("valid-token")).thenReturn(jwt);

        boolean valid = tokenService.validateToken("valid-token");

        assertThat(valid).isTrue();
    }

    @Test
    void validateTokenThrowsTokenExpiredExceptionWhenJwtDecoderFails() {
        when(jwtDecoder.decode("expired-token")).thenThrow(new JwtException("Token expired"));

        assertThatThrownBy(() -> tokenService.validateToken("expired-token"))
                .isInstanceOf(TokenExpiredException.class)
                .hasMessageContaining("Error while trying to validate token");
    }

    private Jwt stubEncodedJwt(String tokenValue) {
        return Jwt.withTokenValue(tokenValue)
                .header("alg", "RS256")
                .subject("test@luxestay.com")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claims(c -> c.putAll(Map.of("custom", "value")))
                .build();
    }
}
