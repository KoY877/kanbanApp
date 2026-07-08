package com.kanban.kanbanapp.Data_Transfer_Object;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response DTO returned after successful authentication")
public class AuthResponse {
    
    @Schema(
        description = "JWT access token (short-lived, 15 minutes)",
        example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
        required = true
    )
    private String accessToken;
    
    @Schema(
        description = "DEPRECATED: Always null. Refresh token is sent via httpOnly cookie (Set-Cookie header)",
        example = "null",
        deprecated = true,
        hidden = true
    )
    private String refreshToken;
    
    @Schema(
        description = "Token type (always 'Bearer')",
        example = "Bearer",
        defaultValue = "Bearer"
    )
    @Builder.Default
    private String tokenType = "Bearer";
    
    @Schema(
        description = "User ID",
        example = "user-123-abc"
    )
    private String userId;

    @Schema(
        description = "Username",
        example = "johndoe"
    )
    private String username;
    
    @Schema(
        description = "User's email",
        example = "john.doe@example.com"
    )
    private String email;

    @Schema(
        description = "User role",
        example = "ADMINISTRATOR"
    )
    @Enumerated(EnumType.STRING)
    private String role;
}