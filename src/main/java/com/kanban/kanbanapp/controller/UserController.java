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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "User Controller", description = "APIs for managing users")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:8081"}, allowCredentials = "true")
@RestController
@RequiredArgsConstructor
@RequestMapping("/user")  
@Validated
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final UserRepository userRepository;

    @PreAuthorize("hasRole('ADMINISTRATOR')") // Ensure only administrators can access this endpoint
    @Operation(summary = "Get all users", description = "Retrieve a list of all users")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<User>> getAll() {
        log.info("Request to get all users");
        // Ensure only administrators can access this endpoint
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @PreAuthorize("hasRole('ADMINISTRATOR')") // Ensure only administrators can access this endpoint
    @Operation(summary = "Get user by ID", description = "Retrieve a specific user by their ID")
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<User> getUserById(@PathVariable String id) {
        log.info("Request to get user with id: {}", id);
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('ADMINISTRATOR')") // Ensure only administrators can access this endpoint
    @Operation(summary = "Delete a user", description = "Delete a user by their ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        log.info("Deleting user with id: {}", id);
        return userRepository.findById(id)
                .map(user -> {
                    userRepository.delete(user);
                    log.info("User deleted successfully");
                    return ResponseEntity.ok().<Void>build();
                })
                .orElseGet(() -> {
                    log.warn("User with id {} not found", id);
                    return ResponseEntity.notFound().build();
                });
    }
}