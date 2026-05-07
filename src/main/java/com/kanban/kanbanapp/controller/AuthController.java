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
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

    @Operation(summary = "User login", description = """
            Authenticate user with email/password and return JWT tokens.

            **IMPORTANT - httpOnly Cookie:**
            - The `refreshToken` is sent via **Set-Cookie header** (httpOnly cookie)
            - It does NOT appear in the JSON response body (refreshToken field is always null)
            - The cookie is automatically sent by the browser on subsequent requests
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful - Access token in body, Refresh token in Set-Cookie header", headers = @Header(name = "Set-Cookie", description = "httpOnly cookie containing the refresh token (7 days expiration)", schema = @Schema(type = "string", example = "refreshToken=abc123...; HttpOnly; Secure; SameSite=Lax; Path=/; Max-Age=604800")), content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })

    /**
     * Authenticate a user with email and password.
     * Returns the access token in the response body and sets the refresh token as
     * an httpOnly cookie.
     *
     * @param request  login credentials (email + password)
     * @param response HTTP response used to set the Set-Cookie header
     * @return 200 with AuthResponse (accessToken), or 401 if credentials are
     *         invalid
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        User user = userService.validateUser(request.getEmail(), request.getPassword());

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String accessToken = jwtService.generateAccessToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        // SECURITY: Send refreshToken as httpOnly cookie
        ResponseCookie responseCookie = ResponseCookie.from("refreshToken", refreshToken.getToken())
                .httpOnly(true) // XSS protection
                .secure(isProduction) // HTTPS in production
                .sameSite("Lax") // CSRF protection
                .path("/") // Sent to all endpoints
                .maxAge(7 * 24 * 60 * 60) // 7 days
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, responseCookie.toString());

        // Return ONLY the accessToken in the body (not the refreshToken)
        AuthResponse authResponse = new AuthResponse();
        authResponse.setAccessToken(accessToken);
        authResponse.setRefreshToken(null); // Not returned in JSON
        authResponse.setTokenType("Bearer");
        authResponse.setUserId(user.getId());
        authResponse.setUsername(user.getUsername());
        authResponse.setEmail(user.getEmail());
        authResponse.setRole(user.getRole().name());

        return ResponseEntity.ok(authResponse);
    }

    @Operation(summary = "User registration", description = """
            Register a new user account and return JWT tokens.

            **IMPORTANT - httpOnly Cookie:**
            - The `refreshToken` is sent via **Set-Cookie header** (httpOnly cookie)
            - It does NOT appear in the JSON response body (refreshToken field is always null)
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User created successfully - Access token in body, Refresh token in Set-Cookie header", headers = @Header(name = "Set-Cookie", description = "httpOnly cookie containing the refresh token (7 days expiration)", schema = @Schema(type = "string", example = "refreshToken=xyz789...; HttpOnly; Secure; SameSite=Lax; Path=/; Max-Age=604800")), content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input or user already exists")
    })
    /**
     * Register a new user account.
     * Returns the access token in the response body and sets the refresh token as
     * an httpOnly cookie.
     *
     * @param request  registration payload (username, email, password)
     * @param response HTTP response used to set the Set-Cookie header
     * @return 201 with AuthResponse, or 400 if the user already exists or input is
     *         invalid
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request, HttpServletResponse response) {
        User user = userService.registerUser(request);

        String accessToken = jwtService.generateAccessToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        // SECURITY: Send refreshToken as httpOnly cookie
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
        authResponse.setRefreshToken(null); // Not returned in JSON
        authResponse.setTokenType("Bearer");
        authResponse.setUserId(user.getId());
        authResponse.setUsername(user.getUsername());
        authResponse.setEmail(user.getEmail());
        authResponse.setRole(user.getRole().name());

        return ResponseEntity.status(HttpStatus.CREATED).body(authResponse);
    }

    @Operation(summary = "Refresh access token", description = "Generate new access token using refresh token")
    /**
     * Refresh the access token using the httpOnly refresh-token cookie.
     * Implements token rotation: the old refresh token is revoked and a new one is
     * issued.
     *
     * @param request  HTTP request carrying the httpOnly refreshToken cookie
     * @param response HTTP response used to set the new Set-Cookie header
     * @return 200 with a new AuthResponse, or 401 if the cookie is
     *         missing/expired/revoked
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest request, HttpServletResponse response) {
        // DEBUG: Check if cookies are received
        Cookie[] cookies = request.getCookies();
        System.out.println("POST /auth/refresh - Cookies received: " + (cookies != null ? cookies.length : "NULL"));

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                String preview = cookie.getValue().length() > 20
                        ? cookie.getValue().substring(0, 20) + "..."
                        : cookie.getValue();
                System.out.println("   [cookie] " + cookie.getName() + " = " + preview);
            }
        }

        // Check that Origin header is present (CORS)
        String origin = request.getHeader("Origin");
        String userAgent = request.getHeader("User-Agent");
        System.out.println("Origin: " + origin);
        System.out.println("User-Agent: "
                + (userAgent != null ? userAgent.substring(0, Math.min(50, userAgent.length())) : "NULL"));

        // Read refreshToken from httpOnly cookie
        String refreshTokenValue = Arrays.stream(cookies != null ? cookies : new Cookie[0])
                .filter(cookie -> "refreshToken".equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElseThrow(() -> {
                    System.err.println("Cookie 'refreshToken' not found in request");
                    System.err.println(
                            "Check: withCredentials=true on the Angular side, CORS allowCredentials=true on the backend");
                    return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No refresh token provided");
                });

        System.out.println("RefreshToken found: " + refreshTokenValue.substring(0, 20) + "...");

        return refreshTokenService.findByToken(refreshTokenValue)
                .map(refreshTokenService::verifyExpiration)
                .map(oldToken -> {
                    User user = oldToken.getUser();

                    // ROTATION: Generate new tokens
                    String newAccessToken = jwtService.generateAccessToken(user);
                    RefreshToken newRefreshToken = refreshTokenService.rotateRefreshToken(oldToken);

                    // Send the NEW refreshToken as cookie
                    ResponseCookie newRefreshCookie = ResponseCookie.from("refreshToken", newRefreshToken.getToken())
                            .httpOnly(true)
                            .secure(isProduction)
                            .sameSite("Lax")
                            .path("/")
                            .maxAge(7 * 24 * 60 * 60)
                            .build();

                    response.addHeader(HttpHeaders.SET_COOKIE, newRefreshCookie.toString());

                    System.out.println("Refresh successful for user: " + user.getEmail());

                    AuthResponse authResponse = new AuthResponse();
                    authResponse.setAccessToken(newAccessToken);
                    authResponse.setRefreshToken(null); // Not returned in JSON
                    authResponse.setTokenType("Bearer");
                    authResponse.setUserId(user.getId());
                    authResponse.setUsername(user.getUsername());
                    authResponse.setEmail(user.getEmail());
                    authResponse.setRole(user.getRole().name());
                    return ResponseEntity.ok(authResponse);
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
    }

    /**
     * Revoke all access tokens and refresh tokens for the currently authenticated
     * user.
     * Use this endpoint when a security event requires immediate full-logout on all
     * devices.
     *
     * @return 200 on success
     */
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

    /**
     * Log out the authenticated user.
     * PROTECTED - do not modify this method without explicit confirmation.
     *
     * Revokes all refresh tokens in the database for the user (preventing cookie
     * reuse),
     * then deletes the httpOnly cookie by setting maxAge=0 in the response.
     *
     * @param request  HTTP request carrying the refreshToken cookie
     * @param response HTTP response used to clear the cookie
     * @return 200 on success
     */
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
            refreshTokenService.findByToken(refreshTokenValue).ifPresent(rt -> refreshTokenService.revokeAllByUserId(
                    java.util.Objects.requireNonNull(rt.getUser().getId())));
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

    /**
     * Change the password for the currently authenticated user.
     * Verifies the current password, hashes the new one, then revokes all existing
     * access and refresh tokens to force re-login on all devices.
     *
     * @param request change-password payload (currentPassword + newPassword)
     * @return 200 on success, 401 if the current password is incorrect
     */
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