package com.kanban.kanbanapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kanban.kanbanapp.Model.Board;

public interface BoardRepository extends JpaRepository<Board, String> {
    
}
