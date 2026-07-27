package com.kanban.kanbanapp.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating or updating a Kanban column.
 *
 * @param columnName          the column name (1-50 characters)
 * @param limitWorkInProgress the WIP limit (1-100), or null for no limit
 * @param boardId             the id of the board the column belongs to
 */
public record KanbanColumnCreateRequest(
        @NotBlank(message = "Column name is required") @Size(min = 1, max = 50, message = "Column name must be between 1 and 50 characters") String columnName,

        @Min(value = 1, message = "WIP limit must be at least 1") @Max(value = 100, message = "WIP limit cannot exceed 100") Integer limitWorkInProgress,

        @NotBlank(message = "Board ID is required") String boardId) {
}
