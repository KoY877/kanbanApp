package com.kanban.kanbanapp.Data_Transfer_Object;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request DTO for user login")
public class LoginRequest {

    @Schema(
        description = "User's email address",
        example = "john.doe@example.com",
        nullable = false
    )

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @Schema(
        description = "User's password (minimum 12 characters)",
        example = "MyS3cur3P@ssw0rd!",
        nullable = false,
        minLength = 12
    )
    @NotBlank(message = "Password is required")
    @Size(min = 12, message = "Password must be at least 12 characters")
    private String password;
}