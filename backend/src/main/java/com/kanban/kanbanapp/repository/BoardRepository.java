package com.kanban.kanbanapp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kanban.kanbanapp.Model.Board;
import com.kanban.kanbanapp.Model.User;

public interface BoardRepository extends JpaRepository<Board, String> {

    /**
     * Find all boards for a specific user using the User entity navigation
     * JPA will navigate through the 'user' relationship to access 'user.id'
     */
    List<Board> findAllByUser_Id(String userId);

    /**
     * Alternative: Find all boards for a specific user using the User object
     */
    List<Board> findAllByUser(User user);

    /**
     * Alternative: Using explicit JPQL query (most explicit and clear)
     */
    @Query("SELECT b FROM Board b WHERE b.user.id = :userId")
    List<Board> findBoardsByUserId(@Param("userId") String userId);

    /**
     * Find a board by ID that belongs to a specific user.
     * Performs the ownership check at the query level to avoid lazy proxy issues.
     */
    @Query("SELECT b FROM Board b WHERE b.id = :id AND b.user.id = :userId")
    Optional<Board> findByIdAndUserId(@Param("id") String id, @Param("userId") String userId);
}
