package com.kanban.kanbanapp.repository;

import com.kanban.kanbanapp.Model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, String> {

    /**
     * Find a password reset token by its token string.
     *
     * @param token the token string
     * @return an Optional containing the token if found
     */
    Optional<PasswordResetToken> findByToken(String token);

    /**
     * Delete all password reset tokens belonging to a user.
     *
     * @param userId the user id
     */
    void deleteByUserId(String userId);

    /**
     * Find the active (not yet used) password reset token for a user.
     *
     * @param userId the user id
     * @return an Optional containing the unused token if found
     */
    Optional<PasswordResetToken> findByUserIdAndUsedFalse(String userId);
}