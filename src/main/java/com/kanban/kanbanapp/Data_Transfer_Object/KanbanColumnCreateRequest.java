package com.kanban.kanbanapp.Data_Transfer_Object;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor
public class KanbanColumnCreateRequest {
    @NotBlank(message = "Column name is required")
    @Size(min = 1, max = 50, message = "Column name must be between 1 and 50 characters")
    private String columnName;
    
    @Min(value = 1, message = "WIP limit must be at least 1")
    @Max(value = 100, message = "WIP limit cannot exceed 100")
    private Integer limitWorkInProgress;
    
    @NotBlank(message = "Board ID is required")
    private String boardId;
}
