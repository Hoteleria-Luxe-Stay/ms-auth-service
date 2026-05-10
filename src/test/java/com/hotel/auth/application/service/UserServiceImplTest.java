package com.hotel.auth.application.service;

import com.hotel.auth.api.dto.UserResponse;
import com.hotel.auth.domain.model.Role;
import com.hotel.auth.domain.model.User;
import com.hotel.auth.domain.repository.UserRepository;
import com.hotel.auth.helpers.exceptions.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;

    @BeforeEach
    void setUp() {
        Role role = new Role();
        role.setRolename("USER");
        user = User.builder()
                .id(1L)
                .username("name")
                .email("u@luxestay.com")
                .role(role)
                .activo(true)
                .build();
    }

    @Test
    void findAllReturnsListOfUserResponses() {
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<UserResponse> result = userService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("u@luxestay.com");
    }

    @Test
    void findAllReturnsEmptyListWhenNoUsers() {
        when(userRepository.findAll()).thenReturn(List.of());

        List<UserResponse> result = userService.findAll();

        assertThat(result).isEmpty();
    }

    @Test
    void findByIdReturnsUserWhenExists() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        User result = userService.findById(1L);

        assertThat(result).isEqualTo(user);
    }

    @Test
    void findByIdThrowsEntityNotFoundExceptionWhenMissing() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("User");
    }

    @Test
    void saveReturnsUserResponse() {
        when(userRepository.save(user)).thenReturn(user);

        UserResponse result = userService.save(user);

        assertThat(result.getEmail()).isEqualTo("u@luxestay.com");
        verify(userRepository).save(user);
    }

    @Test
    void deleteByIdInvokesRepositoryDelete() {
        userService.deleteById(1L);

        verify(userRepository, times(1)).deleteById(1L);
    }

    @Test
    void findByEmailReturnsOptionalUser() {
        when(userRepository.findByEmail("u@luxestay.com")).thenReturn(Optional.of(user));

        Optional<User> result = userService.findByEmail("u@luxestay.com");

        assertThat(result).isPresent().contains(user);
    }

    @Test
    void findByEmailReturnsEmptyOptionalWhenMissing() {
        when(userRepository.findByEmail("missing@luxestay.com")).thenReturn(Optional.empty());

        Optional<User> result = userService.findByEmail("missing@luxestay.com");

        assertThat(result).isEmpty();
    }
}
