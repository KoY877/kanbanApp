package com.kanban.kanbanapp.Data_Transfer_Object;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class PasswordManagement {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChangePasswordRequest {
        @NotBlank(message = "Current password is required")
        private String currentPassword;
        
        @NotBlank(message = "New password is required")
        @Size(min = 12, message = "Password must be at least 12 characters")
        private String newPassword;
    }

    // ForgotPasswordRequest.java
    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ForgotPasswordRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Must be valid email")
        private String email;
    }

    // ResetPasswordRequest.java
    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResetPasswordRequest {
        @NotBlank(message = "Token is required")
        private String token;
        
        @NotBlank(message = "New password is required")
        @Size(min = 12, message = "Password must be at least 12 characters")
        private String newPassword;
    }

}