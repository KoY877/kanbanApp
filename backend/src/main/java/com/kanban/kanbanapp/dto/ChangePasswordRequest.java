package com.kanban.kanbanapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for changing the authenticated user's password.
 *
 * @param currentPassword the user's current password, verified before the change
 * @param newPassword     the new password (minimum 12 characters)
 */
public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required") String currentPassword,

        @NotBlank(message = "New password is required") @Size(min = 12, message = "Password must be at least 12 characters") String newPassword) {
}
