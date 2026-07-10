package com.kanban.kanbanapp.Data_Transfer_Object;

import com.kanban.kanbanapp.Model.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request DTO for user registration")
public class RegisterRequest {

    @Schema(
        description = "Username (3-50 characters)",
        example = "johndoe",
        nullable = false,
        minLength = 3,
        maxLength = 50
    )
    @NotBlank(message = "Le nom d'utilisateur est requis")
    @Size(min = 3, max = 50, message = "Le nom d'utilisateur doit contenir entre 3 et 50 caractères")
    private String username;

    @Schema(
        description = "User's email address",
        example = "john.doe@example.com",
        nullable = false
    )
    @NotBlank(message = "L'email est requis")
    @Email(message = "L'email doit être valide")
    private String email;

    @Schema(
        description = "User's password (minimum 12 characters)",
        example = "MyS3cur3P@ssw0rd!",
        nullable = false,
        minLength = 12
    )
    @NotBlank(message = "Le mot de passe est requis")
    @Size(min = 12, message = "Le mot de passe doit contenir au moins 12 caractères")
    private String password;

    @Schema(
        description = "User role (default: ADMINISTRATOR)",
        example = "ADMINISTRATOR",
        defaultValue = "ADMINISTRATOR"
    )
    private Role role = Role.ADMINISTRATOR;
}