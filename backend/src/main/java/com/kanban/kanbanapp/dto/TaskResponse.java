package com.kanban.kanbanapp.dto;

import com.kanban.kanbanapp.Model.Task;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/** Plain task data, built inside the transaction without exposing Hibernate proxies. */
public record TaskResponse(
        String id, String name, String description, Integer taskOrder,
        LocalDate date, LocalTime time, List<String> colors, List<String> labels,
        List<MemberResponse> members, String columnId,
        LocalDateTime createdAt, LocalDateTime updatedAt) {

    public record MemberResponse(String id, String memberEmail, String role, Integer memberOrder) {}

    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(), task.getName(), task.getDescription(), task.getTaskOrder(),
                task.getDate(), task.getTime(), List.copyOf(task.getColors()),
                List.copyOf(task.getLabels()),
                task.getMembers().stream().map(member -> new MemberResponse(
                        member.getId(), member.getMemberEmail(),
                        member.getRole() == null ? null : member.getRole().name(),
                        member.getMemberOrder())).toList(),
                task.getColumnId(), task.getCreatedAt(), task.getUpdatedAt());
    }
}
