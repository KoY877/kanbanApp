package com.kanban.kanbanapp.controller;

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

    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @Operation(summary = "Get all users", description = "Retrieve a list of all users")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<User>> getAll() {
        log.info("Request to get all users");
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @Operation(summary = "Get user by ID", description = "Retrieve a specific user by their ID")
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<User> getUserById(@PathVariable @NonNull String id) {
        log.info("Request to get user with id: {}", id);
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

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

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update user profile", description = "Update the authenticated user's profile (username, email, password)")
    @PatchMapping("/profile/{id}") 
    public ResponseEntity<User> updateProfile(@PathVariable @NonNull String id, @RequestBody Map<String, String> updates) {
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
            String newPassword = updates.get("password");
            
            // Validation basique (ajouter plus de règles si nécessaire)
            if (newPassword.length() < 12) {
                throw new IllegalArgumentException("Password must be at least 12 characters long");
            }
            
            user.setPassword(passwordEncoder.encode(newPassword));
            log.info("Password updated for user: {}", user.getEmail());
        }
               
        return ResponseEntity.ok(user);
    }

    // getAuthenticatedUser()
    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));
    }
}