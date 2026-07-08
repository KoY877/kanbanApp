package com.kanban.kanbanapp.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kanban.kanbanapp.Data_Transfer_Object.TaskCreateRequest;
import com.kanban.kanbanapp.Model.Board;
import com.kanban.kanbanapp.Model.KanbanColumn;
import com.kanban.kanbanapp.Model.Member;
import com.kanban.kanbanapp.Model.Task;
import com.kanban.kanbanapp.repository.KanbanColumnRepository;
import com.kanban.kanbanapp.repository.MemberRepository;
import com.kanban.kanbanapp.repository.TaskRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final KanbanColumnRepository kanbanColumnRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public Task createTask(@NonNull TaskCreateRequest request) {
        String columnId = java.util.Objects.requireNonNull(request.getColumnId(), "Column ID cannot be null");
        KanbanColumn column = kanbanColumnRepository.findById(columnId)
            .orElseThrow(() -> new RuntimeException("Column not found"));

        Board board = column.getBoard();

        List<Member> assignedMembers = resolveMembers(request.getMembers(), board);

        Task task = new Task();
        task.setName(request.getName().trim());
        task.setDescription(request.getDescription());
        task.setDate(request.getDate());
        task.setTime(request.getTime());
        task.setColors(safeList(request.getColors()));
        task.setLabels(safeList(request.getLabels()));
        task.setMembers(assignedMembers);
        task.setColumn(column);

        int nextOrder = taskRepository.findAllByColumn_IdOrderByTaskOrderAsc(column.getId()).size();
        task.setTaskOrder(nextOrder);

        return taskRepository.save(task);
    }

    @Transactional(readOnly = true)
    public List<Task> getTasksByColumn(@NonNull String columnId) {
        String validColumnId = java.util.Objects.requireNonNull(columnId, "Column ID cannot be null");
        return taskRepository.findAllByColumn_IdOrderByTaskOrderAsc(validColumnId);
    }

    @Transactional(readOnly = true)
    public Task getTaskById(@NonNull String taskId) {
        String validTaskId = java.util.Objects.requireNonNull(taskId, "Task ID cannot be null");
        return taskRepository.findById(validTaskId)
            .orElseThrow(() -> new RuntimeException("Task not found"));
    }

    @Transactional
    public void deleteTask(@NonNull String taskId) {
        String validTaskId = java.util.Objects.requireNonNull(taskId, "Task ID cannot be null");
        Task task = taskRepository.findById(validTaskId)
            .orElseThrow(() -> new RuntimeException("Task not found"));

        String columnId = task.getColumn().getId();

        taskRepository.delete(task);

        reorderColumnTasks(columnId);
    }

    @Transactional
    public Task updateTask(@NonNull String taskId, @NonNull TaskCreateRequest request) {
        String validTaskId = java.util.Objects.requireNonNull(taskId, "Task ID cannot be null");
        Task task = taskRepository.findById(validTaskId)
            .orElseThrow(() -> new RuntimeException("Task not found"));

        String columnId = java.util.Objects.requireNonNull(request.getColumnId(), "Column ID cannot be null");
        KanbanColumn targetColumn = kanbanColumnRepository.findById(columnId)
            .orElseThrow(() -> new RuntimeException("Column not found"));

        Board board = targetColumn.getBoard();
        List<Member> assignedMembers = resolveMembers(request.getMembers(), board);

        String oldColumnId = task.getColumn().getId();
        String newColumnId = targetColumn.getId();

        task.setName(request.getName().trim());
        task.setDescription(request.getDescription());
        task.setDate(request.getDate());
        task.setTime(request.getTime());
        task.setColors(safeList(request.getColors()));
        task.setLabels(safeList(request.getLabels()));
        task.setMembers(assignedMembers);

        if (!oldColumnId.equals(newColumnId)) {
            task.setColumn(targetColumn);
            int nextOrder = taskRepository.findAllByColumn_IdOrderByTaskOrderAsc(newColumnId).size();
            task.setTaskOrder(nextOrder);
        }

        Task savedTask = taskRepository.save(task);

        reorderColumnTasks(oldColumnId);
        if (!oldColumnId.equals(newColumnId)) {
            reorderColumnTasks(newColumnId);
        }

        return savedTask;
    }

    // Helper methods
    private List<Member> resolveMembers(List<String> memberIds, Board board) {
        if (memberIds == null || memberIds.isEmpty()) {
            return new ArrayList<>();
        }

        // Validate that all member IDs exist and belong to the board
        List<Member> members = memberRepository.findAllById(memberIds);
        if (members.size() != memberIds.size()) {
            throw new RuntimeException("One or more members not found");
        }

        for (Member member : members) {
            // board_id FK is already loaded with the member entity — no lazy load needed
            if (!member.getBoard().getId().equals(board.getId())) {
                throw new RuntimeException("Assigned member does not belong to this board");
            }
        }

        return members;
    }

    private void reorderColumnTasks(String columnId) {
        List<Task> tasks = taskRepository.findAllByColumn_IdOrderByTaskOrderAsc(columnId);
        for (int index = 0; index < tasks.size(); index++) {
            tasks.get(index).setTaskOrder(index);
        }
        taskRepository.saveAll(tasks);
    }

    private List<String> safeList(List<String> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }
}