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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Tag(name = "Columns Controller", description = "APIs for managing columns")
@CrossOrigin(origins = "http://localhost:4200 , http://localhost:8081")
@RestController
@RequiredArgsConstructor
@RequestMapping("/board/kanban-column")
@Validated

public class KanbanColumnController {
    
    
    @Autowired
    private KanbanColumnRepository kanbanColumnRepository;

    @Autowired
    private BoardRepository boardRepository;


    // GET /board -> list all boards
    @Operation(summary = "Get all board columns", description = "Retrieve a list of all board columns")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Iterable<KanbanColumn>> getAll() {

        Iterable<KanbanColumn> columnsIterable = kanbanColumnRepository.findAll();

        return ResponseEntity.status(HttpStatus.OK).body(columnsIterable);
    }

    // GET /board/{id} -> get a single member by id
    @SuppressWarnings("null")
    @Operation(summary = "Get Column by ID", description = "Retrieve a single column by its ID")
    @GetMapping("/{id}")
    public ResponseEntity<KanbanColumn> getColumnById(@PathVariable("id") String id) {
        Optional<KanbanColumn> columnInDb = kanbanColumnRepository.findById(id);
        return columnInDb.map(column -> new ResponseEntity<>(column, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    // POST /kanban-column -> create a new kanban column
    @SuppressWarnings("null")
    @Operation(summary = "Create a new kanban column", description = "Create a new kanban column with specified details")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<KanbanColumn> create(@RequestBody KanbanColumnCreateRequest request) {
        KanbanColumn column = new KanbanColumn();
        column.setColumnName(Optional.ofNullable(request.getColumnName()).orElse(""));
        column.setLimitWorkInProgress(Optional.ofNullable(request.getLimitWorkInProgress()).orElse(null));

         // Associate with board if boardId is provided
        if (request.getBoardId() != null && !request.getBoardId().isEmpty()) {
            Optional<Board> board = boardRepository.findById(request.getBoardId());
            if (board.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            column.setBoard(board.get());
        }

        KanbanColumn saved = kanbanColumnRepository.save(column);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // DELETE /board?id={id} -> delete a board by id
    @SuppressWarnings("null")
    @Operation(summary = "Delete a column", description = "Delete a column by its ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        if (kanbanColumnRepository.existsById(id)) {
            kanbanColumnRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    // PUT /board/column/{id} -> update a column by id
    @SuppressWarnings("null")
    @Operation(summary = "Update a column", description = "Update a column by its ID")
    @PutMapping("/{id}")
    public ResponseEntity<KanbanColumn> update(@PathVariable("id") String id, @RequestBody KanbanColumnCreateRequest request) {
        Optional<KanbanColumn> columnInDb = kanbanColumnRepository.findById(id);

        if (columnInDb.isPresent()) {
            KanbanColumn columnToUpdate = columnInDb.get();
            columnToUpdate.setColumnName(
                    Optional.ofNullable(request.getColumnName()).orElse(columnToUpdate.getColumnName()));
            columnToUpdate.setLimitWorkInProgress(Optional.ofNullable(request.getLimitWorkInProgress()).orElse(columnToUpdate.getLimitWorkInProgress()));

            // Update board if boardId is provided
            if (request.getBoardId() != null && !request.getBoardId().isEmpty()) {
                Optional<Board> board = boardRepository.findById(request.getBoardId());
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
    @SuppressWarnings("null")
    @Operation(summary = "Patch a column", description = "Partially update a column by its ID")
    @PatchMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<KanbanColumn> patch(@RequestParam(value = "id") String id,
            @RequestBody Map<String, Object> updates) {

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

            KanbanColumn patchedColumn = kanbanColumnRepository.save(columnToPatch);
            return ResponseEntity.status(HttpStatus.OK).body(patchedColumn);
        }

        return ResponseEntity.notFound().build();
    }
}
