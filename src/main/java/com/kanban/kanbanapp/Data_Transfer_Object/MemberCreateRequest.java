package com.kanban.kanbanapp.Data_Transfer_Object;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MemberCreateRequest {
    @NotBlank(message = "Member email is required")
    @Email(message = "Must be a valid email address")
    private String memberEmail;
    
    @NotBlank(message = "Role is required")
    private String role;

    private Integer memberOrder; 
    
    @NotBlank(message = "Board ID is required")
    private String boardId;
}
