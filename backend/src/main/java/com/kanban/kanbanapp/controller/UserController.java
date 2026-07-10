package com.kanban.kanbanapp.controller;

import com.kanban.kanbanapp.Data_Transfer_Object.UserResponse;
import com.kanban.kanbanapp.Model.User;
import com.kanban.kanbanapp.repository.UserRepository;
import com.kanban.kanbanapp.service.auth.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "User Controller", description = "APIs for managing users")
@CrossOrigin(origins = { "http://localhost:4200", "http://localhost:8081" }, allowCredentials = "true")
@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
@Validated
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Retrieve all registered users. Administrator-only.
     *
     * @return 200 with the list of users
     */
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @Operation(summary = "Get all users", description = "Retrieve a list of all users")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<UserResponse>> getAll() {
        log.info("Request to get all users");
        List<UserResponse> users = userService.getAllUsers().stream()
                .map(UserResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(users);
    }

    /**
     * Retrieve a single user by their ID. Administrator-only.
     *
     * @param id the user id
     * @return 200 with the user, or 404 if not found
     */
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @Operation(summary = "Get user by ID", description = "Retrieve a specific user by their ID")
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserResponse> getUserById(@PathVariable @NonNull String id) {
        log.info("Request to get user with id: {}", id);
        return userRepository.findById(id)
                .map(UserResponse::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete a user by their ID. Administrator-only.
     *
     * @param id the user id
     * @return 200 on success, or 404 if not found
     */
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @Operation(summary = "Delete a user", description = "Delete a user by their ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable @NonNull String id) {
        log.info("Deleting user with id: {}", id);
        return userRepository.findById(id)
                .map(user -> {
                    userRepository.delete(java.util.Objects.requireNonNull(user));
                    log.info("User deleted successfully");
                    return ResponseEntity.ok().<Void>build();
                })
                .orElseGet(() -> {
                    log.warn("User with id {} not found", id);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Partially update the authenticated user's profile.
     * Only the fields present in the request map are updated; the {@code id}
     * path variable is not used to look up the target user — the profile of
     * the currently authenticated user is always updated.
     *
     * @param id      unused path variable (kept for URL shape compatibility)
     * @param updates map of fields to update (username, email, password)
     * @return 200 with the updated user
     * @throws IllegalArgumentException if the new password is shorter than 12
     *                                  characters
     */
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update user profile", description = "Update the authenticated user's profile (username, email, password)")
    @PatchMapping("/profile/{id}")
    public ResponseEntity<UserResponse> updateProfile(@PathVariable @NonNull String id, @RequestBody Map<String, String> updates) {
        User user = getAuthenticatedUser();

        // Partial update - only provided fields are updated
        if (updates.containsKey("username") && updates.get("username") != null) {
            user.setUsername(updates.get("username").trim());
        }

        if (updates.containsKey("email") && updates.get("email") != null) {
            user.setEmail(updates.get("email").trim());
        }

        // Password handling with hashing
        if (updates.containsKey("password") && updates.get("password") != null) {
            String newPassword = updates.get("password").trim();

            // Basic validation (add more rules if needed)
            if (newPassword.length() < 12) {
                throw new IllegalArgumentException("Password must be at least 12 characters long");
            }

            user.setPassword(passwordEncoder.encode(newPassword));
            log.info("Password updated for user id: {}", user.getId());
        }

        // Persist the changes - without this the updates never reach the database
        User savedUser = userRepository.save(user);

        return ResponseEntity.ok(UserResponse.fromEntity(savedUser));
    }

    /**
     * Resolve the currently authenticated user from the security context.
     *
     * @return the authenticated User
     * @throws RuntimeException if no matching user is found
     */

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));
    }
}