package com.kanban.kanbanapp.repository;

import com.kanban.kanbanapp.Model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, String> {
    
    Optional<PasswordResetToken> findByToken(String token);
    
    void deleteByUserId(String userId);
    
    Optional<PasswordResetToken> findByUserIdAndUsedFalse(String userId);
}