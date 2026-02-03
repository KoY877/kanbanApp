package com.kanban.kanbanapp.service.board;

import java.util.List;

import org.springframework.stereotype.Service;

import com.kanban.kanbanapp.Model.Board;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class boardService implements Inter_boardService {
    // Implement all methods from Inter_boardService interface
    @Override
    public Board addBoard(Board board) {
    
        throw new UnsupportedOperationException("Unimplemented method 'addBoard'");
    }

    @Override
    public Board getBoardById(Long id) {
        
        throw new UnsupportedOperationException("Unimplemented method 'getBoardById'");
    }

    @Override
    public Board updateBoard(Long id, Board board) {
        
        throw new UnsupportedOperationException("Unimplemented method 'updateBoard'");
    }

    @Override
    public void deleteBoard(Long id) {
        
        throw new UnsupportedOperationException("Unimplemented method 'deleteBoard'");
    }

    @Override
    public List<Board> getAllBoards() {
      
        throw new UnsupportedOperationException("Unimplemented method 'getAllBoards'");
    }

    @Override
    public List<Board> getBoardsByName() {
       
        throw new UnsupportedOperationException("Unimplemented method 'getBoardsByName'");
    }

    @Override
    public List<Board> getBoardsByColumnName() {
        
        throw new UnsupportedOperationException("Unimplemented method 'getBoardsByColumnName'");
    }

    @Override
    public List<Board> getBoardsByMemberName() {
        
        throw new UnsupportedOperationException("Unimplemented method 'getBoardsByMemberName'");
    }
    
 
    
}
