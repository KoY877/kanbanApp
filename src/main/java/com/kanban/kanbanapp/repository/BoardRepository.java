package com.kanban.kanbanapp.repository;

import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kanban.kanbanapp.Model.Board;

public interface BoardRepository extends JpaRepository<Board, String> {
    Set<Board> findAllByUserId(String userId);
}
