package com.kanban.kanbanapp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kanban.kanbanapp.Model.Task;

public interface TaskRepository extends JpaRepository<Task, String> {
    List<Task> findAllByColumnIdOrderByTaskOrderAsc(String columnId);
}