package com.kanban.kanbanapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for completing a password-reset flow.
 *
 * @param token       the reset token issued to the user
 * @param newPassword the new password (minimum 12 characters)
 */
public record ResetPasswordRequest(
        @NotBlank(message = "Token is required") String token,

        @NotBlank(message = "New password is required") @Size(min = 12, message = "Password must be at least 12 characters") String newPassword) {
}
