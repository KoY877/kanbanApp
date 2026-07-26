package com.kanban.kanbanapp.controller;

import com.kanban.kanbanapp.Data_Transfer_Object.KanbanColumnCreateRequest;
import com.kanban.kanbanapp.Model.Board;
import com.kanban.kanbanapp.Model.KanbanColumn;
import com.kanban.kanbanapp.Model.User;
import com.kanban.kanbanapp.repository.BoardRepository;
import com.kanban.kanbanapp.repository.KanbanColumnRepository;
import com.kanban.kanbanapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.*;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Tag(name = "Columns Controller", description = "APIs for managing columns")
@RestController
@RequiredArgsConstructor
@RequestMapping("/board/kanban-column")
@Validated

public class KanbanColumnController {

    private final KanbanColumnRepository kanbanColumnRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;

    /**
     * Resolve the currently authenticated user from the security context.
     *
     * @return the authenticated User
     * @throws RuntimeException if no matching user is found
     */
    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));
    }

    /**
     * Retrieve all Kanban columns belonging to boards owned by the
     * authenticated user.
     *
     * @return 200 with the list of columns
     */
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all board columns", description = "Retrieve a list of all board columns owned by the authenticated user")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Iterable<KanbanColumn>> getAll() {
        User user = getAuthenticatedUser();

        List<KanbanColumn> columns = kanbanColumnRepository.findAllByBoard_User_Id(user.getId());

        return ResponseEntity.status(HttpStatus.OK).body(columns);
    }

    /**
     * Retrieve a single Kanban column by its ID.
     * Returns 404 if the column does not exist or does not belong to a
     * board owned by the authenticated user.
     *
     * @param id the column UUID
     * @return 200 with the column, or 404 if not found/unauthorized
     */
    @PreAuthorize("isAuthenticated()")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "ID of the column to retrieve", content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "column-id-123")))
    @Operation(summary = "Get Column by ID", description = "Retrieve a single column by its ID")
    @GetMapping("/{id}")
    public ResponseEntity<KanbanColumn> getColumnById(@PathVariable("id") @NonNull String id) {
        User user = getAuthenticatedUser();
        Optional<KanbanColumn> columnInDb = kanbanColumnRepository.findByIdAndBoard_User_Id(id, user.getId());
        return columnInDb.map(column -> new ResponseEntity<>(column, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * Create a new Kanban column and associate it with a board.
     * The column order is automatically set to the current column count of that
     * board. Returns 404 if the referenced board does not exist or is not
     * owned by the authenticated user.
     *
     * @param request creation payload (columnName, limitWorkInProgress, boardId)
     * @return 201 with the saved column, or 404 if the referenced board does not
     *         exist or is not owned by the authenticated user
     */
    @PreAuthorize("isAuthenticated()")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Details of the column to create (columnName, limitWorkInProgress, boardId)", content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"columnName\": \"To Do\", \"limitWorkInProgress\": 5, \"boardId\": \"board-id-123\"}")))
    @Operation(summary = "Create a new kanban column", description = "Create a new kanban column with specified details")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<KanbanColumn> create(@RequestBody @NonNull KanbanColumnCreateRequest request) {
        User user = getAuthenticatedUser();

        KanbanColumn column = new KanbanColumn();
        column.setColumnName(Optional.ofNullable(request.getColumnName()).orElse(""));
        column.setLimitWorkInProgress(Optional.ofNullable(request.getLimitWorkInProgress()).orElse(null));

        // Associate with board if boardId is provided
        if (request.getBoardId() != null && !request.getBoardId().isEmpty()) {
            // Already validated as non-null and non-empty, assert it for null safety
            String boardId = java.util.Objects.requireNonNull(request.getBoardId());
            Optional<Board> board = boardRepository.findByIdAndUserId(boardId, user.getId());
            if (board.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            column.setBoard(board.get());
            int columnOrder = (int) kanbanColumnRepository.countByBoardId(boardId);
            column.setColumnOrder(columnOrder);
        }

        KanbanColumn saved = kanbanColumnRepository.save(column);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * Delete a Kanban column by its ID.
     * Returns 404 if the column does not exist or does not belong to a
     * board owned by the authenticated user.
     *
     * @param id the column UUID
     * @return 204 on success, or 404 if not found/unauthorized
     */
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "ID of the column to delete", content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "column-id-123")))
    @Operation(summary = "Delete a column", description = "Delete a column by its ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable @NonNull String id) {
        User user = getAuthenticatedUser();
        Optional<KanbanColumn> columnInDb = kanbanColumnRepository.findByIdAndBoard_User_Id(id, user.getId());
        if (columnInDb.isPresent()) {
            kanbanColumnRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Full replacement update of a Kanban column.
     * Returns 404 if the column, or the target board when boardId is
     * provided, does not exist or does not belong to the authenticated user.
     *
     * @param id      the column UUID
     * @param request updated column data (columnName, limitWorkInProgress, boardId)
     * @return 200 with the updated column, or 404 if not found/unauthorized
     */
    @PreAuthorize("isAuthenticated()")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Fields to update (columnName, limitWorkInProgress, boardId)", content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"columnName\": \"New Column Name\", \"limitWorkInProgress\": 5, \"boardId\": \"board-id-123\"}")))
    @Operation(summary = "Update a column", description = "Update a column by its ID")
    @PutMapping("/{id}")
    public ResponseEntity<KanbanColumn> update(@PathVariable("id") @NonNull String id,
            @RequestBody @NonNull KanbanColumnCreateRequest request) {
        User user = getAuthenticatedUser();
        Optional<KanbanColumn> columnInDb = kanbanColumnRepository.findByIdAndBoard_User_Id(id, user.getId());

        if (columnInDb.isPresent()) {
            KanbanColumn columnToUpdate = columnInDb.get();
            columnToUpdate.setColumnName(
                    Optional.ofNullable(request.getColumnName()).orElse(columnToUpdate.getColumnName()));
            columnToUpdate.setLimitWorkInProgress(Optional.ofNullable(request.getLimitWorkInProgress())
                    .orElse(columnToUpdate.getLimitWorkInProgress()));

            // Update board if boardId is provided
            if (request.getBoardId() != null && !request.getBoardId().isEmpty()) {
                // Already validated as non-null and non-empty, assert it for null safety
                String boardId = java.util.Objects.requireNonNull(request.getBoardId());
                Optional<Board> board = boardRepository.findByIdAndUserId(boardId, user.getId());
                if (board.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
                }
                columnToUpdate.setBoard(board.get());
            }

            KanbanColumn updatedColumn = kanbanColumnRepository.save(columnToUpdate);
            return ResponseEntity.status(HttpStatus.OK).body(updatedColumn);
        }

        return ResponseEntity.notFound().build();
    }

    /**
     * Partially update a Kanban column. Only the fields present in the
     * request map are updated. Returns 404 if the column, or the target
     * board when boardId is provided, does not exist or does not belong to
     * the authenticated user.
     *
     * @param id      the column UUID
     * @param updates map of fields to update (columnName, limitWorkInProgress,
     *                boardId)
     * @return 200 with the updated column, 404 if the column or referenced
     *         board is not found/unauthorized
     */
    @PreAuthorize("isAuthenticated()")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Fields to update (columnName, limitWorkInProgress, boardId)", content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"columnName\": \"New Column Name\", \"limitWorkInProgress\": 5, \"boardId\": \"board-id-123\"}")))
    @Operation(summary = "Patch a column", description = "Partially update a column by its ID")
    @PatchMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<KanbanColumn> patch(@RequestParam(value = "id") @NonNull String id,
            @RequestBody @NonNull Map<String, Object> updates) {

        User user = getAuthenticatedUser();
        Optional<KanbanColumn> columnInDb = kanbanColumnRepository.findByIdAndBoard_User_Id(id, user.getId());

        if (columnInDb.isPresent()) {
            KanbanColumn columnToPatch = columnInDb.get();

            if (updates.containsKey("columnName")) {
                columnToPatch.setColumnName((String) updates.get("columnName"));
            }
            if (updates.containsKey("limitWorkInProgress")) {
                columnToPatch.setLimitWorkInProgress((Integer) updates.get("limitWorkInProgress"));
            }

            if (updates.containsKey("boardId")) {
                String boardId = (String) updates.get("boardId");
                if (boardId != null && !boardId.isEmpty()) {
                    Optional<Board> board = boardRepository.findByIdAndUserId(boardId, user.getId());
                    if (board.isEmpty()) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
                    }
                    columnToPatch.setBoard(board.get());
                } else {
                    columnToPatch.setBoard(null);
                }
            }

            // Note: Patching columns and members is not handled here for simplicity

            @SuppressWarnings("null") // JPA save() never returns null
            KanbanColumn patchedColumn = kanbanColumnRepository.save(columnToPatch);
            return ResponseEntity.status(HttpStatus.OK).body(patchedColumn);
        }

        return ResponseEntity.notFound().build();
    }
}
