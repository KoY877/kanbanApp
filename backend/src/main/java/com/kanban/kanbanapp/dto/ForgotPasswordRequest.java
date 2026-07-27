package com.kanban.kanbanapp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for initiating a password-reset flow.
 *
 * @param email the account email to send the reset link to
 */
public record ForgotPasswordRequest(
        @NotBlank(message = "Email is required") @Email(message = "Must be valid email") String email) {
}
