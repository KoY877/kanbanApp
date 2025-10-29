package com.kanban.kanbanapp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.kanban.kanbanapp.Model.Board;
import com.kanban.kanbanapp.repository.BoardRepository;
import java.util.List;
import java.util.Optional;


@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/board")
public class BoardController {
    
    @Autowired
    private BoardRepository boardRepository;

    // GET /board -> list all boards
    @GetMapping
    public ResponseEntity<List<Board>> getAllBoards() {
        List<Board> boards = (List<Board>) boardRepository.findAll();
        return new ResponseEntity<>(boards, HttpStatus.OK);
    }

    // GET /board/{id} -> get a single board by id
    @GetMapping("/{id}")
    public ResponseEntity<Board> getBoardById(@PathVariable("id") String id) {
        Optional<Board> opt = boardRepository.findById(id);
        return opt.map(board -> new ResponseEntity<>(board, HttpStatus.OK))
                  .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public ResponseEntity<Board> create(@RequestBody Board board) {

        System.out.println("Creating board: " + board);
        // save board in database
        Board saved = boardRepository.save(board);
        return new ResponseEntity<Board>(saved, HttpStatus.OK);
    }
}
