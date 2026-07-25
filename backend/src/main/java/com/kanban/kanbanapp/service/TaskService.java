package com.kanban.kanbanapp.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kanban.kanbanapp.Data_Transfer_Object.TaskCreateRequest;
import com.kanban.kanbanapp.Model.Board;
import com.kanban.kanbanapp.Model.KanbanColumn;
import com.kanban.kanbanapp.Model.Member;
import com.kanban.kanbanapp.Model.Task;
import com.kanban.kanbanapp.exception.WipLimitExceededException;
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

    /**
     * Create a new task in the given column, appending it at the end of the
     * column's task order.
     *
     * @param request task creation payload (name, description, columnId, members...)
     * @param userId  the id of the authenticated user; must own the target column's board
     * @return the created and persisted task
     * @throws RuntimeException          if the target column is not found, or if a
     *                                   member id is invalid or does not belong to the
     *                                   column's board
     * @throws AccessDeniedException     if the target column's board is not owned by userId
     * @throws WipLimitExceededException if the column is already at its WIP limit
     */
    @Transactional
    public Task createTask(@NonNull TaskCreateRequest request, @NonNull String userId) {
        String columnId = java.util.Objects.requireNonNull(request.getColumnId(), "Column ID cannot be null");
        KanbanColumn column = kanbanColumnRepository.findByIdAndBoard_User_Id(columnId, userId)
            .orElseThrow(() -> new AccessDeniedException("Column not found"));

        Board board = column.getBoard();

        List<Member> assignedMembers = resolveMembers(request.getMembers(), board);

        int currentCount = taskRepository.findAllByColumn_IdOrderByTaskOrderAsc(column.getId()).size();
        checkWipLimit(column, currentCount);

        Task task = new Task();
        task.setName(request.getName().trim());
        task.setDescription(request.getDescription());
        task.setDate(request.getDate());
        task.setTime(request.getTime());
        task.setColors(safeList(request.getColors()));
        task.setLabels(safeList(request.getLabels()));
        task.setMembers(assignedMembers);
        task.setColumn(column);
        task.setTaskOrder(currentCount);

        return taskRepository.save(task);
    }

    /**
     * Reject the operation if adding one more task would push the column
     * past its configured WIP limit.
     *
     * @param column       the target column
     * @param currentCount the column's task count before adding the new task
     * @throws WipLimitExceededException if the column is already at its WIP limit
     */
    private void checkWipLimit(KanbanColumn column, int currentCount) {
        Integer limit = column.getLimitWorkInProgress();
        if (limit != null && currentCount >= limit) {
            throw new WipLimitExceededException(
                "Column \"" + column.getColumnName() + "\" has reached its WIP limit of " + limit);
        }
    }

    /**
     * Retrieve all tasks of a column, ordered by their position (taskOrder).
     *
     * @param columnId the column id
     * @param userId   the id of the authenticated user; must own the column's board
     * @return the ordered list of tasks in the column
     * @throws AccessDeniedException if the column is not found or not owned by userId
     */
    @Transactional(readOnly = true)
    public List<Task> getTasksByColumn(@NonNull String columnId, @NonNull String userId) {
        String validColumnId = java.util.Objects.requireNonNull(columnId, "Column ID cannot be null");
        kanbanColumnRepository.findByIdAndBoard_User_Id(validColumnId, userId)
            .orElseThrow(() -> new AccessDeniedException("Column not found"));
        return taskRepository.findAllByColumn_IdOrderByTaskOrderAsc(validColumnId);
    }

    /**
     * Retrieve a single task by its id.
     *
     * @param taskId the task id
     * @param userId the id of the authenticated user; must own the task's board
     * @return the matching task
     * @throws AccessDeniedException if no task exists with the given id, or it is
     *                               not owned by userId
     */
    @Transactional(readOnly = true)
    public Task getTaskById(@NonNull String taskId, @NonNull String userId) {
        String validTaskId = java.util.Objects.requireNonNull(taskId, "Task ID cannot be null");
        return taskRepository.findByIdAndColumn_Board_User_Id(validTaskId, userId)
            .orElseThrow(() -> new AccessDeniedException("Task not found"));
    }

    /**
     * Delete a task by its id and reorder the remaining tasks in its column.
     *
     * @param taskId the task id
     * @param userId the id of the authenticated user; must own the task's board
     * @throws AccessDeniedException if no task exists with the given id, or it is
     *                               not owned by userId
     */
    @Transactional
    public void deleteTask(@NonNull String taskId, @NonNull String userId) {
        String validTaskId = java.util.Objects.requireNonNull(taskId, "Task ID cannot be null");
        Task task = taskRepository.findByIdAndColumn_Board_User_Id(validTaskId, userId)
            .orElseThrow(() -> new AccessDeniedException("Task not found"));

        String columnId = task.getColumn().getId();

        taskRepository.delete(task);

        reorderColumnTasks(columnId);
    }

    /**
     * Update a task, replacing all its fields. If the task moves to a
     * different column, both the source and target columns are reordered.
     *
     * @param taskId  the task id
     * @param request the new task data
     * @param userId  the id of the authenticated user; must own both the task's
     *                current board and the target column's board
     * @return the updated task
     * @throws AccessDeniedException     if the task or target column is not found, or
     *                                   either is not owned by userId
     * @throws RuntimeException          if a member id is invalid or does not belong
     *                                   to the target column's board
     * @throws WipLimitExceededException if moving the task into a different
     *                                   column would exceed that column's WIP limit
     */
    @Transactional
    public Task updateTask(@NonNull String taskId, @NonNull TaskCreateRequest request, @NonNull String userId) {
        String validTaskId = java.util.Objects.requireNonNull(taskId, "Task ID cannot be null");
        Task task = taskRepository.findByIdAndColumn_Board_User_Id(validTaskId, userId)
            .orElseThrow(() -> new AccessDeniedException("Task not found"));

        String columnId = java.util.Objects.requireNonNull(request.getColumnId(), "Column ID cannot be null");
        KanbanColumn targetColumn = kanbanColumnRepository.findByIdAndBoard_User_Id(columnId, userId)
            .orElseThrow(() -> new AccessDeniedException("Column not found"));

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
            int currentCount = taskRepository.findAllByColumn_IdOrderByTaskOrderAsc(newColumnId).size();
            checkWipLimit(targetColumn, currentCount);

            task.setColumn(targetColumn);
            task.setTaskOrder(currentCount);
        }

        Task savedTask = taskRepository.save(task);

        reorderColumnTasks(oldColumnId);
        if (!oldColumnId.equals(newColumnId)) {
            reorderColumnTasks(newColumnId);
        }

        return savedTask;
    }

    /**
     * Resolve and validate the members assigned to a task.
     *
     * @param memberIds ids of the members to assign, may be null or empty
     * @param board     the board the task's column belongs to
     * @return the resolved members, or an empty list if memberIds is null/empty
     * @throws RuntimeException if a member id does not exist or does not
     *                          belong to the given board
     */
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

    /**
     * Re-sequence the taskOrder of all tasks in a column to be contiguous
     * starting from 0, based on their current relative order.
     *
     * @param columnId the column id
     */
    private void reorderColumnTasks(String columnId) {
        List<Task> tasks = taskRepository.findAllByColumn_IdOrderByTaskOrderAsc(columnId);
        for (int index = 0; index < tasks.size(); index++) {
            tasks.get(index).setTaskOrder(index);
        }
        taskRepository.saveAll(tasks);
    }

    /**
     * Return a mutable copy of the given list, or an empty list if null.
     *
     * @param values the source list, may be null
     * @return a non-null mutable list
     */
    private List<String> safeList(List<String> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }
}