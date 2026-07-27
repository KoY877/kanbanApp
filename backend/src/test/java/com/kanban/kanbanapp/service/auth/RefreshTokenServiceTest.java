package com.kanban.kanbanapp.service.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.kanban.kanbanapp.Model.RefreshToken;
import com.kanban.kanbanapp.Model.User;
import com.kanban.kanbanapp.repository.RefreshTokenRepository;
import com.kanban.kanbanapp.repository.UserRepository;

/**
 * Unit tests for {@link RefreshTokenService}. The service uses field
 * injection, so mocks are wired in via {@link ReflectionTestUtils}.
 */
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final long DURATION_MS = 604_800_000L;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    private RefreshTokenService refreshTokenService;

    private User user;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService();
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenRepository", refreshTokenRepository);
        ReflectionTestUtils.setField(refreshTokenService, "userRepository", userRepository);
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenDuration", DURATION_MS);

        user = new User();
        user.setId("user-1");
    }

    // --- createRefreshToken ---

    @Test
    void createRefreshToken_savesAndReturnsNewToken_whenUserExists() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken result = refreshTokenService.createRefreshToken("user-1");

        assertThat(result.getUser()).isEqualTo(user);
        assertThat(result.getToken()).isNotBlank();
        assertThat(result.getTokenFamily()).isNotBlank();
        assertThat(result.isRevoked()).isFalse();
        assertThat(result.getExpiryDate()).isAfter(Instant.now());
    }

    @Test
    void createRefreshToken_throws_whenUserDoesNotExist() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.createRefreshToken("missing"))
                .isInstanceOf(RuntimeException.class);

        verify(refreshTokenRepository, never()).save(any());
    }

    // --- findByToken ---

    @Test
    void findByToken_returnsToken_whenPresent() {
        RefreshToken token = new RefreshToken();
        when(refreshTokenRepository.findByToken("abc")).thenReturn(Optional.of(token));

        assertThat(refreshTokenService.findByToken("abc")).contains(token);
    }

    @Test
    void findByToken_returnsEmpty_whenNotFound() {
        when(refreshTokenRepository.findByToken("missing")).thenReturn(Optional.empty());

        assertThat(refreshTokenService.findByToken("missing")).isEmpty();
    }

    // --- verifyExpiration ---

    @Test
    void verifyExpiration_returnsToken_whenValidAndNotExpired() {
        RefreshToken token = new RefreshToken();
        token.setRevoked(false);
        token.setExpiryDate(Instant.now().plusSeconds(60));

        RefreshToken result = refreshTokenService.verifyExpiration(token);

        assertThat(result).isEqualTo(token);
    }

    @Test
    void verifyExpiration_throws_whenTokenIsRevoked() {
        RefreshToken token = new RefreshToken();
        token.setRevoked(true);

        assertThatThrownBy(() -> refreshTokenService.verifyExpiration(token))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void verifyExpiration_deletesAndThrows_whenTokenIsExpired() {
        RefreshToken token = new RefreshToken();
        token.setRevoked(false);
        token.setExpiryDate(Instant.now().minusSeconds(60));

        assertThatThrownBy(() -> refreshTokenService.verifyExpiration(token))
                .isInstanceOf(ResponseStatusException.class);

        verify(refreshTokenRepository).delete(token);
    }

    // --- deleteByUserId ---

    @Test
    void deleteByUserId_deletesTokens_whenUserExists() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        refreshTokenService.deleteByUserId("user-1");

        verify(refreshTokenRepository).deleteByUser(user);
    }

    @Test
    void deleteByUserId_throws_whenUserDoesNotExist() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.deleteByUserId("missing"))
                .isInstanceOf(RuntimeException.class);

        verify(refreshTokenRepository, never()).deleteByUser(any());
    }

    // --- cleanupExpiredTokens ---

    @Test
    void cleanupExpiredTokens_deletesTokensExpiredBeforeNow() {
        when(refreshTokenRepository.deleteByExpiryDateBefore(any(Instant.class))).thenReturn(3);

        refreshTokenService.cleanupExpiredTokens();

        verify(refreshTokenRepository).deleteByExpiryDateBefore(any(Instant.class));
    }

    // --- rotateRefreshToken ---

    @Test
    void rotateRefreshToken_revokesOldAndCreatesNewTokenInSameFamily_whenTokenNotRevoked() {
        RefreshToken oldToken = new RefreshToken();
        oldToken.setUser(user);
        oldToken.setRevoked(false);
        oldToken.setTokenFamily("family-1");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken newToken = refreshTokenService.rotateRefreshToken(oldToken);

        assertThat(oldToken.isRevoked()).isTrue();
        assertThat(newToken.getTokenFamily()).isEqualTo("family-1");
        assertThat(newToken.isRevoked()).isFalse();
        assertThat(newToken.getUser()).isEqualTo(user);
        verify(refreshTokenRepository, never()).deleteByTokenFamily(any());
    }

    @Test
    void rotateRefreshToken_throwsWithoutRevokingFamily_whenRevokedRecently() {
        RefreshToken oldToken = new RefreshToken();
        oldToken.setUser(user);
        oldToken.setRevoked(true);
        oldToken.setRevokedAt(Instant.now().minusSeconds(5));
        oldToken.setTokenFamily("family-1");

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken(oldToken))
                .isInstanceOf(ResponseStatusException.class);

        verify(refreshTokenRepository, never()).deleteByTokenFamily(any());
    }

    @Test
    void rotateRefreshToken_revokesEntireFamilyAndThrows_whenRevokedLongAgo() {
        RefreshToken oldToken = new RefreshToken();
        oldToken.setUser(user);
        oldToken.setRevoked(true);
        oldToken.setRevokedAt(Instant.now().minusSeconds(60));
        oldToken.setTokenFamily("family-1");

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken(oldToken))
                .isInstanceOf(ResponseStatusException.class);

        verify(refreshTokenRepository).deleteByTokenFamily("family-1");
    }

    // --- revokeTokenFamily ---

    @Test
    void revokeTokenFamily_deletesAllTokensInFamily() {
        refreshTokenService.revokeTokenFamily("family-1");

        verify(refreshTokenRepository).deleteByTokenFamily("family-1");
    }

    // --- revokeAllByUserId ---

    @Test
    void revokeAllByUserId_marksAllTokensRevoked_whenTokensExist() {
        RefreshToken token1 = new RefreshToken();
        RefreshToken token2 = new RefreshToken();
        when(refreshTokenRepository.findByUserId("user-1")).thenReturn(List.of(token1, token2));

        refreshTokenService.revokeAllByUserId("user-1");

        assertThat(token1.isRevoked()).isTrue();
        assertThat(token2.isRevoked()).isTrue();
        verify(refreshTokenRepository).saveAll(List.of(token1, token2));
    }

    @Test
    void revokeAllByUserId_savesEmptyList_whenUserHasNoTokens() {
        when(refreshTokenRepository.findByUserId("user-1")).thenReturn(List.of());

        refreshTokenService.revokeAllByUserId("user-1");

        verify(refreshTokenRepository).saveAll(List.of());
    }
}
