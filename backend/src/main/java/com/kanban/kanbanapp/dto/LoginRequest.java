package com.kanban.kanbanapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for user login.
 *
 * @param email    the user's email address
 * @param password the user's password (minimum 12 characters)
 */
@Schema(description = "Request DTO for user login")
public record LoginRequest(
        @Schema(description = "User's email address", example = "john.doe@example.com", nullable = false) @NotBlank(message = "Email is required") @Email(message = "Email should be valid") String email,

        @Schema(description = "User's password (minimum 12 characters)", example = "MyS3cur3P@ssw0rd!", nullable = false, minLength = 12) @NotBlank(message = "Password is required") @Size(min = 12, message = "Password must be at least 12 characters") String password) {
}
