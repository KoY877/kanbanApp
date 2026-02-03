package com.kanban.kanbanapp.Data_Transfer_Object;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserCreateRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "The username must contain between 3 and 50 characters.")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "The email address is not valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 16, message = "The password must contain at least 16 characters")
    private String password;
}
