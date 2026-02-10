package com.hotel.auth.domain.repository;

import com.hotel.auth.domain.model.PasswordResetToken;
import com.hotel.auth.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByUserAndCodeAndUsedAtIsNull(User user, String code);
    List<PasswordResetToken> findByUserAndUsedAtIsNull(User user);
    long countByUserAndCreatedAtAfter(User user, LocalDateTime createdAt);
}
