package com.kanban.kanbanapp.service.auth;

import com.kanban.kanbanapp.Model.BlacklistedToken;
import com.kanban.kanbanapp.repository.BlacklistedTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
public class TokenBlacklistService {

    @Autowired
    private BlacklistedTokenRepository repository;

    @Autowired
    private JwtService jwtService;
    
    @Autowired
    private RefreshTokenService refreshTokenService;

    /**
     * Blacklist a single token by adding its JTI to the database
     */
    public void blacklistToken(String token) {
        String jti = jwtService.extractJti(token);
        Date expiration = jwtService.extractExpiration(token);

        BlacklistedToken bt = new BlacklistedToken();
        bt.setJti(jti);
        bt.setExpiryDate(expiration);

        repository.save(bt);
    }

    /**
     * Check if a token is blacklisted
     */
    public boolean isBlacklisted(String token) {
        String jti = jwtService.extractJti(token);
        return repository.existsByJti(jti);
    }
    
    /**
     * Revoke all tokens for a specific user
     * This marks all refresh tokens as revoked in the database
     */
    @Transactional
    public void revokeAllUserTokens(@NonNull String userId) {
        refreshTokenService.revokeAllByUserId(userId);
    }
    
    /**
     * Scheduled cleanup of expired blacklisted tokens
     * Runs daily at 3:00 AM
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupExpiredBlacklistedTokens() {
        repository.deleteByExpiryDateBefore(new Date());
        System.out.println("Cleaned up expired blacklisted tokens");
    }
}