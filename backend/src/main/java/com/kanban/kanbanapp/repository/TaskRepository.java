package com.kanban.kanbanapp.repository;

import java.util.List;

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
}