package com.kanban.kanbanapp.controller;

import com.kanban.kanbanapp.Data_Transfer_Object.*;
import com.kanban.kanbanapp.Data_Transfer_Object.PasswordManagement.ChangePasswordRequest;
import com.kanban.kanbanapp.Model.RefreshToken;
import com.kanban.kanbanapp.Model.User;
import com.kanban.kanbanapp.repository.UserRepository;
import com.kanban.kanbanapp.service.auth.JwtService;
import com.kanban.kanbanapp.service.auth.RefreshTokenService;
import com.kanban.kanbanapp.service.auth.TokenBlacklistService;
import com.kanban.kanbanapp.service.auth.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Authentication Controller", description = "APIs for user authentication and token management")
@CrossOrigin(origins = { "http://localhost:4200", "http://localhost:8081" })
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final PasswordEncoder passwordEncoder;

    private final TokenBlacklistService tokenBlacklistService;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private UserRepository userRepository;

    AuthController(TokenBlacklistService tokenBlacklistService, PasswordEncoder passwordEncoder) {
        this.tokenBlacklistService = tokenBlacklistService;
        this.passwordEncoder = passwordEncoder;
    }

    @Operation(summary = "User login", description = "Authenticate user and return JWT tokens")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        User user = userService.validateUser(request.getEmail(), request.getPassword());

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String accessToken = jwtService.generateAccessToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        AuthResponse response = new AuthResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken.getToken());
        response.setTokenType("Bearer");
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());

        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint pour l'inscription d'un nouvel utilisateur. Il reçoit les détails de
     * l'utilisateur, crée un compte et retourne les tokens JWT.
     * 
     * @param request
     * @return
     */
    @Operation(summary = "User registration", description = "Register new user and return JWT tokens")
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        User user = userService.registerUser(request);

        String accessToken = jwtService.generateAccessToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        AuthResponse response = new AuthResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken.getToken());
        response.setTokenType("Bearer");
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Refresh access token", description = "Generate new access token using refresh token")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshTokenRequest request) {
        return refreshTokenService.findByToken(request.getRefreshToken())
                .map(refreshTokenService::verifyExpiration)
                .map(oldToken -> {
                    User user = oldToken.getUser();

                    // ROTATION: Generate new tokens
                    String newAccessToken = jwtService.generateAccessToken(user);
                    RefreshToken newRefreshToken = refreshTokenService.rotateRefreshToken(oldToken);

                    AuthResponse response = new AuthResponse();
                    response.setAccessToken(newAccessToken);
                    response.setRefreshToken(newRefreshToken.getToken()); // Return NEW token
                    response.setTokenType("Bearer");
                    response.setUserId(user.getId());
                    response.setEmail(user.getEmail());

                    return ResponseEntity.ok(response);
                })
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));
    }

    @Operation(summary = "Revoke all tokens", description = "Revoke all access tokens for current user")
    @PostMapping("/revoke-all-tokens")
    public ResponseEntity<Void> revokeAllTokens() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Revoke all access tokens
        tokenBlacklistService.revokeAllUserTokens(user.getId());

        // Delete all refresh tokens
        refreshTokenService.deleteByUserId(user.getId());

        return ResponseEntity.ok().build();
    }

    @Operation(summary = "User logout", description = "Invalidate user's refresh tokens")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        refreshTokenService.deleteByUserId(user.getId());

        return ResponseEntity.ok().build();
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // IMPORTANT: Revoke all existing tokens
        tokenBlacklistService.revokeAllUserTokens(user.getId());
        refreshTokenService.deleteByUserId(user.getId());

        return ResponseEntity.ok().build();
    }

    @Service
    public class SecurityAuditService {
        public void logLogin(String userId, boolean success, String ip) {
            // Log to database
        }

        public void logPasswordChange(String userId, String ip) {
        }

        public void logTokenRefresh(String userId, String ip) {
        }

        public void logAccountLocked(String userId, String reason) {
        }
    }
}