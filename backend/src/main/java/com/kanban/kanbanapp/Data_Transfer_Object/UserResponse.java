package com.kanban.kanbanapp.Data_Transfer_Object;

import com.kanban.kanbanapp.Model.User;
import com.kanban.kanbanapp.Model.enums.Role;

/**
 * Response DTO representing a user, excluding sensitive fields such as the
 * password hash.
 *
 * @param id       the user id
 * @param username the username
 * @param email    the user's email
 * @param role     the user's role
 */
public record UserResponse(String id, String username, String email, Role role) {

    /**
     * Build a UserResponse from a User entity, stripping the password.
     *
     * @param user the source entity
     * @return a UserResponse with no password
     */
    public static UserResponse fromEntity(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole());
    }
}
