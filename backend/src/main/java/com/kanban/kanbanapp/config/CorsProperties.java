package com.kanban.kanbanapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * Bind CORS-related settings from the {@code cors.*} configuration prefix.
 */
@ConfigurationProperties(prefix = "cors")
@Data
public class CorsProperties {
    
    private String allowedOrigins = "http://localhost:4202,http://localhost:8084";
    private String allowedMethods = "GET,POST,PUT,DELETE,PATCH,OPTIONS";
    private String allowedHeaders = "*";
    private Boolean allowCredentials = true;
    private Integer maxAge = 3600;
}