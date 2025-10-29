package com.kanban.kanbanapp.repository;

import org.springframework.data.repository.CrudRepository;

import com.kanban.kanbanapp.Model.Board;

public interface BoardRepository extends CrudRepository<Board, String> {
    
}
