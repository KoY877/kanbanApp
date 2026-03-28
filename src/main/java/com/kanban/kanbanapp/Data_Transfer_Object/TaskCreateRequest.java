package com.kanban.kanbanapp.Data_Transfer_Object;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskCreateRequest {

     @NotBlank(message = "Task name is required")
    private String name;

    private List<String> colors = new ArrayList<>();

    private List<String> members = new ArrayList<>();

    private List<String> labels = new ArrayList<>();

    private String description;

    private LocalDate date;

    private LocalTime time;

    @NotNull(message = "Column id is required")
    private String columnId;
}