package com.kanban.kanbanapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "jwt")
@Data
public class JwtProperties {
    
    private String secret;
    private AccessToken accessToken = new AccessToken();
    private RefreshToken refreshToken = new RefreshToken();
    
    @Data
    public static class AccessToken {
        private Long expiration = 900000L; // 15 minutes by default
    }
    
    @Data
    public static class RefreshToken {
        private Long expiration = 604800000L; // 7 days by default
    }
}