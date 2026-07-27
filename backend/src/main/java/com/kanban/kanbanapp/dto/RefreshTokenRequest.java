package com.kanban.kanbanapp.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO carrying a refresh token in the request body.
 *
 * @param refreshToken the refresh token
 */
public record RefreshTokenRequest(
        @NotBlank(message = "Refresh token is required") String refreshToken) {
}
