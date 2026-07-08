package com.kanban.kanbanapp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kanban.kanbanapp.Data_Transfer_Object.ErrorResponse;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;
import org.springframework.lang.NonNull;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitConfig rateLimitConfig;
    private final ObjectMapper objectMapper;

    public RateLimitInterceptor(RateLimitConfig rateLimitConfig, ObjectMapper objectMapper) {
        this.rateLimitConfig = rateLimitConfig;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        String uri = request.getRequestURI();
        String clientId = getClientId(request);
        
        RateLimitConfig.RateLimitType rateLimitType = getRateLimitType(uri);
        if (rateLimitType == null) {
            return true; // No rate limiting for this endpoint
        }

        String key = clientId + ":" + uri;
        Bucket bucket = rateLimitConfig.resolveBucket(key, rateLimitType);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            return true;
        }

        long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;
        
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefill));

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .error("Too Many Requests")
                .message("Rate limit exceeded. Try again in " + waitForRefill + " seconds.")
                .path(request.getRequestURI())
                .build();

        objectMapper.findAndRegisterModules();
        objectMapper.writeValue(response.getOutputStream(), error);
        
        return false;
    }

    private String getClientId(HttpServletRequest request) {
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getRemoteAddr();
        }
        return clientIp;
    }

    private RateLimitConfig.RateLimitType getRateLimitType(String uri) {
        if (uri.endsWith("/auth/login")) {
            return RateLimitConfig.RateLimitType.LOGIN;
        } else if (uri.endsWith("/auth/register")) {
            return RateLimitConfig.RateLimitType.REGISTER;
        } else if (uri.endsWith("/auth/refresh")) {
            return RateLimitConfig.RateLimitType.REFRESH;
        }
        return null;
    }
}