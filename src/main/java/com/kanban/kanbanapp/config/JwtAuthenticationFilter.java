package com.kanban.kanbanapp.config;

import com.kanban.kanbanapp.service.auth.JwtService;
import com.kanban.kanbanapp.service.auth.TokenBlacklistService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import org.springframework.lang.NonNull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenBlacklistService tokenBlacklistService;

    @Autowired
    private JwtService jwtService;

    JwtAuthenticationFilter(TokenBlacklistService tokenBlacklistService) {
        this.tokenBlacklistService = tokenBlacklistService;
    }

     @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/auth/") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/v3/api-docs") ||
               path.equals("/swagger-ui.html") ||
               path.startsWith("/swagger-resources") ||
               path.startsWith("/webjars/") || 
               path.contains("/api-docs");
    }


    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        
        String authHeader = request.getHeader("Authorization");
        
        // Skip filtering if no Authorization header or doesn't start with Bearer
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = authHeader.substring(7);
            String email = jwtService.extractEmail(token);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // CHECK BLACKLIST
                if (tokenBlacklistService.isBlacklisted(token)) {
                    logger.warn("Token is blacklisted");
                    filterChain.doFilter(request, response);
                    return;
                }
                
                // Reste du code...
                String userId = jwtService.extractUserId(token);
                Date issuedAt = jwtService.extractIssuedAt(token);
                if (tokenBlacklistService.areUserTokensRevoked(userId, issuedAt)) {
                    logger.warn("All user tokens revoked");
                    filterChain.doFilter(request, response);
                    return;
                }

                // Validate token
                if (jwtService.validateToken(token, email)) {
                    // Extract roles from JWT claims
                    @SuppressWarnings("unchecked")
                    List<String> roles = jwtService.extractClaim(token, claims -> claims.get("roles", List.class));
                    
                    List<SimpleGrantedAuthority> authorities = roles.stream()
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                            .collect(Collectors.toList());

                    UsernamePasswordAuthenticationToken authToken = 
                        new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            authorities
                        );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (ExpiredJwtException e) {
             logger.error("JWT expired: " + e.getMessage());
        } catch (MalformedJwtException e) {
            logger.error("Malformed JWT: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Cannot set user authentication: " + e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}