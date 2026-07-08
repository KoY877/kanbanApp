package com.kanban.kanbanapp.controller;

import com.kanban.kanbanapp.Data_Transfer_Object.KanbanColumnCreateRequest;
import com.kanban.kanbanapp.Model.Board;
import com.kanban.kanbanapp.Model.KanbanColumn;
import com.kanban.kanbanapp.repository.BoardRepository;
import com.kanban.kanbanapp.repository.KanbanColumnRepository;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Tag(name = "Columns Controller", description = "APIs for managing columns")
@RestController
@RequiredArgsConstructor
@RequestMapping("/board/kanban-column")
@Validated

public class KanbanColumnController {

    @Autowired
    private KanbanColumnRepository kanbanColumnRepository;

    @Autowired
    private BoardRepository boardRepository;

    /**
     * Retrieve all Kanban columns across all boards.
     *
     * @return 200 with the list of columns
     */
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all board columns", description = "Retrieve a list of all board columns")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Iterable<KanbanColumn>> getAll() {

        Iterable<KanbanColumn> columnsIterable = kanbanColumnRepository.findAll();

        return ResponseEntity.status(HttpStatus.OK).body(columnsIterable);
    }

    /**
     * Retrieve a single Kanban column by its ID.
     *
     * @param id the column UUID
     * @return 200 with the column, or 404 if not found
     */
    @PreAuthorize("isAuthenticated()")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "ID of the column to retrieve", content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "column-id-123")))
    @Operation(summary = "Get Column by ID", description = "Retrieve a single column by its ID")
    @GetMapping("/{id}")
    public ResponseEntity<KanbanColumn> getColumnById(@PathVariable("id") @NonNull String id) {
        Optional<KanbanColumn> columnInDb = kanbanColumnRepository.findById(id);
        return columnInDb.map(column -> new ResponseEntity<>(column, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * Create a new Kanban column and associate it with a board.
     * The column order is automatically set to the current column count of that
     * board.
     *
     * @param request creation payload (columnName, limitWorkInProgress, boardId)
     * @return 201 with the saved column, or 404 if the referenced board does not
     *         exist
     */
    @PreAuthorize("isAuthenticated()")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Details of the column to create (columnName, limitWorkInProgress, boardId)", content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"columnName\": \"To Do\", \"limitWorkInProgress\": 5, \"boardId\": \"board-id-123\"}")))
    @Operation(summary = "Create a new kanban column", description = "Create a new kanban column with specified details")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<KanbanColumn> create(@RequestBody @NonNull KanbanColumnCreateRequest request) {
        KanbanColumn column = new KanbanColumn();
        column.setColumnName(Optional.ofNullable(request.getColumnName()).orElse(""));
        column.setLimitWorkInProgress(Optional.ofNullable(request.getLimitWorkInProgress()).orElse(null));

        // Associate with board if boardId is provided
        if (request.getBoardId() != null && !request.getBoardId().isEmpty()) {
            // Already validated as non-null and non-empty, assert it for null safety
            String boardId = java.util.Objects.requireNonNull(request.getBoardId());
            Optional<Board> board = boardRepository.findById(boardId);
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
     *
     * @param id the column UUID
     * @return 204 on success, or 404 if not found
     */
    @PreAuthorize("isAuthenticated()")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "ID of the column to delete", content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "column-id-123")))
    @Operation(summary = "Delete a column", description = "Delete a column by its ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable @NonNull String id) {
        if (kanbanColumnRepository.existsById(id)) {
            kanbanColumnRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Full replacement update of a Kanban column.
     *
     * @param id      the column UUID
     * @param request updated column data (columnName, limitWorkInProgress, boardId)
     * @return 200 with the updated column, or 404 if not found
     */
    @PreAuthorize("isAuthenticated()")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Fields to update (columnName, limitWorkInProgress, boardId)", content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"columnName\": \"New Column Name\", \"limitWorkInProgress\": 5, \"boardId\": \"board-id-123\"}")))
    @Operation(summary = "Update a column", description = "Update a column by its ID")
    @PutMapping("/{id}")
    public ResponseEntity<KanbanColumn> update(@PathVariable("id") @NonNull String id,
            @RequestBody @NonNull KanbanColumnCreateRequest request) {
        Optional<KanbanColumn> columnInDb = kanbanColumnRepository.findById(id);

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
                Optional<Board> board = boardRepository.findById(boardId);
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

    // PATCH /column?id={id} -> patch a column by id
    @PreAuthorize("isAuthenticated()")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Fields to update (columnName, limitWorkInProgress, boardId)", content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"columnName\": \"New Column Name\", \"limitWorkInProgress\": 5, \"boardId\": \"board-id-123\"}")))
    @Operation(summary = "Patch a column", description = "Partially update a column by its ID")
    @PatchMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<KanbanColumn> patch(@RequestParam(value = "id") @NonNull String id,
            @RequestBody @NonNull Map<String, Object> updates) {

        Optional<KanbanColumn> columnInDb = kanbanColumnRepository.findById(id);

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
                    Optional<Board> board = boardRepository.findById(boardId);
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
