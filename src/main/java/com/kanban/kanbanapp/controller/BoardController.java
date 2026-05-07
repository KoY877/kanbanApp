package com.kanban.kanbanapp.controller;

import com.kanban.kanbanapp.Model.Board;
import com.kanban.kanbanapp.Model.User;
import com.kanban.kanbanapp.dto.BoardDetailResponse;
import com.kanban.kanbanapp.dto.BoardResponse;
import com.kanban.kanbanapp.repository.BoardRepository;
import com.kanban.kanbanapp.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Board Controller", description = "APIs for managing boards")
@RestController
@RequestMapping("/boards")
@RequiredArgsConstructor
public class BoardController {

    private final BoardRepository boardRepository;
    private final UserRepository userRepository;

    /**
     * Get authenticated user from JWT token
     * The JwtAuthenticationFilter sets the email (String) as the principal, not a
     * UserDetails object
     */
    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }

        String email = authentication.getName();
        System.out.println("DEBUG - Attempting to find user with email: " + email);

        if (email == null || email.equals("anonymousUser")) {
            throw new RuntimeException("No valid user principal found");
        }

        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    System.err.println("ERROR - User not found in database with email: " + email);
                    return new RuntimeException("User not found with email: " + email);
                });
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get user's boards", description = "Retrieve all boards for the authenticated user")
    /**
     * Retrieve all boards belonging to the authenticated user.
     *
     * @return 200 with the list of boards
     */
    @GetMapping("/all")
    public ResponseEntity<List<Board>> getUserBoards() {
        try {
            System.out.println("DEBUG - GET /boards/all called");
            User user = getAuthenticatedUser();
            System.out.println("DEBUG - User found: " + user.getEmail() + " (ID: " + user.getId() + ")");

            // Use the corrected repository method that navigates through the User
            // relationship
            List<Board> boards = boardRepository.findAllByUser_Id(user.getId());
            System.out.println("DEBUG - Found " + boards.size() + " boards for user");

            return ResponseEntity.ok(boards);
        } catch (Exception e) {
            System.err.println("ERROR in getUserBoards: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get board by ID", description = "Retrieve a specific board by its ID")
    /**
     * Retrieve a single board by its ID, including columns and members.
     * Returns 403 if the board does not belong to the authenticated user.
     *
     * @param id the board UUID
     * @return 200 with BoardDetailResponse, or 403 if not found/unauthorized
     */
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<BoardDetailResponse> getBoardById(@PathVariable @NonNull String id) {
        User user = getAuthenticatedUser();

        return boardRepository.findByIdAndUserId(id, user.getId())
                .map(board -> {
                    // Trigger lazy loading within transaction
                    List<BoardDetailResponse.ColumnDto> columnDtos = board.getColumns().stream()
                            .map(col -> new BoardDetailResponse.ColumnDto(
                                    col.getId(),
                                    col.getColumnName(),
                                    col.getColumnOrder(),
                                    col.getLimitWorkInProgress()))
                            .toList();

                    List<BoardDetailResponse.MemberDto> memberDtos = board.getMembers().stream()
                            .map(m -> new BoardDetailResponse.MemberDto(
                                    m.getId(),
                                    m.getMemberEmail(),
                                    m.getRole() != null ? m.getRole().name() : null,
                                    m.getMemberOrder()))
                            .toList();

                    BoardDetailResponse detail = new BoardDetailResponse(
                            board.getId(),
                            board.getName(),
                            board.getDescription(),
                            columnDtos,
                            memberDtos);

                    return ResponseEntity.ok(detail);
                })
                .orElse(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
    }

    /**
     * Create a new board for the authenticated user.
     *
     * @param board board payload (name, description)
     * @return 201 with the created BoardResponse
     */
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create a new board", description = "Create a new board for the authenticated user")
    @PostMapping
    public ResponseEntity<BoardResponse> createBoard(@RequestBody @NonNull Board board) {
        User user = getAuthenticatedUser();

        board.setUser(user);
        Board savedBoard = boardRepository.save(board);

        BoardResponse response = new BoardResponse(
                savedBoard.getId(),
                savedBoard.getName(),
                savedBoard.getDescription());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("isAuthenticated()") // Ensure only members and administrators can access this endpoint
    @Operation(summary = "Update a board", description = "Update an existing board")
    /**
     * Update an existing board (name and description).
     * Returns 403 if the board does not belong to the authenticated user.
     *
     * @param id           the board UUID
     * @param boardDetails new board data
     * @return 200 with the updated board, or 403 if not found/unauthorized
     */
    @PutMapping("/{id}")
    public ResponseEntity<Board> updateBoard(
            @PathVariable @NonNull String id,
            @RequestBody @NonNull Board boardDetails) {

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

    @PreAuthorize("isAuthenticated()") // Ensure only members and administrators can access this endpoint
    @Operation(summary = "Delete a board", description = "Delete a board by its ID")
    /**
     * Delete a board by its ID.
     * Returns 403 if the board does not belong to the authenticated user.
     *
     * @param id the board UUID
     * @return 200 on success, or 403 if not found/unauthorized
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBoard(@PathVariable @NonNull String id) {
        User user = getAuthenticatedUser();

        return boardRepository.findById(id)
                .filter(board -> board.getUser().getId().equals(user.getId()))
                .map(board -> {
                    boardRepository.delete(java.util.Objects.requireNonNull(board));
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
    }
}