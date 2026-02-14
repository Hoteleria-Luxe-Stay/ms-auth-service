package com.hotel.auth.application.service;

import com.hotel.auth.application.mapper.AuthMapper;
import com.hotel.auth.api.dto.LoginRequest;
import com.hotel.auth.api.dto.RegisterRequest;
import com.hotel.auth.domain.model.Role;
import com.hotel.auth.domain.model.User;
import com.hotel.auth.domain.model.PasswordResetToken;
import com.hotel.auth.domain.repository.PasswordResetTokenRepository;
import com.hotel.auth.domain.repository.RoleRepository;
import com.hotel.auth.domain.repository.UserRepository;
import com.hotel.auth.domain.service.AuthService;
import com.hotel.auth.domain.service.TokenService;
import com.hotel.auth.helpers.exceptions.ConflictException;
import com.hotel.auth.helpers.exceptions.EntityNotFoundException;
import com.hotel.auth.helpers.exceptions.ValidationException;
import com.hotel.auth.infrastructure.events.EventPublisher;
import com.hotel.auth.infrastructure.events.PasswordResetEvent;
import com.hotel.auth.infrastructure.events.UserLoginEvent;
import com.hotel.auth.infrastructure.events.UserRegisteredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;
import java.time.LocalDateTime;
import java.security.SecureRandom;

@Service
public class AuthServiceImpl implements AuthService, UserDetailsService {

    private static final String DEFAULT_ROLE = "USER";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationConfiguration authenticationConfiguration;
    private final RoleRepository roleRepository;
    private final EventPublisher eventPublisher;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Value("${application.security.password-reset-expiration-minutes:30}")
    private int passwordResetExpirationMinutes;

    @Value("${application.security.password-reset-max-attempts:3}")
    private int passwordResetMaxAttempts;

    @Value("${application.security.password-reset-block-minutes:15}")
    private int passwordResetBlockMinutes;

    public AuthServiceImpl(UserRepository userRepository,
                           TokenService tokenService,
                           PasswordEncoder passwordEncoder,
                           AuthenticationConfiguration authenticationConfiguration,
                           RoleRepository roleRepository,
                           EventPublisher eventPublisher,
                           PasswordResetTokenRepository passwordResetTokenRepository) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationConfiguration = authenticationConfiguration;
        this.roleRepository = roleRepository;
        this.eventPublisher = eventPublisher;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    @Override
    public Map<String, String> login(LoginRequest loginRequest) {
        try {
            AuthenticationManager authenticationManager = authenticationConfiguration.getAuthenticationManager();
            Authentication authRequest = AuthMapper.fromDto(loginRequest);
            Authentication authentication = authenticationManager.authenticate(authRequest);

            User user = (User) authentication.getPrincipal();
            String token = tokenService.generateToken(authentication);

            UserLoginEvent loginEvent = new UserLoginEvent(
                    user.getId(),
                    user.getNombre(),
                    user.getEmail(),
                    user.getRole().getRolename()
            );
            eventPublisher.publishUserLogin(loginEvent);

            return Map.of("access-token", token);
        } catch (BadCredentialsException e) {
            LOGGER.error("ERROR: Credenciales incorrectas para: {}", loginRequest.getEmail());
            throw new BadCredentialsException("Credenciales inválidas", e);
        } catch (UsernameNotFoundException e) {
            LOGGER.error("ERROR: Usuario no encontrado: {}", loginRequest.getEmail());
            throw new UsernameNotFoundException("Usuario no encontrado", e);
        } catch (DisabledException e) {
            LOGGER.error("ERROR: Usuario deshabilitado: {}", loginRequest.getEmail());
            throw new DisabledException("Usuario deshabilitado", e);
        } catch (Exception e) {
            LOGGER.error("ERROR INESPERADO durante login: {}", e.getMessage(), e);
            throw new BadCredentialsException("Error en autenticación", e);
        }
    }

    @Override
    public boolean validateToken(String token) {
        return tokenService.validateToken(token);
    }

    @Override
    public String getUserFromToken(String token) {
        return tokenService.getUserFromToken(token);
    }

    @Override
    public void createUser(RegisterRequest registerRequest) {
        if (userRepository.findByEmail(registerRequest.getEmail()).isPresent()) {
            throw new ConflictException("Email ya registrado", "EMAIL_DUPLICATE");
        }

        Role roleClient = roleRepository.findByRolename(DEFAULT_ROLE)
                .orElseThrow(() -> new EntityNotFoundException("Role", DEFAULT_ROLE));

        User createUser = AuthMapper.fromDto(registerRequest);
        createUser.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        createUser.setRole(roleClient);
        createUser.setTelefono(registerRequest.getTelefono());

        User user = userRepository.save(createUser);
        LOGGER.info("[USER] : User successfully created with id {}", user.getId());

        // Publicar evento de usuario registrado
        UserRegisteredEvent event = new UserRegisteredEvent(
                user.getId(),
                user.getNombre(),
                user.getEmail(),
                user.getRole().getRolename()
        );
        eventPublisher.publishUserRegistered(event);
    }

    @Override
    public User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> {
                    LOGGER.info("[USER] : User not found with id {}", id);
                    return new UsernameNotFoundException("User not found");
                });
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmail(username)
                .orElseThrow(() -> {
                    LOGGER.error("[USER] : User not found with email {}", username);
                    return new UsernameNotFoundException("User not found");
                });
    }

    @Override
    public void requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            LOGGER.info("[PASSWORD RESET] Email not found: {}", email);
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        long recentAttempts = passwordResetTokenRepository.countByUserAndCreatedAtAfter(
                user,
                now.minusMinutes(passwordResetBlockMinutes)
        );
        if (recentAttempts >= passwordResetMaxAttempts) {
            throw new ValidationException("email", "Demasiados intentos. Intenta nuevamente en 15 minutos");
        }

        passwordResetTokenRepository.findByUserAndUsedAtIsNull(user)
                .forEach(token -> {
                    token.setUsedAt(LocalDateTime.now());
                    passwordResetTokenRepository.save(token);
                });

        String code = generateCode();
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setCode(code);
        token.setCreatedAt(now);
        token.setExpiresAt(now.plusMinutes(passwordResetExpirationMinutes));
        passwordResetTokenRepository.save(token);

        PasswordResetEvent event = new PasswordResetEvent(
                user.getId(),
                user.getNombre(),
                user.getEmail(),
                code
        );
        eventPublisher.publishPasswordReset(event);
    }

    @Override
    public void verifyPasswordResetCode(String email, String code) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            throw new ValidationException("email", "Email inválido");
        }
        PasswordResetToken token = passwordResetTokenRepository.findByUserAndCodeAndUsedAtIsNull(user, code)
                .orElseThrow(() -> new ValidationException("code", "Código inválido"));
        if (token.getExpiresAt() != null && token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ValidationException("code", "Código expirado");
        }
    }

    @Override
    public void resetPassword(String email, String code, String newPassword) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            throw new ValidationException("email", "Email inválido");
        }
        PasswordResetToken token = passwordResetTokenRepository.findByUserAndCodeAndUsedAtIsNull(user, code)
                .orElseThrow(() -> new ValidationException("code", "Código inválido"));

        if (token.getExpiresAt() != null && token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ValidationException("code", "Código expirado");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        passwordResetTokenRepository.delete(token);
    }

    private String generateCode() {
        int code = SECURE_RANDOM.nextInt(1_000_000);
        return String.format("%06d", code);
    }
}
