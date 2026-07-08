package com.kanban.kanbanapp.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.kanban.kanbanapp.Data_Transfer_Object.TaskCreateRequest;
import com.kanban.kanbanapp.Model.Task;
import com.kanban.kanbanapp.service.TaskService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Task Controller", description = "APIs for managing tasks within Kanban columns")
@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
@Validated
@SecurityRequirement(name = "bearerAuth")
public class TaskController {

    private final TaskService taskService;

    /**
     * Create a new task and assign it to a specific column. The columnId must be provided in the request body, and the task will be created with a taskOrder based on the current tasks in the column.
     *
     * @param request the task creation request
     * @return the created task
     */
    @Operation(
        summary = "Create a new task",
        description = "Create a task and assign it to a specific column. Returns the created task with generated ID and taskOrder."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Task created successfully",
            content = @Content(schema = @Schema(implementation = Task.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input (missing required fields or validation errors)"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - JWT token missing or invalid"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Column not found"
        )
    })
    @PostMapping
    public ResponseEntity<Task> createTask(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Task details",
            required = true,
            content = @Content(schema = @Schema(implementation = TaskCreateRequest.class))
        )
        @Valid @RequestBody @NonNull TaskCreateRequest request
    ) {
        Task savedTask = taskService.createTask(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedTask);
    }

    /**
     * Retrieve a single task by its unique identifier.
     * @param id the task ID
     * @return the task with the specified ID, or 404 if not found
     */
    @Operation(
        summary = "Get task by ID",
        description = "Retrieve a single task by its unique identifier"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Task found",
            content = @Content(schema = @Schema(implementation = Task.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - JWT token missing or invalid"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Task not found"
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<Task> getTaskById(
        @Parameter(description = "Task ID", example = "task-123", required = true)
        @PathVariable @NonNull String id
    ) {
        return ResponseEntity.ok(taskService.getTaskById(id));
    }

    /**
     * Retrieve all tasks belonging to a specific column, ordered by their taskOrder (position in the column).
     * @param columnId the ID of the column
     * @return list of tasks in the specified column, or 404 if the column is not found
     */
    @Operation(
        summary = "Get tasks by column",
        description = "Retrieve all tasks belonging to a specific column, ordered by taskOrder (drag & drop position)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "List of tasks retrieved successfully"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - JWT token missing or invalid"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Column not found"
        )
    })
    @GetMapping("/column/{columnId}")
    public ResponseEntity<List<Task>> getTasksByColumn(
        @Parameter(description = "Column ID", example = "col-123", required = true)
        @PathVariable @NonNull String columnId
    ) {
        return ResponseEntity.ok(taskService.getTasksByColumn(columnId));
    }

    /**
     * Update an existing task by its ID. All fields provided in the request will replace the existing values of the task.
     * Fields that are not included in the request will be set to null or default values, so the client must send all necessary fields.
     *
     * @param id the task ID
     * @param request the updated task details
     * @return the updated task
     */
    @Operation(
        summary = "Update task (full replacement)",
        description = "Update all fields of an existing task. All fields from the request will replace the existing values."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Task updated successfully",
            content = @Content(schema = @Schema(implementation = Task.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - JWT token missing or invalid"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Task not found"
        )
    })
    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(
        @Parameter(description = "Task ID", example = "task-123", required = true)
        @PathVariable @NonNull String id,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Updated task details",
            required = true
        )
        @Valid @RequestBody @NonNull TaskCreateRequest request
    ) {
        return ResponseEntity.ok(taskService.updateTask(id, request));
    }

    /**
     * Permanently delete a task by its ID. This action cannot be undone.
     * @param id the task ID
     * @return 204 if the task was deleted successfully
     */
    @Operation(
        summary = "Delete task",
        description = "Permanently delete a task by its ID. This action cannot be undone."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Task deleted successfully (no content returned)"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - JWT token missing or invalid"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Task not found"
        )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(
        @Parameter(description = "Task ID", example = "task-123", required = true)
        @PathVariable @NonNull String id
    ) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Partially update an existing task. Only the fields provided in the request will be updated, while others will remain unchanged.
     * @param id the task ID
     * @param request the fields to update
     * @return the updated task
     */
    @Operation(
        summary = "Patch task (partial update)",
        description = "Partially update an existing task. Only provided fields will be updated."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Task updated successfully",
            content = @Content(schema = @Schema(implementation = Task.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - JWT token missing or invalid"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Task not found"
        )
    })
    @PatchMapping("/{id}")
    public ResponseEntity<Task> patchTask(
        @Parameter(description = "Task ID", example = "task-123", required = true)
        @PathVariable @NonNull String id,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Fields to update",
            required = true
        )
        @Valid @RequestBody @NonNull TaskCreateRequest request
    ) {
        return ResponseEntity.ok(taskService.updateTask(id, request));
    }
}