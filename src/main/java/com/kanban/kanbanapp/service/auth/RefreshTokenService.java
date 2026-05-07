package com.kanban.kanbanapp.service.auth;

import com.kanban.kanbanapp.Model.RefreshToken;
import com.kanban.kanbanapp.Model.User;
import com.kanban.kanbanapp.repository.RefreshTokenRepository;
import com.kanban.kanbanapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
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
        String validUserId = java.util.Objects.requireNonNull(userId, "User ID cannot be null");
        User user = userRepository.findById(validUserId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + validUserId));

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setTokenFamily(UUID.randomUUID().toString()); // Créer nouvelle famille
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

        // Verify if the token is revoked
        if (token.isRevoked()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token has been revoked");
        }

        // Check if token is expired
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired. Please login again.");
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
        String validUserId = java.util.Objects.requireNonNull(userId, "User ID cannot be null");
        User user = userRepository.findById(validUserId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + validUserId));
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
     * Detects token reuse attacks with grace period for race conditions
     * 
     * @param oldToken the old refresh token to rotate
     * @return the new refresh token
     * @throws SecurityException if token reuse detected
     */
    @Transactional
    public RefreshToken rotateRefreshToken(RefreshToken oldToken) {
        // Verify if the token is already revoked
        if (oldToken.isRevoked()) {
            // Check if this is a race condition or a real attack

            Instant revokedAt = oldToken.getRevokedAt();
            if (revokedAt != null) {
                long secondsSinceRevocation = Instant.now().getEpochSecond() - revokedAt.getEpochSecond();

                // GRACE PERIOD: If revoked less than 30 seconds ago, this is probably
                // a race condition
                if (secondsSinceRevocation < 30) {
                    System.out.println(
                            "Token already revoked " + secondsSinceRevocation + "s ago - probable race condition");
                    System.out.println("   → Rejet de la tentative SANS détruire la famille");
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                            "Token already used - please retry with the new token");
                }

                // ATTACK DETECTED: Token revoked long ago = real attack
                System.err.println("TOKEN REUSE ATTACK DETECTED!");
                System.err.println("   Token revoked " + secondsSinceRevocation + "s ago");
                System.err.println("   → Révocation de toute la famille: " + oldToken.getTokenFamily());
                revokeTokenFamily(oldToken.getTokenFamily());
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Token reuse attack detected - all sessions have been terminated for security");
            }

            // Si revokedAt est null (ne devrait pas arriver), rejeter sans détruire
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token already revoked");
        }

        // Valid token, proceed with normal rotation
        System.out.println("Token rotation for user: " + oldToken.getUser().getEmail());

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

        RefreshToken savedToken = refreshTokenRepository.save(newToken);
        System.out.println("New token created in family: " + oldToken.getTokenFamily());

        return savedToken;
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

    public void revokeAllByUserId(@NonNull String userId) {
        List<RefreshToken> tokens = refreshTokenRepository.findByUserId(userId);
        for (RefreshToken token : tokens) {
            token.setRevoked(true);
            token.setRevokedAt(Instant.now());
        }

        refreshTokenRepository.saveAll(java.util.Objects.requireNonNull(tokens));
    }
}