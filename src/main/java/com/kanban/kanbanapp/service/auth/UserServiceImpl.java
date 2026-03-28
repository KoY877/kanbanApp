package com.kanban.kanbanapp.service.auth;

import com.kanban.kanbanapp.Data_Transfer_Object.RegisterRequest;
import com.kanban.kanbanapp.Model.User;
import com.kanban.kanbanapp.Model.enums.Role;
import com.kanban.kanbanapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration; 
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<User> getAllUsers() {
        log.info("Fetching all users");
        return userRepository.findAll();
    }

    @Override
    public User validateUser(String email, String password) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        
        // Check if account is locked
        if (user.isAccountLocked()) {
            // Auto-unlock after 30 minutes
            if (user.getLockTime() != null && 
                Duration.between(user.getLockTime(), Instant.now()).toMinutes() >= 30) {
                
                log.info("Auto-unlocking account for user: {}", email);
                user.setAccountLocked(false);
                user.setFailedLoginAttempts(0);
                user.setLockTime(null);
                userRepository.save(user);
            } else {
                log.warn("Login attempt for locked account: {}", email);
                // Exception correcte
                throw new LockedException("Account is locked due to multiple failed login attempts. Please try again later.");
            }
        }
        
        // Validate password
        if (!passwordEncoder.matches(password, user.getPassword())) {
            // Increment failed attempts
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            user.setLastFailedLogin(Instant.now());
            
            log.warn("Failed login attempt #{} for user: {}", user.getFailedLoginAttempts(), email);
            
            // Lock after 5 failures
            if (user.getFailedLoginAttempts() >= 5) {
                user.setAccountLocked(true);
                user.setLockTime(Instant.now());
                log.error("Account locked for user: {} after 5 failed attempts", email);
                                
            }
            
            userRepository.save(user);
            throw new BadCredentialsException("Invalid credentials");
        }
        
        // Success - reset counter
        if (user.getFailedLoginAttempts() > 0) {
            log.info("Successful login for user: {} - resetting failed attempts counter", email);
            user.setFailedLoginAttempts(0);
            user.setLastFailedLogin(null);
            userRepository.save(user);
        }
        
        return user;
    }

    @Override
    public User roleUser(String userId, Role newRole) {
        log.info("Updating role for user with ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found with ID: {}", userId);
                    return new UserNotFoundException(userId);
                });

        user.setRole(newRole);
        User savedUser = userRepository.save(user);
        log.info("User role updated successfully for ID: {}", savedUser.getId());

        return savedUser;
    }

    @Override
    public User registerUser(RegisterRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());

        // Check if user already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            log.error("User already exists with email: {}", request.getEmail());
            throw new UserAlreadyExistsException(request.getEmail());
        }

        // Create new user
        User user = new User();
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        // All registred users get Administrator role
        user.setRole(Role.ADMINISTRATOR);
        
        // Save user
        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {}", savedUser.getId());

        return savedUser;
    }

    public class UserAlreadyExistsException extends RuntimeException {
        public UserAlreadyExistsException(String email) {
            super("User already exists with email: " + email);
        }
    }

    public class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(String userId) {
            super("User not found with ID: " + userId);
        }
    }

    @Override
    public void deleteUserById(String userId) {
        log.info("Deleting user with ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found with ID: {}", userId);
                    return new UserNotFoundException(userId);
                });

        userRepository.delete(user);
        log.info("User deleted successfully with ID: {}", userId);
    }

    @Override
    public User updateUser(String userId, User updatedUser) {
        log.info("Updating user with ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found with ID: {}", userId);
                    return new UserNotFoundException(userId);
                });

        // Update fields
        if (updatedUser.getUsername() != null) {
            user.setUsername(updatedUser.getUsername());
        }
        if (updatedUser.getEmail() != null) {
            user.setEmail(updatedUser.getEmail());
        }
        if (updatedUser.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
        }

        User savedUser = userRepository.save(user);
        log.info("User updated successfully with ID: {}", savedUser.getId());

        return savedUser;
    }
}