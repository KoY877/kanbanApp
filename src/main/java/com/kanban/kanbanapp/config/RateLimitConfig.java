package com.kanban.kanbanapp.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitConfig {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    public Bucket resolveBucket(String key, RateLimitType type) {
        return cache.computeIfAbsent(key, k -> createBucket(type));
    }

    private Bucket createBucket(RateLimitType type) {
        Bandwidth limit = switch (type) {
            case LOGIN -> Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1)));
            case REGISTER -> Bandwidth.classic(3, Refill.intervally(3, Duration.ofMinutes(10)));
            case REFRESH -> Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(1)));
        };
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    public enum RateLimitType {
        LOGIN,      // 5 requests per minute
        REGISTER,   // 3 requests per 10 minutes
        REFRESH     // 10 requests per minute
    }
}