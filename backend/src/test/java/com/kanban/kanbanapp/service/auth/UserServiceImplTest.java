package com.kanban.kanbanapp.service.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.kanban.kanbanapp.Model.User;
import com.kanban.kanbanapp.Model.enums.Role;
import com.kanban.kanbanapp.dto.RegisterRequest;
import com.kanban.kanbanapp.repository.UserRepository;

/**
 * Unit tests for {@link UserServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserServiceImpl userService;

    private User user;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, passwordEncoder);

        user = new User();
        user.setId("user-1");
        user.setEmail("john.doe@example.com");
        user.setUsername("johndoe");
        user.setPassword("hashed-password");
        user.setRole(Role.STANDARD);
    }

    // --- getAllUsers ---

    @Test
    void getAllUsers_returnsAllUsers() {
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<User> result = userService.getAllUsers();

        assertThat(result).containsExactly(user);
    }

    @Test
    void getAllUsers_returnsEmptyList_whenNoUsers() {
        when(userRepository.findAll()).thenReturn(List.of());

        assertThat(userService.getAllUsers()).isEmpty();
    }

    // --- validateUser ---

    @Test
    void validateUser_returnsUser_whenCredentialsAreValid() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("raw-password", user.getPassword())).thenReturn(true);

        User result = userService.validateUser(user.getEmail(), "raw-password");

        assertThat(result).isEqualTo(user);
    }

    @Test
    void validateUser_throwsBadCredentials_whenUserNotFound() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.validateUser("missing@example.com", "password"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void validateUser_throwsLocked_whenAccountLockedWithinLockWindow() {
        user.setAccountLocked(true);
        user.setLockTime(Instant.now().minus(5, ChronoUnit.MINUTES));
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.validateUser(user.getEmail(), "raw-password"))
                .isInstanceOf(LockedException.class);

        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void validateUser_autoUnlocksAndSucceeds_whenLockWindowHasExpired() {
        user.setAccountLocked(true);
        user.setLockTime(Instant.now().minus(31, ChronoUnit.MINUTES));
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("raw-password", user.getPassword())).thenReturn(true);

        User result = userService.validateUser(user.getEmail(), "raw-password");

        assertThat(result.isAccountLocked()).isFalse();
        assertThat(result.getFailedLoginAttempts()).isZero();
    }

    @Test
    void validateUser_throwsBadCredentialsAndIncrementsFailedAttempts_whenPasswordIsWrong() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", user.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> userService.validateUser(user.getEmail(), "wrong-password"))
                .isInstanceOf(BadCredentialsException.class);

        assertThat(user.getFailedLoginAttempts()).isEqualTo(1);
        assertThat(user.isAccountLocked()).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    void validateUser_locksAccount_whenFailedAttemptsReachFive() {
        user.setFailedLoginAttempts(4);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", user.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> userService.validateUser(user.getEmail(), "wrong-password"))
                .isInstanceOf(BadCredentialsException.class);

        assertThat(user.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(user.isAccountLocked()).isTrue();
        assertThat(user.getLockTime()).isNotNull();
    }

    // --- roleUser ---

    @Test
    void roleUser_updatesAndReturnsUser_whenUserExists() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        User result = userService.roleUser("user-1", Role.ADMINISTRATOR);

        assertThat(result.getRole()).isEqualTo(Role.ADMINISTRATOR);
        verify(userRepository).save(user);
    }

    @Test
    void roleUser_throwsUserNotFound_whenUserDoesNotExist() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.roleUser("missing", Role.ADMINISTRATOR))
                .isInstanceOf(UserServiceImpl.UserNotFoundException.class);

        verify(userRepository, never()).save(any());
    }

    // --- registerUser ---

    @Test
    void registerUser_createsUserWithAdministratorRole_whenEmailNotTaken() {
        RegisterRequest request = new RegisterRequest("newuser", "new@example.com", "raw-password-123");
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("raw-password-123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.registerUser(request);

        assertThat(result.getEmail()).isEqualTo("new@example.com");
        assertThat(result.getUsername()).isEqualTo("newuser");
        assertThat(result.getPassword()).isEqualTo("hashed");
        assertThat(result.getRole()).isEqualTo(Role.ADMINISTRATOR);
    }

    @Test
    void registerUser_throwsUserAlreadyExists_whenEmailIsTaken() {
        RegisterRequest request = new RegisterRequest("newuser", user.getEmail(), "raw-password-123");
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.registerUser(request))
                .isInstanceOf(UserServiceImpl.UserAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }

    // --- deleteUserById ---

    @Test
    void deleteUserById_deletesUser_whenUserExists() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        userService.deleteUserById("user-1");

        verify(userRepository).delete(user);
    }

    @Test
    void deleteUserById_throwsUserNotFound_whenUserDoesNotExist() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUserById("missing"))
                .isInstanceOf(UserServiceImpl.UserNotFoundException.class);

        verify(userRepository, never()).delete(any());
    }

    // --- updateUser ---

    @Test
    void updateUser_updatesAllProvidedFields_whenUserExists() {
        User updates = new User();
        updates.setUsername("newname");
        updates.setEmail("new@example.com");
        updates.setPassword("new-raw-password");
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("new-raw-password")).thenReturn("new-hashed");
        when(userRepository.save(user)).thenReturn(user);

        User result = userService.updateUser("user-1", updates);

        assertThat(result.getUsername()).isEqualTo("newname");
        assertThat(result.getEmail()).isEqualTo("new@example.com");
        assertThat(result.getPassword()).isEqualTo("new-hashed");
    }

    @Test
    void updateUser_keepsExistingFields_whenUpdatedFieldsAreNull() {
        User updates = new User();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        User result = userService.updateUser("user-1", updates);

        assertThat(result.getUsername()).isEqualTo("johndoe");
        assertThat(result.getEmail()).isEqualTo("john.doe@example.com");
        assertThat(result.getPassword()).isEqualTo("hashed-password");
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void updateUser_throwsUserNotFound_whenUserDoesNotExist() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser("missing", new User()))
                .isInstanceOf(UserServiceImpl.UserNotFoundException.class);

        verify(userRepository, never()).save(any());
    }
}
