package com.kanban.kanbanapp.controller;

import com.kanban.kanbanapp.Model.Board;
import com.kanban.kanbanapp.Model.User;
import com.kanban.kanbanapp.repository.BoardRepository;
import com.kanban.kanbanapp.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Board Controller", description = "APIs for managing boards")
@RestController
@RequestMapping("/boards")
@RequiredArgsConstructor
public class BoardController {

    private final BoardRepository boardRepository;
    private final UserRepository userRepository;

    /**
     * Get authenticated user from JWT token
     */
    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @PreAuthorize("hasRole('ADMINISTRATOR')") // Ensure only administrators can access this endpoint
    @Operation(summary = "Get all boards", description = "Retrieve all boards (admin only)")
    @GetMapping
    public ResponseEntity<Iterable<Board>> getAllBoards() {
        Iterable<Board> boards = boardRepository.findAll();
        return ResponseEntity.ok(boards);
    }

    @PreAuthorize("hasRole('STANDARD') or hasRole('ADMINISTRATOR')") // Ensure only members and administrators can access this endpoint
    @Operation(summary = "Get user's boards", description = "Retrieve all boards for the authenticated user")
    @GetMapping("/all")
    public ResponseEntity<Iterable<Board>> getUserBoards() {
        User user = getAuthenticatedUser();
        Iterable<Board> boards = boardRepository.findAllByUserId(user.getId());
        return ResponseEntity.ok(boards);
    }

    @PreAuthorize("hasRole('STANDARD') or hasRole('ADMINISTRATOR')") // Ensure only members and administrators can access this endpoint
    @Operation(summary = "Get board by ID", description = "Retrieve a specific board by its ID")
    @GetMapping("/{id}")
    public ResponseEntity<Board> getBoardById(@PathVariable String id) {
        User user = getAuthenticatedUser();
        
        return boardRepository.findById(id)
                .filter(board -> board.getUser().getId().equals(user.getId()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
    }

    @PreAuthorize("hasRole('STANDARD') or hasRole('ADMINISTRATOR')") // Ensure only members and administrators can access this endpoint
    @Operation(summary = "Create a new board", description = "Create a new board for the authenticated user")
    @PostMapping
    public ResponseEntity<Board> createBoard(@RequestBody Board board) {
        User user = getAuthenticatedUser();
        
        board.setUser(user);
        Board savedBoard = boardRepository.save(board);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(savedBoard);
    }

    @PreAuthorize("hasRole('STANDARD') or hasRole('ADMINISTRATOR')") // Ensure only members and administrators can access this endpoint
    @Operation(summary = "Update a board", description = "Update an existing board")
    @PutMapping("/{id}")
    public ResponseEntity<Board> updateBoard(
            @PathVariable String id,
            @RequestBody Board boardDetails) {
        
        User user = getAuthenticatedUser();
        
        return boardRepository.findById(id)
                .filter(board -> board.getUser().getId().equals(user.getId()))
                .map(board -> {
                    board.setName(boardDetails.getName());
                    board.setDescription(boardDetails.getDescription());
                    Board updatedBoard = boardRepository.save(board);
                    return ResponseEntity.ok(updatedBoard);
                })
                .orElse(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
    }

    @PreAuthorize("hasRole('STANDARD') or hasRole('ADMINISTRATOR')") // Ensure only members and administrators can access this endpoint
    @Operation(summary = "Delete a board", description = "Delete a board by its ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBoard(@PathVariable String id) {
        User user = getAuthenticatedUser();
        
        return boardRepository.findById(id)
                .filter(board -> board.getUser().getId().equals(user.getId()))
                .map(board -> {
                    boardRepository.delete(board);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
    }
}