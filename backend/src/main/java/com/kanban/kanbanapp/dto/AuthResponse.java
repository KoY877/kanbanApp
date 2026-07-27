package com.kanban.kanbanapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO returned after successful authentication.
 *
 * @param accessToken  JWT access token (short-lived, 15 minutes)
 * @param refreshToken deprecated, always null; the refresh token is sent via the httpOnly cookie instead
 * @param tokenType    token type, always "Bearer"
 * @param userId       the authenticated user's id
 * @param username     the authenticated user's username
 * @param email        the authenticated user's email
 * @param role         the authenticated user's role
 */
@Schema(description = "Response DTO returned after successful authentication")
public record AuthResponse(
        @Schema(description = "JWT access token (short-lived, 15 minutes)", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...", nullable = false) String accessToken,

        @Schema(description = "DEPRECATED: Always null. Refresh token is sent via httpOnly cookie (Set-Cookie header)", example = "null", deprecated = true, hidden = true) String refreshToken,

        @Schema(description = "Token type (always 'Bearer')", example = "Bearer", defaultValue = "Bearer") String tokenType,

        @Schema(description = "User ID", example = "user-123-abc") String userId,

        @Schema(description = "Username", example = "johndoe") String username,

        @Schema(description = "User's email", example = "john.doe@example.com") String email,

        @Schema(description = "User role", example = "ADMINISTRATOR") String role) {
}
