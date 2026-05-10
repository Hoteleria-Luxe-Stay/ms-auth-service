package com.hotel.auth.application.mapper;

import com.hotel.auth.api.dto.AuthResponse;
import com.hotel.auth.api.dto.LoginRequest;
import com.hotel.auth.api.dto.RegisterRequest;
import com.hotel.auth.api.dto.TokenValidationResponse;
import com.hotel.auth.api.dto.UserResponse;
import com.hotel.auth.domain.model.Role;
import com.hotel.auth.domain.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthMapperTest {

    @Test
    void privateConstructorThrowsUnsupportedOperationException() throws NoSuchMethodException {
        Constructor<AuthMapper> constructor = AuthMapper.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        assertThatThrownBy(constructor::newInstance)
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void fromDtoRegisterRequestMapsEmailAndUsername() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("test@luxestay.com");
        req.setUsername("testuser");

        User user = AuthMapper.fromDto(req);

        assertThat(user.getEmail()).isEqualTo("test@luxestay.com");
        assertThat(user.getNombre()).isEqualTo("testuser");
    }

    @Test
    void fromDtoLoginRequestReturnsUsernamePasswordAuthenticationToken() {
        LoginRequest req = new LoginRequest();
        req.setEmail("test@luxestay.com");
        req.setPassword("Password123");

        Authentication auth = AuthMapper.fromDto(req);

        assertThat(auth).isInstanceOf(UsernamePasswordAuthenticationToken.class);
        assertThat(auth.getPrincipal()).isEqualTo("test@luxestay.com");
        assertThat(auth.getCredentials()).isEqualTo("Password123");
    }

    @Test
    void toUserResponseMapsAllFieldsWhenComplete() {
        Role role = new Role();
        role.setRolename("USER");
        User user = User.builder()
                .id(1L)
                .username("nameuser")
                .email("user@luxestay.com")
                .telefono("+5491100000000")
                .activo(true)
                .role(role)
                .fechaCreacion(LocalDateTime.of(2026, 1, 1, 12, 0))
                .build();

        UserResponse resp = AuthMapper.toUserResponse(user);

        assertThat(resp.getId()).isEqualTo(1L);
        assertThat(resp.getUsername()).isEqualTo("nameuser");
        assertThat(resp.getEmail()).isEqualTo("user@luxestay.com");
        assertThat(resp.getTelefono()).isEqualTo("+5491100000000");
        assertThat(resp.getActivo()).isTrue();
        assertThat(resp.getFechaCreacion()).isNotNull();
        assertThat(resp.getRole()).isNotNull();
    }

    @Test
    void toUserResponseHandlesNullFechaCreacion() {
        Role role = new Role();
        role.setRolename("USER");
        User user = User.builder()
                .id(1L)
                .username("name")
                .email("e@e.com")
                .role(role)
                .activo(false)
                .fechaCreacion(null)
                .build();

        UserResponse resp = AuthMapper.toUserResponse(user);

        assertThat(resp.getFechaCreacion()).isNull();
        assertThat(resp.getActivo()).isFalse();
    }

    @Test
    void toUserResponseHandlesNullActivoAsFalse() {
        Role role = new Role();
        role.setRolename("USER");
        User user = User.builder()
                .id(1L)
                .username("name")
                .email("e@e.com")
                .role(role)
                .activo(null)
                .build();

        UserResponse resp = AuthMapper.toUserResponse(user);

        assertThat(resp.getActivo()).isFalse();
    }

    @Test
    void toUserResponseHandlesNullRole() {
        User user = User.builder()
                .id(1L)
                .username("name")
                .email("e@e.com")
                .role(null)
                .activo(true)
                .build();

        UserResponse resp = AuthMapper.toUserResponse(user);

        assertThat(resp.getRole()).isNull();
    }

    @Test
    void toAuthResponseMapsAllFields() {
        Role role = new Role();
        role.setRolename("USER");
        User user = User.builder()
                .id(1L)
                .username("name")
                .email("e@e.com")
                .role(role)
                .activo(true)
                .build();

        AuthResponse resp = AuthMapper.toAuthResponse(user, "access-token-x", "refresh-token-y", 3600);

        assertThat(resp.getAccessToken()).isEqualTo("access-token-x");
        assertThat(resp.getRefreshToken()).isEqualTo("refresh-token-y");
        assertThat(resp.getTokenType()).isEqualTo("Bearer");
        assertThat(resp.getExpiresIn()).isEqualTo(3600);
        assertThat(resp.getUser()).isNotNull();
        assertThat(resp.getUser().getEmail()).isEqualTo("e@e.com");
    }

    @Test
    void toTokenValidationResponseMapsAllFields() {
        Role role = new Role();
        role.setRolename("ADMIN");
        User user = User.builder()
                .id(7L)
                .username("admin")
                .email("admin@luxestay.com")
                .role(role)
                .activo(true)
                .build();

        TokenValidationResponse resp = AuthMapper.toTokenValidationResponse(user, true);

        assertThat(resp.getValid()).isTrue();
        assertThat(resp.getUserId()).isEqualTo(7L);
        assertThat(resp.getEmail()).isEqualTo("admin@luxestay.com");
        assertThat(resp.getRole()).isEqualTo("ADMIN");
    }

    @Test
    void toTokenValidationResponseHandlesNullRole() {
        User user = User.builder()
                .id(7L)
                .username("admin")
                .email("admin@luxestay.com")
                .role(null)
                .activo(true)
                .build();

        TokenValidationResponse resp = AuthMapper.toTokenValidationResponse(user, false);

        assertThat(resp.getRole()).isNull();
        assertThat(resp.getValid()).isFalse();
    }
}
