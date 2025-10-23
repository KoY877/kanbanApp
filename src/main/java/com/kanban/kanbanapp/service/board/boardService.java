package com.kanban.kanbanapp.service.board;

import java.util.List;

import org.springframework.stereotype.Service;

import com.kanban.kanbanapp.Model.Board;
import com.kanban.kanbanapp.repository.BoardRepository;

import exceptions.BoardNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class boardService implements Inter_boardService {
    private final BoardRepository boardRepository;

    @Override
    public Board addBoard(Board board) {
       return boardRepository.save(board);
    }

    @Override
    public Board getBoardById(Long id) {
        return boardRepository.findById(id).orElseThrow(() -> new BoardNotFoundException("Board not found!"));
    }

    @Override
    public Board updateBoard(Long id, Board board) {
        Board existing = boardRepository.findById(id).orElseThrow(() -> new BoardNotFoundException("Board not found!"));
        // update allowed fields (name, columns, members)
        existing.setName(board.getName());
        existing.setColumns(board.getColumns());
        existing.setMembers(board.getMembers());
        return boardRepository.save(existing);
    }

    @Override
    public void deleteBoard(Long id) {
        boardRepository.findById(id).ifPresentOrElse(boardRepository::delete, () -> {
            throw new BoardNotFoundException("Board not found!");
        });
    }

    @Override
    public List<Board> getAllBoards() {
        return boardRepository.findAll();
    }
    
    @Override
    public List<Board> getBoardsByName() {
        // No custom query implemented yet; return all as fallback
        return boardRepository.findAll();
    }
    
    @Override
    public List<Board> getBoardsByColumnName() {
        // No custom query implemented yet; return all as fallback
        return boardRepository.findAll();
    }

    @Override
    public List<Board> getBoardsByMemberName() {
        // No custom query implemented yet; return all as fallback
        return boardRepository.findAll();
    }
    
}
