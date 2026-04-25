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
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Task Controller", description = "APIs for managing tasks")
@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
@Validated
public class TaskController {

    private final TaskService taskService;

    
    @Operation(summary = "Create task", description = "Create a task from form data")
    @PostMapping
    public ResponseEntity<Task> createTask(@Valid @RequestBody @NonNull TaskCreateRequest request) {
        Task savedTask = taskService.createTask(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedTask);
    }

    @Operation(summary = "Get task by id", description = "Retrieve one task by id")
    @GetMapping("/{id}")
    public ResponseEntity<Task> getTaskById(@PathVariable @NonNull String id) {
        return ResponseEntity.ok(taskService.getTaskById(id));
    }

    @Operation(summary = "Get tasks by column", description = "Retrieve tasks ordered by taskOrder for a column")
    @GetMapping("/column/{columnId}")
    public ResponseEntity<List<Task>> getTasksByColumn(@PathVariable @NonNull String columnId) {
        return ResponseEntity.ok(taskService.getTasksByColumn(columnId));
    }


    @Operation(summary = "Update task", description = "Update an existing task")
    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(
        @PathVariable @NonNull String id,
        @Valid @RequestBody @NonNull TaskCreateRequest request
    ) {
        return ResponseEntity.ok(taskService.updateTask(id, request));
    }


    @Operation(summary = "Delete task", description = "Delete a task by id")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable @NonNull String id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Patch task", description = "Partially update an existing task")
    @PatchMapping("/{id}")
    public ResponseEntity<Task> patchTask(
        @PathVariable @NonNull String id,
        @Valid @RequestBody @NonNull TaskCreateRequest request
    ) {
        return ResponseEntity.ok(taskService.updateTask(id, request));
    }
}