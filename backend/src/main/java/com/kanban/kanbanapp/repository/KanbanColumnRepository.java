package com.kanban.kanbanapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kanban.kanbanapp.Model.KanbanColumn;

public interface KanbanColumnRepository extends JpaRepository<KanbanColumn, String> {

    /**
     * Count the columns belonging to a board.
     *
     * @param boardId the board id
     * @return the number of columns in the board
     */
    long countByBoardId(String boardId);
}
