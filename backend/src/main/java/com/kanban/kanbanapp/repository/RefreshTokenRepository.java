package com.kanban.kanbanapp.repository;

import com.kanban.kanbanapp.Model.RefreshToken;
import com.kanban.kanbanapp.Model.User;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    
   Optional<RefreshToken> findByToken(String token);
    
    @Modifying
    @Transactional
    void deleteByUser(User user);
    
    @Modifying
    @Transactional
    int deleteByExpiryDateBefore(Instant date);
    
   
    List<RefreshToken> findByTokenFamily(String tokenFamily);
    
    @Modifying
    @Transactional
    void deleteByTokenFamily(String tokenFamily);

    List<RefreshToken> findByUserId(String userId);
}