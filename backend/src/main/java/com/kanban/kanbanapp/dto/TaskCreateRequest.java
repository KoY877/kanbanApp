package com.kanban.kanbanapp.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for creating or updating a task.
 *
 * @param name        the task name/title
 * @param colors      colors/tags associated with the task, may be null
 * @param members     ids of the members assigned to the task, may be null
 * @param labels      labels/tags associated with the task, may be null
 * @param description a detailed description of the task
 * @param date        the task's due date
 * @param time        the task's due time
 * @param columnId    the id of the column the task belongs to
 */
@Schema(description = "Request DTO for creating and updating a task")
public record TaskCreateRequest(
        @Schema(description = "Task name/title", example = "Implement user authentication", nullable = false, maxLength = 255) @NotBlank(message = "Task name is required") String name,

        @Schema(description = "List of colors/tags associated with the task", example = "[\"#FF5733\", \"#33FF57\"]") List<String> colors,

        @Schema(description = "List of member emails assigned to the task", example = "[\"user1@example.com\", \"user2@example.com\"]") List<String> members,

        @Schema(description = "List of labels/tags associated with the task", example = "[\"Urgent\", \"Backend\"]") List<String> labels,

        @Schema(description = "Detailed description of the task", example = "Set up JWT authentication with retfresh tokens stored in httpOnly cookies. ") String description,

        @Schema(description = "Due date of the task", example = "2026-05-15") LocalDate date,

        @Schema(description = "Due time of the task", example = "17:00:00") LocalTime time,

        @Schema(description = "ID of the column to which the task belongs", example = "column-12345", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull(message = "Column id is required") String columnId) {
}
