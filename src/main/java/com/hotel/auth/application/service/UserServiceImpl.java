package com.hotel.auth.application.service;

import com.hotel.auth.application.mapper.AuthMapper;
import com.hotel.auth.api.dto.UserResponse;
import com.hotel.auth.domain.model.User;
import com.hotel.auth.domain.repository.UserRepository;
import com.hotel.auth.domain.service.UserService;
import com.hotel.auth.helpers.exceptions.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream()
                .map(AuthMapper::toUserResponse)
                .toList();
    }

    @Override
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User", id));
    }

    @Override
    public UserResponse save(User user) {
        User saved = userRepository.save(user);
        return AuthMapper.toUserResponse(saved);
    }

    @Override
    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}
