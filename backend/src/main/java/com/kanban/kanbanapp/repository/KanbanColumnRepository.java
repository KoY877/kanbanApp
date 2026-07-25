package com.kanban.kanbanapp.repository;

import java.util.Optional;

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

    /**
     * Find a column by id, scoped to the board owner. Used to enforce
     * object-level authorization so a user cannot access another user's column.
     *
     * @param id     the column id
     * @param userId the id of the user who must own the column's board
     * @return the matching column, or empty if not found or not owned by the user
     */
    Optional<KanbanColumn> findByIdAndBoard_User_Id(String id, String userId);

    /**
     * Find all columns belonging to boards owned by the given user.
     *
     * @param userId the id of the board owner
     * @return the columns of all boards owned by the user
     */
    java.util.List<KanbanColumn> findAllByBoard_User_Id(String userId);
}
