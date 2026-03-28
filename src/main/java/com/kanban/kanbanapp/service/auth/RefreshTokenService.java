package com.kanban.kanbanapp.service.auth;

import com.kanban.kanbanapp.Model.RefreshToken;
import com.kanban.kanbanapp.Model.User;
import com.kanban.kanbanapp.repository.RefreshTokenRepository;
import com.kanban.kanbanapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    @Value("${jwt.refresh-token.expiration}")
    private Long refreshTokenDuration;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Create a new refresh token for a user
     * This creates a NEW token family
     * 
     * @param userId the user ID (as String)
     * @return the created RefreshToken
     */
    public RefreshToken createRefreshToken(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setTokenFamily(UUID.randomUUID().toString()); // ✅ Créer nouvelle famille
        refreshToken.setRevoked(false);
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDuration));
        refreshToken.setCreatedDate(Instant.now());

        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Find a refresh token by its token string
     * 
     * @param token the token string
     * @return Optional containing the RefreshToken if found
     */
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    /**
     * Verify if a refresh token is expired
     * If expired, delete it from database
     * 
     * @param token the RefreshToken to verify
     * @return the same token if valid
     * @throws RuntimeException if token is expired
     */
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh token expired. Please login again.");
        }
        
        // Verify if the token is revoked
        if (token.isRevoked()) {
            throw new SecurityException("Token has been revoked");
        }
        
        return token;
    }

    /**
     * Delete all refresh tokens for a specific user
     * Used during logout to invalidate all sessions
     * 
     * @param userId the user ID (as String)
     */
    @Transactional
    public void deleteByUserId(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        refreshTokenRepository.deleteByUser(user);
    }

    /**
     * Scheduled task to cleanup expired refresh tokens
     * Runs daily at 2:00 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        int deletedCount = refreshTokenRepository.deleteByExpiryDateBefore(Instant.now());
        System.out.println("Cleaned up " + deletedCount + " expired refresh tokens");
    }

    /**
     * Rotate refresh token with token family tracking
     * Detects token reuse attacks
     * 
     * @param oldToken the old refresh token to rotate
     * @return the new refresh token
     * @throws SecurityException if token reuse detected
     */
    @Transactional
    public RefreshToken rotateRefreshToken(RefreshToken oldToken) {
        // ✅ Verify if the token is already revoked (reuse detected!)
        if (oldToken.isRevoked()) {
            // ATTACK DETECTED: someone is reusing an old token
            System.err.println("⚠️ TOKEN REUSE DETECTED! Revoking entire token family: " + oldToken.getTokenFamily());
            revokeTokenFamily(oldToken.getTokenFamily());
            throw new SecurityException("Token reuse detected - all tokens in family have been revoked for security");
        }
        
        // Mark the old token as revoked
        oldToken.setRevoked(true);
        oldToken.setRevokedAt(Instant.now());
        refreshTokenRepository.save(oldToken);
        
        // Create new token with the SAME family
        User user = oldToken.getUser();
        RefreshToken newToken = new RefreshToken();
        newToken.setUser(user);
        newToken.setToken(UUID.randomUUID().toString());
        newToken.setTokenFamily(oldToken.getTokenFamily()); // Keep the same family!
        newToken.setRevoked(false);
        newToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDuration));
        newToken.setCreatedDate(Instant.now());
        
        return refreshTokenRepository.save(newToken);
    }
    
    /**
     * Revoke all tokens in a token family
     * Used when token theft is detected
     * 
     * @param tokenFamily the token family ID to revoke
     */
    @Transactional
    public void revokeTokenFamily(String tokenFamily) {
        // Simple implementation: delete all tokens in the family
        refreshTokenRepository.deleteByTokenFamily(tokenFamily);
        System.out.println("🔒 Revoked all tokens in family: " + tokenFamily);
    }
}