package com.hotel.auth.infraestructure.controllers;

import com.hotel.auth.api.AuthApi;
import com.hotel.auth.api.UsersApi;
import com.hotel.auth.api.dto.AuthResponse;
import com.hotel.auth.api.dto.LoginRequest;
import com.hotel.auth.api.dto.RefreshTokenRequest;
import com.hotel.auth.api.dto.RegisterRequest;
import com.hotel.auth.api.dto.TokenValidationResponse;
import com.hotel.auth.api.dto.UserResponse;
import com.hotel.auth.api.dto.ValidateTokenRequest;
import com.hotel.auth.application.mapper.AuthMapper;
import com.hotel.auth.domain.model.User;
import com.hotel.auth.domain.service.AuthService;
import com.hotel.auth.domain.service.TokenService;
import com.hotel.auth.domain.service.UserService;
import com.hotel.auth.helpers.exceptions.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.Map;
import java.util.Optional;

@RestController
public class AuthController implements AuthApi, UsersApi {

    private final AuthService authService;
    private final TokenService tokenService;
    private final UserService userService;

    @Value("${application.security.jwt.expiration}")
    private int jwtExpiration;

    public AuthController(AuthService authService, TokenService tokenService, UserService userService) {
        this.authService = authService;
        this.tokenService = tokenService;
        this.userService = userService;
    }

    @Override
    public ResponseEntity<AuthResponse> register(RegisterRequest registerRequest) {
        authService.createUser(registerRequest);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(registerRequest.getEmail());
        loginRequest.setPassword(registerRequest.getPassword());

        Map<String, String> token = authService.login(loginRequest);
        User user = userService.findByEmail(registerRequest.getEmail()).orElseThrow(
                () -> new EntityNotFoundException("User", registerRequest.getEmail())
        );

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                user,
                null,
                user.getAuthorities()
        );
        String refreshToken = tokenService.generateRefreshToken(authentication);

        AuthResponse authResponse = AuthMapper.toAuthResponse(user, token.get("access-token"), refreshToken, jwtExpiration);
        return ResponseEntity.status(HttpStatus.CREATED).body(authResponse);
    }

    @Override
    public ResponseEntity<AuthResponse> login(LoginRequest loginRequest) {
        Map<String, String> token = authService.login(loginRequest);
        User user = userService.findByEmail(loginRequest.getEmail()).orElseThrow(
                () -> new EntityNotFoundException("User", loginRequest.getEmail())
        );

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                user,
                null,
                user.getAuthorities()
        );
        String refreshToken = tokenService.generateRefreshToken(authentication);

        AuthResponse authResponse = AuthMapper.toAuthResponse(user, token.get("access-token"), refreshToken, jwtExpiration);
        return ResponseEntity.ok(authResponse);
    }

    @Override
    public ResponseEntity<AuthResponse> refreshToken(RefreshTokenRequest refreshTokenRequest) {
        String refreshToken = refreshTokenRequest.getRefreshToken();
        authService.validateToken(refreshToken);
        String email = authService.getUserFromToken(refreshToken);

        User user = userService.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User", email));

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                user,
                null,
                user.getAuthorities()
        );

        String accessToken = tokenService.generateToken(authentication);
        String newRefreshToken = tokenService.generateRefreshToken(authentication);
        AuthResponse authResponse = AuthMapper.toAuthResponse(user, accessToken, newRefreshToken, jwtExpiration);
        return ResponseEntity.ok(authResponse);
    }

    @Override
    public ResponseEntity<TokenValidationResponse> validateToken(ValidateTokenRequest request) {
        authService.validateToken(request.getToken());
        String email = authService.getUserFromToken(request.getToken());

        User user = userService.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User", email));

        TokenValidationResponse response = AuthMapper.toTokenValidationResponse(user, true);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<UserResponse> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            throw new EntityNotFoundException("User", "current");
        }

        User user = (User) authentication.getPrincipal();
        User currentUser = userService.findByEmail(user.getEmail())
                .orElseThrow(() -> new EntityNotFoundException("User", user.getEmail()));

        UserResponse userResponse = AuthMapper.toUserResponse(currentUser);
        return ResponseEntity.ok(userResponse);
    }

    @Override
    public ResponseEntity<UserResponse> getUserById(Long id) {
        User user = authService.getUser(id);
        return ResponseEntity.ok(AuthMapper.toUserResponse(user));
    }

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return Optional.empty();
    }
}
