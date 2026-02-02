package com.hotel.auth.infrastructure.events;

import java.io.Serializable;

public class UserLoginEvent implements Serializable {

    private Long userId;
    private String username;
    private String email;
    private String role;

    public UserLoginEvent() {
    }

    public UserLoginEvent(Long userId, String username, String email, String role) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.role = role;
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
