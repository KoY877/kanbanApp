package com.kanban.kanbanapp.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record TaskMoveRequest(@NotBlank String columnId, @Min(0) int position) {}
