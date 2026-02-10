package com.hotel.auth.infrastructure.events;

import java.io.Serializable;

public class PasswordResetEvent implements Serializable {

    private Long userId;
    private String username;
    private String email;
    private String code;

    public PasswordResetEvent() {
    }

    public PasswordResetEvent(Long userId, String username, String email, String code) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.code = code;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
