package com.hotel.auth.application.service;

import com.hotel.auth.api.dto.LoginRequest;
import com.hotel.auth.api.dto.RegisterRequest;
import com.hotel.auth.domain.model.PasswordResetToken;
import com.hotel.auth.domain.model.Role;
import com.hotel.auth.domain.model.User;
import com.hotel.auth.domain.repository.PasswordResetTokenRepository;
import com.hotel.auth.domain.repository.RoleRepository;
import com.hotel.auth.domain.repository.UserRepository;
import com.hotel.auth.domain.service.TokenService;
import com.hotel.auth.helpers.exceptions.ConflictException;
import com.hotel.auth.helpers.exceptions.EntityNotFoundException;
import com.hotel.auth.helpers.exceptions.ValidationException;
import com.hotel.auth.infrastructure.events.EventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private TokenService tokenService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationConfiguration authenticationConfiguration;
    @Mock private RoleRepository roleRepository;
    @Mock private EventPublisher eventPublisher;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private Authentication authentication;

    @InjectMocks
    private AuthServiceImpl authService;

    private User user;
    private Role role;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "passwordResetExpirationMinutes", 30);
        ReflectionTestUtils.setField(authService, "passwordResetMaxAttempts", 3);
        ReflectionTestUtils.setField(authService, "passwordResetBlockMinutes", 15);

        role = new Role();
        role.setId(1L);
        role.setRolename("USER");

        user = User.builder()
                .id(1L)
                .username("name")
                .email("user@luxestay.com")
                .password("encoded-password")
                .role(role)
                .activo(true)
                .build();
    }

    // ==================== login ====================

    @Test
    void loginSuccess() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("user@luxestay.com");
        req.setPassword("Password123");

        when(authenticationConfiguration.getAuthenticationManager()).thenReturn(authenticationManager);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);
        when(tokenService.generateToken(authentication)).thenReturn("access-token-x");

        Map<String, String> result = authService.login(req);

        assertThat(result).containsEntry("access-token", "access-token-x");
        verify(eventPublisher, times(1)).publishUserLogin(any());
    }

    @Test
    void loginThrowsBadCredentialsWhenAuthenticationFails() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("user@luxestay.com");
        req.setPassword("wrong");

        when(authenticationConfiguration.getAuthenticationManager()).thenReturn(authenticationManager);
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("invalid"));

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void loginThrowsUsernameNotFoundWhenUserMissing() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("missing@luxestay.com");
        req.setPassword("any");

        when(authenticationConfiguration.getAuthenticationManager()).thenReturn(authenticationManager);
        when(authenticationManager.authenticate(any()))
                .thenThrow(new UsernameNotFoundException("not found"));

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void loginThrowsDisabledExceptionWhenUserDisabled() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("disabled@luxestay.com");
        req.setPassword("any");

        when(authenticationConfiguration.getAuthenticationManager()).thenReturn(authenticationManager);
        when(authenticationManager.authenticate(any()))
                .thenThrow(new DisabledException("disabled"));

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(DisabledException.class);
    }

    @Test
    void loginWrapsUnexpectedExceptionAsBadCredentials() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("user@luxestay.com");
        req.setPassword("any");

        when(authenticationConfiguration.getAuthenticationManager())
                .thenThrow(new RuntimeException("unexpected"));

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("autenticación");
    }

    // ==================== validateToken / getUserFromToken ====================

    @Test
    void validateTokenDelegatesToTokenService() {
        when(tokenService.validateToken("token-x")).thenReturn(true);

        boolean result = authService.validateToken("token-x");

        assertThat(result).isTrue();
    }

    @Test
    void getUserFromTokenDelegatesToTokenService() {
        when(tokenService.getUserFromToken("token-x")).thenReturn("user@luxestay.com");

        String result = authService.getUserFromToken("token-x");

        assertThat(result).isEqualTo("user@luxestay.com");
    }

    // ==================== createUser ====================

    @Test
    void createUserSuccess() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("new@luxestay.com");
        req.setUsername("newuser");
        req.setPassword("Password123");
        req.setTelefono("+5491100000000");

        when(userRepository.findByEmail("new@luxestay.com")).thenReturn(Optional.empty());
        when(roleRepository.findByRolename("USER")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("Password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(42L);
            return u;
        });

        authService.createUser(req);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("new@luxestay.com");
        assertThat(captor.getValue().getPassword()).isEqualTo("hashed");
        assertThat(captor.getValue().getTelefono()).isEqualTo("+5491100000000");
        assertThat(captor.getValue().getRole()).isEqualTo(role);
        verify(eventPublisher, times(1)).publishUserRegistered(any());
    }

    @Test
    void createUserThrowsConflictExceptionWhenEmailExists() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("dup@luxestay.com");
        req.setUsername("dup");
        req.setPassword("any");

        when(userRepository.findByEmail("dup@luxestay.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.createUser(req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Email ya registrado");

        verify(userRepository, never()).save(any());
    }

    @Test
    void createUserThrowsEntityNotFoundExceptionWhenDefaultRoleMissing() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("new@luxestay.com");
        req.setUsername("new");
        req.setPassword("any");

        when(userRepository.findByEmail("new@luxestay.com")).thenReturn(Optional.empty());
        when(roleRepository.findByRolename("USER")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.createUser(req))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ==================== getUser / loadUserByUsername ====================

    @Test
    void getUserSuccess() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        User result = authService.getUser(1L);

        assertThat(result).isEqualTo(user);
    }

    @Test
    void getUserThrowsUsernameNotFoundWhenMissing() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getUser(999L))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void loadUserByUsernameSuccess() {
        when(userRepository.findByEmail("user@luxestay.com")).thenReturn(Optional.of(user));

        UserDetails result = authService.loadUserByUsername("user@luxestay.com");

        assertThat(result).isEqualTo(user);
    }

    @Test
    void loadUserByUsernameThrowsUsernameNotFoundWhenMissing() {
        when(userRepository.findByEmail("missing@luxestay.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.loadUserByUsername("missing@luxestay.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    // ==================== requestPasswordReset ====================

    @Test
    void requestPasswordResetSuccess() {
        when(userRepository.findByEmail("user@luxestay.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.countByUserAndCreatedAtAfter(eq(user), any())).thenReturn(0L);
        when(passwordResetTokenRepository.findByUserAndUsedAtIsNull(user)).thenReturn(List.of());

        authService.requestPasswordReset("user@luxestay.com");

        verify(passwordResetTokenRepository, times(1)).save(any(PasswordResetToken.class));
        verify(eventPublisher, times(1)).publishPasswordReset(any());
    }

    @Test
    void requestPasswordResetSilentWhenUserNotFound() {
        when(userRepository.findByEmail("missing@luxestay.com")).thenReturn(Optional.empty());

        authService.requestPasswordReset("missing@luxestay.com");

        verify(passwordResetTokenRepository, never()).save(any());
        verify(eventPublisher, never()).publishPasswordReset(any());
    }

    @Test
    void requestPasswordResetThrowsValidationExceptionWhenTooManyAttempts() {
        when(userRepository.findByEmail("user@luxestay.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.countByUserAndCreatedAtAfter(eq(user), any())).thenReturn(3L);

        assertThatThrownBy(() -> authService.requestPasswordReset("user@luxestay.com"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Demasiados intentos");
    }

    @Test
    void requestPasswordResetInvalidatesActiveTokensBeforeIssuingNew() {
        PasswordResetToken active = new PasswordResetToken();
        active.setId(7L);
        active.setUser(user);

        when(userRepository.findByEmail("user@luxestay.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.countByUserAndCreatedAtAfter(eq(user), any())).thenReturn(1L);
        when(passwordResetTokenRepository.findByUserAndUsedAtIsNull(user)).thenReturn(List.of(active));

        authService.requestPasswordReset("user@luxestay.com");

        verify(passwordResetTokenRepository, times(2)).save(any(PasswordResetToken.class));
        assertThat(active.getUsedAt()).isNotNull();
    }

    // ==================== verifyPasswordResetCode ====================

    @Test
    void verifyPasswordResetCodeSuccess() {
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setCode("123456");
        token.setExpiresAt(LocalDateTime.now().plusMinutes(10));

        when(userRepository.findByEmail("user@luxestay.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findByUserAndCodeAndUsedAtIsNull(user, "123456"))
                .thenReturn(Optional.of(token));

        authService.verifyPasswordResetCode("user@luxestay.com", "123456");

        // No exception expected
    }

    @Test
    void verifyPasswordResetCodeThrowsWhenUserNotFound() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.verifyPasswordResetCode("missing@luxestay.com", "123456"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void verifyPasswordResetCodeThrowsWhenCodeInvalid() {
        when(userRepository.findByEmail("user@luxestay.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findByUserAndCodeAndUsedAtIsNull(user, "wrong"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.verifyPasswordResetCode("user@luxestay.com", "wrong"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Código inválido");
    }

    @Test
    void verifyPasswordResetCodeThrowsWhenCodeExpired() {
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setCode("123456");
        token.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(userRepository.findByEmail("user@luxestay.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findByUserAndCodeAndUsedAtIsNull(user, "123456"))
                .thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.verifyPasswordResetCode("user@luxestay.com", "123456"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("expirado");
    }

    // ==================== resetPassword ====================

    @Test
    void resetPasswordSuccess() {
        PasswordResetToken token = new PasswordResetToken();
        token.setId(7L);
        token.setUser(user);
        token.setCode("123456");
        token.setExpiresAt(LocalDateTime.now().plusMinutes(5));

        when(userRepository.findByEmail("user@luxestay.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findByUserAndCodeAndUsedAtIsNull(user, "123456"))
                .thenReturn(Optional.of(token));
        when(passwordEncoder.encode("NewPassword123")).thenReturn("new-hashed");

        authService.resetPassword("user@luxestay.com", "123456", "NewPassword123");

        assertThat(user.getPassword()).isEqualTo("new-hashed");
        verify(userRepository).save(user);
        verify(passwordResetTokenRepository).delete(token);
    }

    @Test
    void resetPasswordThrowsWhenUserNotFound() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.resetPassword("missing@luxestay.com", "123456", "any"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void resetPasswordThrowsWhenCodeInvalid() {
        when(userRepository.findByEmail("user@luxestay.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findByUserAndCodeAndUsedAtIsNull(user, "wrong"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.resetPassword("user@luxestay.com", "wrong", "any"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Código inválido");
    }

    @Test
    void resetPasswordThrowsWhenCodeExpired() {
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setCode("123456");
        token.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(userRepository.findByEmail("user@luxestay.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findByUserAndCodeAndUsedAtIsNull(user, "123456"))
                .thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.resetPassword("user@luxestay.com", "123456", "any"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("expirado");
    }
}
