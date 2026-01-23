package com.hotel.auth.application.mapper;

import com.hotel.auth.api.dto.AuthResponse;
import com.hotel.auth.api.dto.LoginRequest;
import com.hotel.auth.api.dto.RegisterRequest;
import com.hotel.auth.api.dto.TokenValidationResponse;
import com.hotel.auth.api.dto.UserResponse;
import com.hotel.auth.domain.model.Role;
import com.hotel.auth.domain.model.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

public class AuthMapper {

    private AuthMapper() {
        throw new UnsupportedOperationException("This class should never be instantiated");
    }

    public static User fromDto(RegisterRequest registerRequest) {
        User user = new User();
        user.setEmail(registerRequest.getEmail());
        user.setUsername(registerRequest.getUsername());
        return user;
    }

    public static Authentication fromDto(LoginRequest loginRequest) {
        return new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword());
    }

    public static UserResponse toUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setTelefono(user.getTelefono());
        response.setActivo(Boolean.TRUE.equals(user.getActivo()));
        if (user.getFechaCreacion() != null) {
            response.setFechaCreacion(user.getFechaCreacion().atOffset(java.time.ZoneOffset.UTC));
        }
        response.setRole(resolveRole(user.getRole()));
        return response;
    }

    public static AuthResponse toAuthResponse(User user, String accessToken, String refreshToken, Integer expiresIn) {
        AuthResponse response = new AuthResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setTokenType("Bearer");
        response.setExpiresIn(expiresIn);
        response.setUser(toUserResponse(user));
        return response;
    }

    public static TokenValidationResponse toTokenValidationResponse(User user, boolean valid) {
        TokenValidationResponse response = new TokenValidationResponse();
        response.setValid(valid);
        response.setUserId(user.getId());
        response.setEmail(user.getEmail());
        response.setRole(roleName(user.getRole()));
        return response;
    }

    private static UserResponse.RoleEnum resolveRole(Role role) {
        if (role == null) {
            return null;
        }
        return UserResponse.RoleEnum.fromValue(role.getRolename());
    }

    private static String roleName(Role role) {
        if (role == null) {
            return null;
        }
        return role.getRolename();
    }
}
