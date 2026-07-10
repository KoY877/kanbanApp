package com.kanban.kanbanapp.service.auth;

import java.util.List;

import org.springframework.lang.NonNull;

import com.kanban.kanbanapp.Data_Transfer_Object.RegisterRequest;
import com.kanban.kanbanapp.Model.User;
import com.kanban.kanbanapp.Model.enums.Role;

public interface UserService {

    /**
     * Retrieve all registered users.
     *
     * @return the list of users
     */
    List<User> getAllUsers();

    /**
     * Validate a user's credentials, handling failed-attempt tracking and
     * account lockout.
     *
     * @param email    the user's email
     * @param password the raw (non-hashed) password to verify
     * @return the authenticated user
     * @throws org.springframework.security.authentication.BadCredentialsException
     *                                                                          if the email/password combination is invalid
     * @throws org.springframework.security.authentication.LockedException
     *                                                                          if the account is currently locked
     */
    User validateUser(String email, String password);

    /**
     * Change the role of a user.
     *
     * @param userId  the user id
     * @param newRole the role to assign
     * @return the updated user
     */
    User roleUser(String userId, Role newRole);

    /**
     * Register a new user account.
     *
     * @param request the registration payload (username, email, password)
     * @return the created user
     */
    User registerUser(RegisterRequest request);

    /**
     * Delete a user by id.
     *
     * @param userId the user id
     */
    void deleteUserById(String userId);

    /**
     * Update the mutable fields (username, email, password) of a user.
     *
     * @param userId      the user id
     * @param updatedUser a User carrying the new field values (null fields are
     *                    left unchanged)
     * @return the updated user
     */
    User updateUser(@NonNull String userId, @NonNull User updatedUser);
}
