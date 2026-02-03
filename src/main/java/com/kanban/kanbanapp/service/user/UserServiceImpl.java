package com.kanban.kanbanapp.service.user;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.kanban.kanbanapp.Data_Transfer_Object.UserCreateRequest;
import com.kanban.kanbanapp.Model.User;
import com.kanban.kanbanapp.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    // Logger instance
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<User> getAllUsers() {
        // Log the incoming request
        log.info("Fetching all users");
        // Retrieve all users from the repository
        return userRepository.findAll();
    }

    @Override
    public User validateUser(String email, String password) {
        // Log the incoming request
        log.info("Validating user with email: {}", email);
        // Find user by email
        Optional<User> userInDB = userRepository.findByEmail(email);

        if (userInDB.isEmpty()) {
            // Log the incoming request
            log.warn("User not found with email: {}", email);
            return null;
        }

        User user = userInDB.get();
        if (passwordEncoder.matches(password, user.getPassword())) {
            // Log the incoming request
            log.info("User validated successfully: {}", email);
            return user;
        }

        // Log the incoming request
        log.warn("Invalid password for user: {}", email);
        return null;
    }

    @Override
    public User registerUser(UserCreateRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setSecret(UUID.randomUUID().toString());

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {}", savedUser.getId());
        return savedUser;
    }

    @Override
    public void deleteUserBySecret(String secret) {
        // Log the incoming request
        log.info("Deleting user with secret: {}", secret);
        // Find user by secret
        Optional<User> userInDB = userRepository.findBySecret(secret);

        // If user not found, throw exception
        if (userInDB.isEmpty()) {
            // Log the incoming request
            log.error("User not found with secret: {}", secret);
            // Throw an exception indicating user not found
            throw new RuntimeException("User not found");
        }

        // Delete the user
        userRepository.delete(userInDB.get());
        log.info("User deleted successfully");
    }

    @Override
    @Transactional
    public User updateUser(String secret, UserCreateRequest request) {
        // Log the incoming request
        log.info("Updating user with secret: {}", secret);
        // Find user by secret
        Optional<User> userInDB = userRepository.findBySecret(secret);

        if (userInDB.isEmpty()) {
            // Log the incoming request
            log.error("User not found with secret: {}", secret);
            // Throw an exception indicating user not found
            throw new RuntimeException("User not found");
        }

        User user = userInDB.get();
        // Update fields
        user.setEmail(Optional.ofNullable(request.getEmail()).orElse(user.getEmail()));
        user.setUsername(Optional.ofNullable(request.getUsername()).orElse(user.getUsername()));

        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        
        // Save the updated user
        User updatedUser = userRepository.save(user);
        // Log the incoming request
        log.info("User updated successfully: {}", updatedUser.getId());
        return updatedUser;
    }

    @Override
    @Transactional
    public User patchUser(String secret, Map<String, Object> updates) {
        // Log the incoming request
        log.info("Patching user with secret: {}", secret);
        // Find user by secret
        Optional<User> userInDB = userRepository.findBySecret(secret);

        if (userInDB.isEmpty()) {
            // Log the incoming request
            log.error("User not found with secret: {}", secret);
            // Throw an exception indicating user not found
            throw new RuntimeException("User not found");
        }

        User user = userInDB.get();

        if (updates.containsKey("email")) {
            // Update email
            user.setEmail((String) updates.get("email"));
        }
        if (updates.containsKey("username")) {
            // Update username
            user.setUsername((String) updates.get("username"));
        }
        if (updates.containsKey("password")) {
            // Update password
            String rawPassword = (String) updates.get("password");
            // Only update if password is not null or empty
            if (rawPassword != null && !rawPassword.isEmpty()) {
                // Encode and set the new password
                user.setPassword(passwordEncoder.encode(rawPassword));
            }
        }

        // Save the patched user
        User patchedUser = userRepository.save(user);

        // Log the incoming request
        log.info("User patched successfully: {}", patchedUser.getId());
        return patchedUser;
    }
}
