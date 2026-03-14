package com.kanban.kanbanapp.controller;

import com.kanban.kanbanapp.Data_Transfer_Object.UserCreateRequest;
import com.kanban.kanbanapp.Model.Board;
import com.kanban.kanbanapp.Model.User;
import com.kanban.kanbanapp.repository.BoardRepository;
import com.kanban.kanbanapp.repository.UserRepository;
import com.kanban.kanbanapp.service.user.UserService;

import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User Controller", description = "APIs for managing users")
@CrossOrigin(origins = { "http://localhost:4200", "http://localhost:8081"}, allowCredentials = "true")
@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
@Validated
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final UserRepository userRepository;
    private final BoardRepository boardRepository;

    // GET /user -> list all users
    @Operation(summary = "Get all users", description = "Retrieve a list of all users")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<User>> getAll() {
        log.info("Request to get all users");
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    // Get endpoint to fetch a user by email
    @GetMapping(value = "/validate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> validate(@RequestParam(value = "email") String email,
            @RequestParam(value = "password") String password) {

        log.info("Validating user with email: {}", email);
        User user = userService.validateUser(email, password);
        if (user != null) {
            // Retourner à la fois l'API Secret et l'User ID
            String response = String.format("{\"apiSecret\": \"%s\", \"userId\": \"%s\", \"username\": \"%s\", \"email\": \"%s\"}", 
                                        user.getSecret(), 
                                        user.getId(),
                                        user.getUsername(),
                                        user.getEmail());
            return ResponseEntity.ok(response);
        }
        log.warn("User validation failed for email: {}", email);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    // Endpoint to create a new user
    @Operation(summary = "Create a new user", description = "Create a new user with specified details")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<User> register(@Valid @RequestBody UserCreateRequest newUser) {
        log.info("Request to register new user with email: {}", newUser.getEmail());
        User savedUser = userService.registerUser(newUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }

    // DELETE /user/{secret} -> delete a user by secret
    @Operation(summary = "Delete a user", description = "Delete a user by its secret")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Secret of the user to delete", content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "user-secret-123")))
    @DeleteMapping("/{secret}")
    public void deleteUserBySecret(String secret) {
        log.info("Deleting user with secret: {}", secret);
        Optional<User> userInDB = userRepository.findBySecret(secret);
        
        if (userInDB.isPresent()) {
            User user = userInDB.get();
            
            // Supprimer tous les boards du user (déclenchera cascade vers columns/members)
            Set<Board> userBoards = boardRepository.findAllByUserId(user.getId());
            for (Board board : userBoards) {
                boardRepository.delete(board);  // Cascade vers KanbanColumn et Member
            }
            
            // Supprimer le user
            userRepository.delete(user);
            log.info("User and all associated boards deleted successfully");
        } else {
            log.warn("User with secret {} not found", secret);
        }
    }

    // PUT /user/{secret} -> update a user by secret
    @Operation(summary = "Update a user", description = "Update a user by its secret")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Fields to update (username, email, password)", content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"username\": \"newUsername\", \"email\": \"new@email.com\", \"password\": \"newPassword\"}")))
    @PutMapping("/{secret}")
    public ResponseEntity<User> update(@PathVariable("secret") String secret,
            @Valid @RequestBody UserCreateRequest request) {

        // Log the incoming request
        log.info("Request to update user with secret: {}", secret);
        try {
            User updatedUser = userService.updateUser(secret, request);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            log.error("Error updating user: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    // PATCH /user/{secret} -> patch a user by secret
    @Operation(summary = "Patch a user", description = "Partially update a user by its secret. Only send the fields you want to update.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Fields to update (username, email, password)", content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"username\": \"newUsername\", \"email\": \"new@email.com\", \"password\": \"newPassword\"}")))
    @PatchMapping(value = "/{secret}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<User> patch(@PathVariable String secret, @RequestBody Map<String, Object> updates) {

        // Log the incoming request
        log.info("Request to patch user with secret: {}", secret);
        try {
            User patchedUser = userService.patchUser(secret, updates);
            return ResponseEntity.ok(patchedUser);
        } catch (RuntimeException e) {
            log.error("Error patching user: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}