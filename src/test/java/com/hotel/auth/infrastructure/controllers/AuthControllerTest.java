package com.hotel.auth.infrastructure.controllers;

import com.hotel.auth.api.dto.AuthResponse;
import com.hotel.auth.api.dto.LoginRequest;
import com.hotel.auth.api.dto.MessageResponse;
import com.hotel.auth.api.dto.PasswordResetConfirmRequest;
import com.hotel.auth.api.dto.PasswordResetRequest;
import com.hotel.auth.api.dto.PasswordResetVerifyRequest;
import com.hotel.auth.api.dto.RefreshTokenRequest;
import com.hotel.auth.api.dto.RegisterRequest;
import com.hotel.auth.api.dto.TokenValidationResponse;
import com.hotel.auth.api.dto.UserResponse;
import com.hotel.auth.api.dto.ValidateTokenRequest;
import com.hotel.auth.domain.model.Role;
import com.hotel.auth.domain.model.User;
import com.hotel.auth.domain.service.AuthService;
import com.hotel.auth.domain.service.TokenService;
import com.hotel.auth.domain.service.UserService;
import com.hotel.auth.helpers.exceptions.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private AuthService authService;
    @Mock private TokenService tokenService;
    @Mock private UserService userService;

    @InjectMocks
    private AuthController authController;

    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authController, "jwtExpiration", 3600);

        Role role = new Role();
        role.setRolename("USER");
        user = User.builder()
                .id(1L)
                .username("testuser")
                .email("user@luxestay.com")
                .password("encoded")
                .role(role)
                .activo(true)
                .build();
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    // ==================== register ====================

    @Test
    void registerReturnsCreatedWithAuthResponse() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("user@luxestay.com");
        req.setPassword("Password123");
        req.setUsername("testuser");

        when(authService.login(any(LoginRequest.class)))
                .thenReturn(Map.of("access-token", "access-x"));
        when(userService.findByEmail("user@luxestay.com")).thenReturn(Optional.of(user));
        when(tokenService.generateRefreshToken(any())).thenReturn("refresh-x");

        ResponseEntity<AuthResponse> response = authController.register(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getAccessToken()).isEqualTo("access-x");
        assertThat(response.getBody().getRefreshToken()).isEqualTo("refresh-x");
        verify(authService).createUser(req);
    }

    @Test
    void registerThrowsWhenUserNotFoundAfterRegistration() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("missing@luxestay.com");
        req.setPassword("Password123");
        req.setUsername("missing");

        when(authService.login(any(LoginRequest.class)))
                .thenReturn(Map.of("access-token", "access-x"));
        when(userService.findByEmail("missing@luxestay.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authController.register(req))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ==================== login ====================

    @Test
    void loginReturnsOkWithAuthResponse() {
        LoginRequest req = new LoginRequest();
        req.setEmail("user@luxestay.com");
        req.setPassword("Password123");

        when(authService.login(req)).thenReturn(Map.of("access-token", "access-x"));
        when(userService.findByEmail("user@luxestay.com")).thenReturn(Optional.of(user));
        when(tokenService.generateRefreshToken(any())).thenReturn("refresh-x");

        ResponseEntity<AuthResponse> response = authController.login(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getAccessToken()).isEqualTo("access-x");
    }

    @Test
    void loginThrowsWhenUserNotFound() {
        LoginRequest req = new LoginRequest();
        req.setEmail("missing@luxestay.com");
        req.setPassword("any");

        when(authService.login(req)).thenReturn(Map.of("access-token", "x"));
        when(userService.findByEmail("missing@luxestay.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authController.login(req))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ==================== refreshToken ====================

    @Test
    void refreshTokenReturnsOkWithNewTokens() {
        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("old-refresh");

        when(authService.validateToken("old-refresh")).thenReturn(true);
        when(authService.getUserFromToken("old-refresh")).thenReturn("user@luxestay.com");
        when(userService.findByEmail("user@luxestay.com")).thenReturn(Optional.of(user));
        when(tokenService.generateToken(any())).thenReturn("new-access");
        when(tokenService.generateRefreshToken(any())).thenReturn("new-refresh");

        ResponseEntity<AuthResponse> response = authController.refreshToken(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getAccessToken()).isEqualTo("new-access");
        assertThat(response.getBody().getRefreshToken()).isEqualTo("new-refresh");
    }

    @Test
    void refreshTokenThrowsWhenUserNotFound() {
        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("token");

        when(authService.validateToken("token")).thenReturn(true);
        when(authService.getUserFromToken("token")).thenReturn("missing@luxestay.com");
        when(userService.findByEmail("missing@luxestay.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authController.refreshToken(req))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ==================== validateToken ====================

    @Test
    void validateTokenReturnsOkWhenTokenValid() {
        ValidateTokenRequest req = new ValidateTokenRequest();
        req.setToken("valid-token");

        when(authService.validateToken("valid-token")).thenReturn(true);
        when(authService.getUserFromToken("valid-token")).thenReturn("user@luxestay.com");
        when(userService.findByEmail("user@luxestay.com")).thenReturn(Optional.of(user));

        ResponseEntity<TokenValidationResponse> response = authController.validateToken(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getValid()).isTrue();
        assertThat(response.getBody().getEmail()).isEqualTo("user@luxestay.com");
    }

    @Test
    void validateTokenThrowsWhenUserNotFound() {
        ValidateTokenRequest req = new ValidateTokenRequest();
        req.setToken("any");

        when(authService.validateToken("any")).thenReturn(true);
        when(authService.getUserFromToken("any")).thenReturn("ghost@luxestay.com");
        when(userService.findByEmail("ghost@luxestay.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authController.validateToken(req))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ==================== password reset endpoints ====================

    @Test
    void requestPasswordResetReturnsOkWithMessage() {
        PasswordResetRequest req = new PasswordResetRequest();
        req.setEmail("user@luxestay.com");

        ResponseEntity<MessageResponse> response = authController.requestPasswordReset(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getMessage()).contains("Si el correo existe");
        verify(authService).requestPasswordReset("user@luxestay.com");
    }

    @Test
    void verifyPasswordResetReturnsOkWithMessage() {
        PasswordResetVerifyRequest req = new PasswordResetVerifyRequest();
        req.setEmail("user@luxestay.com");
        req.setCode("123456");

        ResponseEntity<MessageResponse> response = authController.verifyPasswordReset(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getMessage()).contains("Código verificado");
        verify(authService).verifyPasswordResetCode("user@luxestay.com", "123456");
    }

    @Test
    void resetPasswordReturnsOkWithMessage() {
        PasswordResetConfirmRequest req = new PasswordResetConfirmRequest();
        req.setEmail("user@luxestay.com");
        req.setCode("123456");
        req.setNewPassword("New12345");

        ResponseEntity<MessageResponse> response = authController.resetPassword(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getMessage()).contains("Contraseña actualizada");
        verify(authService).resetPassword("user@luxestay.com", "123456", "New12345");
    }

    // ==================== getCurrentUser ====================

    @Test
    void getCurrentUserReturnsOkWhenAuthenticated() {
        Authentication auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(userService.findByEmail("user@luxestay.com")).thenReturn(Optional.of(user));

        ResponseEntity<UserResponse> response = authController.getCurrentUser();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getEmail()).isEqualTo("user@luxestay.com");
    }

    @Test
    void getCurrentUserThrowsWhenAuthenticationMissing() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> authController.getCurrentUser())
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getCurrentUserThrowsWhenPrincipalIsNotUser() {
        Authentication auth = new UsernamePasswordAuthenticationToken("string-principal", null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThatThrownBy(() -> authController.getCurrentUser())
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getCurrentUserThrowsWhenUserNotInDb() {
        Authentication auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(userService.findByEmail("user@luxestay.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authController.getCurrentUser())
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ==================== getUserById ====================

    @Test
    void getUserByIdReturnsOk() {
        when(authService.getUser(1L)).thenReturn(user);

        ResponseEntity<UserResponse> response = authController.getUserById(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getEmail()).isEqualTo("user@luxestay.com");
    }

    // ==================== getRequest ====================

    @Test
    void getRequestReturnsEmptyOptional() {
        assertThat(authController.getRequest()).isEmpty();
    }
}
