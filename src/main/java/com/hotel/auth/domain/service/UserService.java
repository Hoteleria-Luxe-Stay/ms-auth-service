package com.hotel.auth.domain.service;

import com.hotel.auth.api.dto.UserResponse;
import com.hotel.auth.domain.model.User;

import java.util.List;
import java.util.Optional;

public interface UserService {

    List<UserResponse> findAll();

    User findById(Long id);

    UserResponse save(User user);

    void deleteById(Long id);

    Optional<User> findByEmail(String email);
}
