package com.kanban.kanbanapp.service.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;

@Service
public class TokenBlacklistService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Autowired
    private JwtService jwtService;
    
    @Value("${jwt.access-token.expiration}")
    private Long accessTokenExpiration;
    
    private static final String BLACKLIST_PREFIX = "blacklist:";
    
    /**
     * Add a token to the blacklist
     */
    public void blacklistToken(String token) {
        String jti = jwtService.extractJti(token);
        Date expiration = jwtService.extractExpiration(token);
        long ttl = expiration.getTime() - System.currentTimeMillis();
        
        if (ttl > 0) {
            redisTemplate.opsForValue().set(
                BLACKLIST_PREFIX + jti, 
                "revoked", 
                Duration.ofMillis(ttl)
            );
        }
    }
    
    /**
     * Check if a token is blacklisted
     */
    public boolean isBlacklisted(String token) {
        String jti = jwtService.extractJti(token);
        return Boolean.TRUE.equals(
            redisTemplate.hasKey(BLACKLIST_PREFIX + jti)
        );
    }
    
    /**
     * Revoke all tokens for a user (e.g., on password change)
     */
    public void revokeAllUserTokens(String userId) {
        // Store user ID with TTL equal to max token lifetime
        redisTemplate.opsForValue().set(
            "revoked_user:" + userId,
            String.valueOf(System.currentTimeMillis()),
            Duration.ofMillis(accessTokenExpiration)
        );
    }
    
    /**
     * Check if all user tokens are revoked
     */
    public boolean areUserTokensRevoked(String userId, Date tokenIssuedAt) {
        String revokedTime = redisTemplate.opsForValue()
            .get("revoked_user:" + userId);
        
        if (revokedTime == null) return false;
        
        long revokeTimestamp = Long.parseLong(revokedTime);
        return tokenIssuedAt.getTime() < revokeTimestamp;
    }
}