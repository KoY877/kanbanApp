package com.kanban.kanbanapp.Data_Transfer_Object;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Request DTO for creating and updating a task")
public class TaskCreateRequest {

    @Schema(
        description = "Task name/title", 
        example = "Implement user authentication", 
        nullable = false,
        maxLength = 255
    )
    @NotBlank(message = "Task name is required")
    private String name;

    @Schema(
        description = "List of colors/tags associated with the task", 
        example = "[\"#FF5733\", \"#33FF57\"]"
    )
    private List<String> colors = new ArrayList<>();

    @Schema(
        description = "List of member emails assigned to the task", 
        example = "[\"user1@example.com\", \"user2@example.com\"]"
    )
    private List<String> members = new ArrayList<>();

    @Schema(
        description = "List of labels/tags associated with the task", 
        example = "[\"Urgent\", \"Backend\"]"
    )
    private List<String> labels = new ArrayList<>();

    @Schema(
        description = "Detailed description of the task", 
        example = "Set up JWT authentication with retfresh tokens stored in httpOnly cookies. "
    )
    private String description;

    @Schema(
        description = "Due date of the task", 
        example = "2026-05-15"
    )
    private LocalDate date;

    @Schema(
        description = "Due time of the task", 
        example = "17:00:00"
    )
    private LocalTime time;

    @Schema(
        description = "ID of the column to which the task belongs", 
        example = "column-12345", 
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Column id is required")
    private String columnId;
}