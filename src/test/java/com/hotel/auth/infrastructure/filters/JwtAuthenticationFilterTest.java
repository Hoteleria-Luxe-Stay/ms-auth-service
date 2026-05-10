package com.hotel.auth.infrastructure.filters;

import com.hotel.auth.domain.model.Role;
import com.hotel.auth.domain.model.User;
import com.hotel.auth.domain.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock private AuthService authService;
    @Mock private UserDetailsService userDetailsService;
    @Mock private JwtDecoder jwtDecoder;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    private User user;

    @BeforeEach
    void setUp() {
        Role role = new Role();
        role.setRolename("USER");
        user = User.builder()
                .id(1L)
                .username("name")
                .email("user@luxestay.com")
                .password("hashed")
                .role(role)
                .activo(true)
                .build();
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    // ==================== shouldNotFilter ====================

    @ParameterizedTest
    @ValueSource(strings = {
            "/auth/login",
            "/auth/register",
            "/auth/refresh",
            "/auth/validate",
            "/oauth/token",
            "/api-docs",
            "/swagger-ui",
            "/swagger-ui.html",
            "/swagger-ui/index.html",
            "/error"
    })
    void shouldNotFilterReturnsTrueForPublicPaths(String path) {
        when(request.getServletPath()).thenReturn(path);

        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void shouldNotFilterReturnsFalseForProtectedPath() {
        when(request.getServletPath()).thenReturn("/api/v1/users/me");

        assertThat(filter.shouldNotFilter(request)).isFalse();
    }

    // ==================== doFilterInternal ====================

    @Test
    void doFilterInternalSkipsAuthForPublicPath() throws Exception {
        when(request.getServletPath()).thenReturn("/auth/login");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        verify(authService, never()).validateToken(anyString());
    }

    @Test
    void doFilterInternalContinuesChainWhenNoTokenHeader() throws Exception {
        when(request.getServletPath()).thenReturn("/api/v1/users/me");
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        verify(authService, never()).validateToken(anyString());
    }

    @Test
    void doFilterInternalContinuesChainWhenAuthorizationHeaderMissingBearerPrefix() throws Exception {
        when(request.getServletPath()).thenReturn("/api/v1/users/me");
        when(request.getHeader("Authorization")).thenReturn("Basic abc123");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        verify(authService, never()).validateToken(anyString());
    }

    @Test
    void doFilterInternalReturns401WhenTokenInvalid() throws Exception {
        when(request.getServletPath()).thenReturn("/api/v1/users/me");
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");
        when(authService.validateToken("invalid-token"))
                .thenThrow(new JwtException("invalid"));

        filter.doFilterInternal(request, response, filterChain);

        verify(response, times(1)).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void doFilterInternalAuthenticatesAsServiceWhenTokenHasClientId() throws Exception {
        Jwt jwt = Jwt.withTokenValue("svc-token")
                .header("alg", "RS256")
                .subject("hotel-client")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("client_id", "hotel-client")
                .claim("scope", "service:hotel")
                .build();

        when(request.getServletPath()).thenReturn("/api/v1/internal/anything");
        when(request.getHeader("Authorization")).thenReturn("Bearer svc-token");
        when(authService.validateToken("svc-token")).thenReturn(true);
        when(jwtDecoder.decode("svc-token")).thenReturn(jwt);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isEqualTo("hotel-client");
    }

    @Test
    void doFilterInternalAuthenticatesAsUserWhenTokenHasUserId() throws Exception {
        Jwt jwt = Jwt.withTokenValue("user-token")
                .header("alg", "RS256")
                .subject("user@luxestay.com")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("userId", 1L)
                .claim("roles", "ROLE_USER")
                .build();

        when(request.getServletPath()).thenReturn("/api/v1/users/me");
        when(request.getHeader("Authorization")).thenReturn("Bearer user-token");
        when(authService.validateToken("user-token")).thenReturn(true);
        when(jwtDecoder.decode("user-token")).thenReturn(jwt);
        when(authService.getUserFromToken("user-token")).thenReturn("user@luxestay.com");
        when(userDetailsService.loadUserByUsername("user@luxestay.com")).thenReturn(user);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isEqualTo(user);
    }
}
