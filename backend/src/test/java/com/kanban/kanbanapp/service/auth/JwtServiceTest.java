package com.kanban.kanbanapp.service.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.kanban.kanbanapp.Model.User;
import com.kanban.kanbanapp.Model.enums.Role;
import com.kanban.kanbanapp.config.JwtProperties;

/**
 * Unit tests for {@link JwtService}. Pure token logic, no collaborators to
 * mock.
 */
class JwtServiceTest {

    private static final String SECRET = "test-secret-key-must-be-at-least-32-bytes-long-for-hmac-sha256";

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.getAccessToken().setExpiration(900_000L);
        properties.getRefreshToken().setExpiration(604_800_000L);
        jwtService = new JwtService(properties);

        user = new User();
        user.setId("user-1");
        user.setEmail("john.doe@example.com");
        user.setUsername("johndoe");
        user.setRole(Role.STANDARD);
    }

    private JwtService jwtServiceWithExpiration(long accessTokenExpirationMs) {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.getAccessToken().setExpiration(accessTokenExpirationMs);
        properties.getRefreshToken().setExpiration(604_800_000L);
        return new JwtService(properties);
    }

    // --- generateAccessToken / extract* ---

    @Test
    void generateAccessToken_producesTokenWithExpectedClaims() {
        String token = jwtService.generateAccessToken(user);

        assertThat(jwtService.extractSubject(token)).isEqualTo(user.getEmail());
        assertThat(jwtService.extractEmail(token)).isEqualTo(user.getEmail());
        assertThat(jwtService.extractUserId(token)).isEqualTo(user.getId());
        assertThat(jwtService.extractExpiration(token)).isAfter(new Date());
        assertThat(jwtService.extractIssuedAt(token)).isNotNull();
        assertThat(jwtService.isTokenExpired(token)).isFalse();
    }

    @Test
    void isTokenExpired_returnsTrue_whenTokenIsPastExpiration() {
        JwtService expiredJwtService = jwtServiceWithExpiration(-10_000L);
        String token = expiredJwtService.generateAccessToken(user);

        assertThat(expiredJwtService.isTokenExpired(token)).isTrue();
    }

    // --- generateRefreshToken ---

    @Test
    void generateRefreshToken_producesTokenWithSubjectAndEmail() {
        String token = jwtService.generateRefreshToken(user);

        assertThat(jwtService.extractSubject(token)).isEqualTo(user.getEmail());
        assertThat(jwtService.extractEmail(token)).isEqualTo(user.getEmail());
    }

    // --- validateToken(token, email) ---

    @Test
    void validateToken_returnsTrue_whenAccessTokenMatchesUserAndNotExpired() {
        String token = jwtService.generateAccessToken(user);

        assertThat(jwtService.validateToken(token, user.getEmail())).isTrue();
    }

    @Test
    void validateToken_returnsFalse_whenEmailDoesNotMatchSubject() {
        String token = jwtService.generateAccessToken(user);

        assertThat(jwtService.validateToken(token, "someone-else@example.com")).isFalse();
    }

    @Test
    void validateToken_returnsFalse_whenTokenIsExpired() {
        JwtService expiredJwtService = jwtServiceWithExpiration(-10_000L);
        String token = expiredJwtService.generateAccessToken(user);

        assertThat(expiredJwtService.validateToken(token, user.getEmail())).isFalse();
    }

    @Test
    void validateToken_returnsFalse_whenTokenIsARefreshTokenNotAccessToken() {
        String refreshToken = jwtService.generateRefreshToken(user);

        assertThat(jwtService.validateToken(refreshToken, user.getEmail())).isFalse();
    }

    @Test
    void validateToken_returnsFalse_whenTokenIsMalformed() {
        assertThat(jwtService.validateToken("not-a-valid-jwt", user.getEmail())).isFalse();
    }

    // --- validateToken(token) ---

    @Test
    void validateTokenSingleArg_returnsTrue_whenTokenIsValid() {
        String token = jwtService.generateAccessToken(user);

        assertThat(jwtService.validateToken(token)).isTrue();
    }

    @Test
    void validateTokenSingleArg_returnsFalse_whenTokenIsExpired() {
        JwtService expiredJwtService = jwtServiceWithExpiration(-10_000L);
        String token = expiredJwtService.generateAccessToken(user);

        assertThat(expiredJwtService.validateToken(token)).isFalse();
    }

    @Test
    void validateTokenSingleArg_returnsFalse_whenTokenIsMalformed() {
        assertThat(jwtService.validateToken("garbage.token.value")).isFalse();
    }

    // --- extractClaim ---

    @Test
    void extractClaim_appliesResolverToTokenClaims() {
        String token = jwtService.generateAccessToken(user);

        String subject = jwtService.extractClaim(token, claims -> claims.getSubject());
        assertThat(subject).isEqualTo(user.getEmail());
    }

    @Test
    void extractExpiration_isAfterIssuedAt() {
        String token = jwtService.generateAccessToken(user);

        Date issuedAt = jwtService.extractIssuedAt(token);
        Date expiration = jwtService.extractExpiration(token);

        assertThat(expiration).isAfter(issuedAt);
        assertThat(expiration.toInstant()).isAfter(Instant.now().minusSeconds(1));
    }
}
