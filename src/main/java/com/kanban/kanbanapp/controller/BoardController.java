package com.kanban.kanbanapp.controller;

import com.kanban.kanbanapp.Data_Transfer_Object.BoardCreateRequest;
import com.kanban.kanbanapp.Data_Transfer_Object.BoardCreateRequest.AddedColumnDto;
import com.kanban.kanbanapp.Data_Transfer_Object.BoardCreateRequest.ColumnNameDto;
import com.kanban.kanbanapp.Data_Transfer_Object.BoardCreateRequest.MemberDto;
import com.kanban.kanbanapp.Model.Board;
import com.kanban.kanbanapp.Model.KanbanColumn;
import com.kanban.kanbanapp.Model.Member;
import com.kanban.kanbanapp.repository.BoardRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;
// collectors not used anymore

@CrossOrigin(origins = "http://localhost:4200 , http://localhost:8081")
@RestController
@RequiredArgsConstructor
@RequestMapping("/board")
public class BoardController {
    
    @Autowired
    private BoardRepository boardRepository;

    // GET /board -> list all boards
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Board> getAll() {
       
        return boardRepository.findAll();
    }

    // GET /board/{id} -> get a single board by id
    @GetMapping("/{id}")
    public ResponseEntity<Board> getBoardById(@PathVariable("id") String id) {
        Optional<Board> opt = boardRepository.findById(id);
        return opt.map(board -> new ResponseEntity<>(board, HttpStatus.OK))
                  .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Board> create(@RequestBody BoardCreateRequest request) {
        Board board = new Board();
        board.setName(Optional.ofNullable(request.getName()).orElse(""));
        board.setSelectedTask(Optional.ofNullable(request.getSelectedTask()).orElse(""));
        board.setGlobalOption(Optional.ofNullable(request.getGlobalOption()).orElse(""));

        // Map limits
        Map<String, Integer> limitByName = new HashMap<>();
        if (request.getAdded_columns() != null) {
            for (AddedColumnDto ac : request.getAdded_columns()) {
                if (ac.getColumnName() != null) {
                    limitByName.put(ac.getColumnName(), ac.getLimitWorkInProgress());
                }
            }
        }

        // Final names set
        LinkedHashSet<String> finalNames = new LinkedHashSet<>();
        if (request.getColumns() != null) {
            for (ColumnNameDto c : request.getColumns()) {
                if (c.getColumnName() != null && !c.getColumnName().isBlank()) {
                    finalNames.add(c.getColumnName());
                }
            }
        }
        if (request.getAdded_columns() != null) {
            for (AddedColumnDto ac : request.getAdded_columns()) {
                if (ac.getColumnName() != null && !ac.getColumnName().isBlank()) {
                    finalNames.add(ac.getColumnName());
                }
            }
        }

        // Create KanbanColumns
        for (String name : finalNames) {
            KanbanColumn kc = new KanbanColumn();
            kc.setColumnName(name);
            kc.setLimitWorkInProgress(limitByName.getOrDefault(name, null));
            kc.setBoard(board);
            board.getColumns().add(kc);
        }

        // Members
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

        Board saved = boardRepository.save(board);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // @PostMapping
    // public ResponseEntity<Board> create(@RequestBody Board board) {

    //     System.out.println("Creating board: " + board);
    //     // save board in database
    //     Board saved = boardRepository.save(board);
    //     return new ResponseEntity<Board>(saved, HttpStatus.OK);
    // }
}
