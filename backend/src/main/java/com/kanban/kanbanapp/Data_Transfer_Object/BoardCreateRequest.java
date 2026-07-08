package com.kanban.kanbanapp.Data_Transfer_Object;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request DTO for creating a new board")
public class BoardCreateRequest {
    
    @Schema(description = "Board name", example = "Sprint 2024-Q2", required = true)
    private String name;
    
    @Schema(description = "Default task template", example = "User Story")
    private String selectedTask;
    
    @Schema(description = "Global board option", example = "Agile")
    private String globalOption;
    
    @Schema(description = "User ID of the board owner", example = "user-123", required = true)
    private String userId;
    
    @Schema(description = "List of initial column names")
    private List<ColumnNameDto> columns;
    
    @Schema(description = "List of dynamically added columns with WIP limits")
    private List<AddedColumnDto> added_columns;
    
    @Schema(description = "List of board members with their roles")
    private List<MemberDto> members;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Column name DTO")
    public static class ColumnNameDto {
        @Schema(description = "Column name", example = "To Do")
        private String columnName;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Added column with WIP limit")
    public static class AddedColumnDto {
        @Schema(description = "Column name", example = "In Review")
        private String columnName;
        
        @Schema(description = "Work In Progress limit", example = "3")
        private Integer limitWorkInProgress;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Board member DTO")
    public static class MemberDto {
        @Schema(description = "Member email", example = "member@example.com")
        private String memberEmail;
        
        @Schema(description = "Member role", example = "VIEWER")
        private String role;
    }
}