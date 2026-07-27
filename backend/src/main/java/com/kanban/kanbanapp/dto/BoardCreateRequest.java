package com.kanban.kanbanapp.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request DTO for creating a new board.
 *
 * @param name           the board name
 * @param selectedTask   the default task template
 * @param globalOption   the global board option
 * @param userId         the id of the board owner
 * @param columns        the initial column names
 * @param added_columns  dynamically added columns with WIP limits
 * @param members        the board members with their roles
 */
@Schema(description = "Request DTO for creating a new board")
public record BoardCreateRequest(
        @Schema(description = "Board name", example = "Sprint 2024-Q2", nullable = false) String name,

        @Schema(description = "Default task template", example = "User Story") String selectedTask,

        @Schema(description = "Global board option", example = "Agile") String globalOption,

        @Schema(description = "User ID of the board owner", example = "user-123", nullable = false) String userId,

        @Schema(description = "List of initial column names") List<ColumnNameDto> columns,

        @Schema(description = "List of dynamically added columns with WIP limits") List<AddedColumnDto> added_columns,

        @Schema(description = "List of board members with their roles") List<MemberDto> members) {

    /**
     * @param columnName the column name
     */
    @Schema(description = "Column name DTO")
    public record ColumnNameDto(@Schema(description = "Column name", example = "To Do") String columnName) {
    }

    /**
     * @param columnName          the column name
     * @param limitWorkInProgress the WIP limit
     */
    @Schema(description = "Added column with WIP limit")
    public record AddedColumnDto(
            @Schema(description = "Column name", example = "In Review") String columnName,
            @Schema(description = "Work In Progress limit", example = "3") Integer limitWorkInProgress) {
    }

    /**
     * @param memberEmail the member's email
     * @param role        the member's role
     */
    @Schema(description = "Board member DTO")
    public record MemberDto(
            @Schema(description = "Member email", example = "member@example.com") String memberEmail,
            @Schema(description = "Member role", example = "VIEWER") String role) {
    }
}
