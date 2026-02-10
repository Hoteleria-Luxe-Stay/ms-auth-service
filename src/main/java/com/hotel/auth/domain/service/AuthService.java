package com.hotel.auth.domain.service;

import com.hotel.auth.api.dto.LoginRequest;
import com.hotel.auth.api.dto.RegisterRequest;
import com.hotel.auth.domain.model.User;

import java.util.Map;

public interface AuthService {

    Map<String, String> login(LoginRequest loginRequest);

    boolean validateToken(String token);

    String getUserFromToken(String token);

    void createUser(RegisterRequest registerRequest);

    User getUser(Long id);

    void requestPasswordReset(String email);

    void verifyPasswordResetCode(String email, String code);

    void resetPassword(String email, String code, String newPassword);
}
