package com.kanban.kanbanapp.repository;

import com.kanban.kanbanapp.Model.RefreshToken;
import com.kanban.kanbanapp.Model.User;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.time.Instant;
import java.util.List;
import java.util.Optional;


public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

    /**
     * Find a refresh token by its token string.
     *
     * @param token the token string
     * @return an Optional containing the token if found
     */
   Optional<RefreshToken> findByToken(String token);

    /**
     * Delete all refresh tokens belonging to a user.
     *
     * @param user the user
     */
    @Modifying
    @Transactional
    void deleteByUser(User user);

    /**
     * Delete all refresh tokens that expired before the given instant.
     *
     * @param date the cutoff instant
     * @return the number of deleted tokens
     */
    @Modifying
    @Transactional
    int deleteByExpiryDateBefore(Instant date);

    /**
     * Find all tokens belonging to a token family.
     *
     * @param tokenFamily the token family id
     * @return the list of tokens in that family
     */
    List<RefreshToken> findByTokenFamily(String tokenFamily);

    /**
     * Delete all tokens belonging to a token family.
     * Used to terminate every session in the family when token reuse is
     * detected.
     *
     * @param tokenFamily the token family id
     */
    @Modifying
    @Transactional
    void deleteByTokenFamily(String tokenFamily);

    /**
     * Find all refresh tokens belonging to a user.
     *
     * @param userId the user id
     * @return the list of the user's refresh tokens
     */
    List<RefreshToken> findByUserId(String userId);
}