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
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;

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

    @Value("${app.security.cookie.secure:false}")
    private boolean isProduction;

    AuthController(TokenBlacklistService tokenBlacklistService, PasswordEncoder passwordEncoder) {
        this.tokenBlacklistService = tokenBlacklistService;
        this.passwordEncoder = passwordEncoder;
    }

    @Operation(summary = "User login", description = "Authenticate user and return JWT tokens")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        User user = userService.validateUser(request.getEmail(), request.getPassword());

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String accessToken = jwtService.generateAccessToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        // 🔐 SÉCURITÉ: Envoyer le refreshToken en httpOnly cookie
        ResponseCookie responseCookie = ResponseCookie.from("refreshToken", refreshToken.getToken())
                .httpOnly(true) // ✅ Protection XSS
                .secure(isProduction) // ✅ HTTPS en production
                .sameSite("Lax") // ✅ Protection CSRF
                .path("/") // ✅ Envoyé sur tous les endpoints
                .maxAge(7 * 24 * 60 * 60) // ✅ 7 jours
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, responseCookie.toString());

        // 📦 Retourner SEULEMENT l'accessToken dans le body (pas le refreshToken)
        AuthResponse authResponse = new AuthResponse();
        authResponse.setAccessToken(accessToken);
        authResponse.setRefreshToken(null); // ❌ Ne plus retourner dans le JSON
        authResponse.setTokenType("Bearer");
        authResponse.setUserId(user.getId());
        authResponse.setUsername(user.getUsername());
        authResponse.setEmail(user.getEmail());
        authResponse.setRole(user.getRole().name());

        return ResponseEntity.ok(authResponse);
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
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request, HttpServletResponse response) {
        User user = userService.registerUser(request);

        String accessToken = jwtService.generateAccessToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        // 🔐 SÉCURITÉ: Envoyer le refreshToken en httpOnly cookie
        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", refreshToken.getToken())
                .httpOnly(true)
                .secure(isProduction)
                .sameSite("Lax")
                .path("/")
                .maxAge(7 * 24 * 60 * 60)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

        AuthResponse authResponse = new AuthResponse();
        authResponse.setAccessToken(accessToken);
        authResponse.setRefreshToken(null); // ❌ Ne pas retourner dans le JSON
        authResponse.setTokenType("Bearer");
        authResponse.setUserId(user.getId());
        authResponse.setUsername(user.getUsername());
        authResponse.setEmail(user.getEmail());
        authResponse.setRole(user.getRole().name());

        return ResponseEntity.status(HttpStatus.CREATED).body(authResponse);
    }

    @Operation(summary = "Refresh access token", description = "Generate new access token using refresh token")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest request, HttpServletResponse response) {
        // 🐛 DEBUG: Vérifier si les cookies arrivent
        Cookie[] cookies = request.getCookies();
        System.out.println("📥 POST /auth/refresh - Cookies reçus: " + (cookies != null ? cookies.length : "NULL"));

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                String preview = cookie.getValue().length() > 20
                        ? cookie.getValue().substring(0, 20) + "..."
                        : cookie.getValue();
                System.out.println("   🍪 " + cookie.getName() + " = " + preview);
            }
        }

        // Vérifier que le header Origin est présent (CORS)
        String origin = request.getHeader("Origin");
        String userAgent = request.getHeader("User-Agent");
        System.out.println("🌍 Origin: " + origin);
        System.out.println("🔧 User-Agent: "
                + (userAgent != null ? userAgent.substring(0, Math.min(50, userAgent.length())) : "NULL"));

        // 🍪 Lire le refreshToken depuis le cookie httpOnly
        String refreshTokenValue = Arrays.stream(cookies != null ? cookies : new Cookie[0])
                .filter(cookie -> "refreshToken".equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElseThrow(() -> {
                    System.err.println("❌ Cookie 'refreshToken' introuvable dans la requête");
                    System.err.println(
                            "💡 Vérifier: withCredentials=true côté Angular, CORS allowCredentials=true côté backend");
                    return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No refresh token provided");
                });

        System.out.println("✅ RefreshToken trouvé: " + refreshTokenValue.substring(0, 20) + "...");

        return refreshTokenService.findByToken(refreshTokenValue)
                .map(refreshTokenService::verifyExpiration)
                .map(oldToken -> {
                    User user = oldToken.getUser();

                    // ROTATION: Generate new tokens
                    String newAccessToken = jwtService.generateAccessToken(user);
                    RefreshToken newRefreshToken = refreshTokenService.rotateRefreshToken(oldToken);

                    // 🔐 Envoyer le NOUVEAU refreshToken en cookie
                    ResponseCookie newRefreshCookie = ResponseCookie.from("refreshToken", newRefreshToken.getToken())
                            .httpOnly(true)
                            .secure(isProduction)
                            .sameSite("Lax")
                            .path("/")
                            .maxAge(7 * 24 * 60 * 60)
                            .build();

                    response.addHeader(HttpHeaders.SET_COOKIE, newRefreshCookie.toString());

                    System.out.println("✅ Refresh réussi pour user: " + user.getEmail());

                    AuthResponse authResponse = new AuthResponse();
                    authResponse.setAccessToken(newAccessToken);
                    authResponse.setRefreshToken(null); // ❌ Ne pas retourner dans le JSON
                    authResponse.setTokenType("Bearer");
                    authResponse.setUserId(user.getId());
                    authResponse.setUsername(user.getUsername());
                    authResponse.setEmail(user.getEmail());
                    authResponse.setRole(user.getRole().name());
                    return ResponseEntity.ok(authResponse);
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
    }

    @Operation(summary = "Revoke all tokens", description = "Revoke all access tokens for current user")
    @PostMapping("/revoke-all-tokens")
    public ResponseEntity<Void> revokeAllTokens() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Revoke all access tokens
        tokenBlacklistService.revokeAllUserTokens(java.util.Objects.requireNonNull(user.getId()));

        // Delete all refresh tokens
        refreshTokenService.deleteByUserId(user.getId());

        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {

        // Lire le refreshToken depuis le cookie httpOnly
        String refreshTokenValue = null;
        if (request.getCookies() != null) {
            refreshTokenValue = Arrays.stream(request.getCookies())
                .filter(c -> "refreshToken".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
        }

        // Si cookie présent, révoquer les tokens via le refreshToken en base
        if (refreshTokenValue != null) {
            refreshTokenService.findByToken(refreshTokenValue).ifPresent(rt ->
                refreshTokenService.revokeAllByUserId(
                    java.util.Objects.requireNonNull(rt.getUser().getId())
                )
            );
        }

        // Supprimer le cookie dans tous les cas
        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(isProduction)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());

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
        tokenBlacklistService.revokeAllUserTokens(java.util.Objects.requireNonNull(user.getId()));
        refreshTokenService.deleteByUserId(java.util.Objects.requireNonNull(user.getId()));

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