package com.kanban.kanbanapp.controller;

import com.kanban.kanbanapp.Data_Transfer_Object.BoardCreateRequest;
import com.kanban.kanbanapp.Data_Transfer_Object.BoardCreateRequest.AddedColumnDto;
import com.kanban.kanbanapp.Data_Transfer_Object.BoardCreateRequest.ColumnNameDto;
import com.kanban.kanbanapp.Data_Transfer_Object.BoardCreateRequest.MemberDto;
import com.kanban.kanbanapp.Model.Board;
import com.kanban.kanbanapp.Model.KanbanColumn;
import com.kanban.kanbanapp.Model.Member;
import com.kanban.kanbanapp.Model.User;
import com.kanban.kanbanapp.repository.BoardRepository;
import com.kanban.kanbanapp.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;
// collectors not used anymore

@Tag(name = "Kanban Controller", description = "APIs for managing Kanban boards")
@CrossOrigin(origins = { "http://localhost:4200", "http://localhost:8081" }, allowCredentials = "true")
@RestController
@RequiredArgsConstructor
@RequestMapping("/board")
@Validated
public class BoardController {

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private UserRepository userRepository;

    // GET /board -> list all boards (without authentication)
    @Operation(summary = "Get all boards", description = "Retrieve a list of all boards")
    @GetMapping
    public ResponseEntity<Iterable<Board>> getAllBoards() {
        Iterable<Board> boards = boardRepository.findAll();
        return ResponseEntity.ok(boards);
    }

