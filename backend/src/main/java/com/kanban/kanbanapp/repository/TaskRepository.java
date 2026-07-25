package com.kanban.kanbanapp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kanban.kanbanapp.Model.Task;

public interface TaskRepository extends JpaRepository<Task, String> {

    /**
     * Find all tasks of a column, ordered by their position.
     *
     * @param columnId the column id
     * @return the ordered list of tasks
     */
    List<Task> findAllByColumn_IdOrderByTaskOrderAsc(String columnId);

    /**
     * Find a task by id, scoped to the board owner. Used to enforce
     * object-level authorization so a user cannot access another user's task.
     *
     * @param id     the task id
     * @param userId the id of the user who must own the task's board
     * @return the matching task, or empty if not found or not owned by the user
     */
    Optional<Task> findByIdAndColumn_Board_User_Id(String id, String userId);
}