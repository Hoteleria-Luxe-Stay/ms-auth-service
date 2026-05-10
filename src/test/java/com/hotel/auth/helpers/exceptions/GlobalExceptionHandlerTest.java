package com.hotel.auth.helpers.exceptions;

import com.hotel.auth.api.dto.ErrorResponse;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        when(request.getRequestURI()).thenReturn("/api/v1/test");
    }

    @Test
    void handleEntityNotFoundExceptionReturns404() {
        EntityNotFoundException ex = new EntityNotFoundException("User", 1L);

        ResponseEntity<ErrorResponse> response = handler.handleEntityNotFoundException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getError()).isEqualTo("Not Found");
        assertThat(response.getBody().getPath()).isEqualTo("/api/v1/test");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    void handleBusinessExceptionReturns400WithErrorCode() {
        BusinessException ex = new BusinessException("invalid input", "CODE_001");

        ResponseEntity<ErrorResponse> response = handler.handleBusinessException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getError()).isEqualTo("CODE_001");
        assertThat(response.getBody().getMessage()).isEqualTo("invalid input");
    }

    @Test
    void handleValidationExceptionReturns400() {
        ValidationException ex = new ValidationException("email", "invalid format");

        ResponseEntity<ErrorResponse> response = handler.handleValidationException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getError()).isEqualTo("Validation Error");
    }

    @Test
    void handleConflictExceptionReturns409WithConflictType() {
        ConflictException ex = new ConflictException("email duplicado", "EMAIL_DUPLICATE");

        ResponseEntity<ErrorResponse> response = handler.handleConflictException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getError()).isEqualTo("EMAIL_DUPLICATE");
    }

    @Test
    void handleJwtExceptionReturns401() {
        JwtException ex = new JwtException("invalid signature");

        ResponseEntity<ErrorResponse> response = handler.handleJwtException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getError()).isEqualTo("Invalid Token");
    }

    @Test
    void handleTokenExpiredExceptionReturns401() {
        TokenExpiredException ex = new TokenExpiredException("token expired");

        ResponseEntity<ErrorResponse> response = handler.handleTokenExpiredException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getError()).isEqualTo("Token Expired");
    }

    @Test
    void handleBadCredentialsExceptionReturns401() {
        BadCredentialsException ex = new BadCredentialsException("invalid");

        ResponseEntity<ErrorResponse> response = handler.handleBadCredentialsException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getError()).isEqualTo("Bad Credentials");
    }

    @Test
    void handleMethodArgumentNotValidExceptionReturns400() throws NoSuchMethodException {
        Method method = DummyTarget.class.getMethod("dummy", String.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "email", "must not be blank"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleMethodArgumentNotValid(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getError()).isEqualTo("Validation Failed");
    }

    @Test
    void handleRuntimeExceptionReturns500() {
        RuntimeException ex = new RuntimeException("boom");

        ResponseEntity<ErrorResponse> response = handler.handleRuntimeException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().getMessage()).isEqualTo("boom");
    }

    @Test
    void handleGenericExceptionReturns500() {
        Exception ex = new Exception("oops");

        ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().getMessage()).contains("inesperado");
    }

    private static class DummyTarget {
        public void dummy(@Autowired String arg) { /* no-op */ }
    }
}