    // GET /board/all -> list all boards for a specific user
    @Operation(summary = "Get all boards by user", description = "Retrieve a list of all boards for a specific user")
    @GetMapping("/all")
    public ResponseEntity<Iterable<Board>> getAll(@RequestHeader("api-secret") String secret) {
        // Find user by secret
        Optional<User> userBySecret = userRepository.findBySecret(secret);

        // If user with the provided secret exists
        if (userBySecret.isPresent()) {
            // Fetch boards by user ID
            Iterable<Board> boardsIterable = boardRepository.findAllByUserId(userBySecret.get().getId());

            // Return boards
            return new ResponseEntity<Iterable<Board>>(boardsIterable, HttpStatus.OK);
        }

        // If user not found
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    // GET /board/{id} -> get a single board by id
    @SuppressWarnings("null")
    @Operation(summary = "Get board by ID", description = "Retrieve a single board by its ID")
    @GetMapping("/{id}")
    public ResponseEntity<Board> getBoardById(@PathVariable("id") String id) {
        // Find board by id
        Optional<Board> boardInDb = boardRepository.findById(id);

        // If board exists
        if (boardInDb.isPresent()) {
            return ResponseEntity.status(HttpStatus.OK).body(boardInDb.get());
        }

        // If board not found
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    // POST /board -> create a new board
    @Operation(summary = "Create a new board", description = "Create a new board with specified details")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Board> create(@RequestBody BoardCreateRequest request) {
        Board board = new Board();

        // Set basic fields
        board.setName(Optional.ofNullable(request.getName()).orElse(""));
        board.setSelectedTask(Optional.ofNullable(request.getSelectedTask()).orElse(""));
        board.setGlobalOption(Optional.ofNullable(request.getGlobalOption()).orElse(""));
        board.setUserId(Optional.ofNullable(request.getUserId()).orElse(""));

        // Map limits
        Map<String, Integer> limitByName = new HashMap<>();
        if (request.getAdded_columns() != null) {
            for (AddedColumnDto added_column : request.getAdded_columns()) {
                if (added_column.getColumnName() != null) {
                    limitByName.put(added_column.getColumnName(), added_column.getLimitWorkInProgress());
                }
            }
        }

        // Final names set
        LinkedHashSet<String> finalNames = new LinkedHashSet<>();
        if (request.getColumns() != null) {
            for (ColumnNameDto column : request.getColumns()) {
                if (column.getColumnName() != null && !column.getColumnName().isBlank()) {
                    finalNames.add(column.getColumnName());
                }
            }
        }

        // Added columns
        if (request.getAdded_columns() != null) {
            for (AddedColumnDto added_column : request.getAdded_columns()) {
                if (added_column.getColumnName() != null && !added_column.getColumnName().isBlank()) {
                    finalNames.add(added_column.getColumnName());
                }
            }
        }

        // Create KanbanColumns
        for (String name : finalNames) {
            KanbanColumn kanbanColumn = new KanbanColumn();
            kanbanColumn.setColumnName(name);
            kanbanColumn.setLimitWorkInProgress(limitByName.getOrDefault(name, null));
            kanbanColumn.setBoard(board);
            board.getColumns().add(kanbanColumn);
        }

        // Add members
        if (request.getMembers() != null) {
            for (MemberDto m : request.getMembers()) {
                if (m.getMemberEmail() != null) {
                    Member entity = new Member();
                    entity.setMemberEmail(m.getMemberEmail());
                    entity.setRole(m.getRole());
                    entity.setBoard(board);
                    board.getMembers().add(entity);
                }
            }
        }

        // Save board
        Board saved = boardRepository.save(board);

        // Return response
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // DELETE /board?id={id} -> delete a board by id
    @SuppressWarnings("null")
    @Operation(summary = "Delete a board", description = "Delete a board by its ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        // Check if board exists
        if (boardRepository.existsById(id)) {
            boardRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }

        // If board not found
        return ResponseEntity.notFound().build();
    }

    // PUT /board/{id} -> update a board by id
    @SuppressWarnings("null")
    @Operation(summary = "Update a board", description = "Update a board by its ID")
    @PutMapping("/{id}")
    public ResponseEntity<Board> update(@PathVariable("id") String id, @RequestBody BoardCreateRequest request) {
        // Find board by id
        Optional<Board> boardInDb = boardRepository.findById(id);

        // If board exists, update its fields
        if (boardInDb.isPresent()) {

            Board boardToUpdate = boardInDb.get();
            // Update basic fields
            boardToUpdate.setName(Optional.ofNullable(request.getName()).orElse(boardToUpdate.getName()));
            boardToUpdate.setSelectedTask(
                    Optional.ofNullable(request.getSelectedTask()).orElse(boardToUpdate.getSelectedTask()));
            boardToUpdate.setGlobalOption(
                    Optional.ofNullable(request.getGlobalOption()).orElse(boardToUpdate.getGlobalOption()));

            // Note: Updating columns and members is not handled here for simplicity
            Board updatedBoard = boardRepository.save(boardToUpdate);
            return ResponseEntity.status(HttpStatus.OK).body(updatedBoard);
        }

        // If board not found
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);

    }

    // PATCH /board?id={id} -> patch a board by id
    @SuppressWarnings("null")
    @Operation(summary = "Patch a board", description = "Partially update a board by its ID")
    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Board> patch(@PathVariable(value = "id") String id,
            @RequestBody Map<String, Object> updates) {

        // Find board by id
        Optional<Board> boardInDb = boardRepository.findById(id);

        if (boardInDb.isPresent()) {
            Board boardToPatch = boardInDb.get();

            // Patch basic fields
            if (updates.containsKey("name")) {
                boardToPatch.setName((String) updates.get("name"));
            }

            // Patch selectedTask and globalOption
            if (updates.containsKey("selectedTask")) {
                boardToPatch.setSelectedTask((String) updates.get("selectedTask"));
            }
            if (updates.containsKey("globalOption")) {
                boardToPatch.setGlobalOption((String) updates.get("globalOption"));
            }

            // Note: Patching columns and members is not handled here for simplicity
            Board patchedBoard = boardRepository.save(boardToPatch);

            return ResponseEntity.status(HttpStatus.OK).body(patchedBoard);
        }

        // If board not found
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
    }
}
