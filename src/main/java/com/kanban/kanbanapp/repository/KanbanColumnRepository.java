package com.kanban.kanbanapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kanban.kanbanapp.Model.KanbanColumn;

public interface KanbanColumnRepository extends JpaRepository<KanbanColumn, String> {
    
}
