package com.kanban.kanbanapp.service.board;

import java.util.List;
import com.kanban.kanbanapp.Model.Board;

public interface Inter_boardService {
    Board addBoard(Board board);   
    Board getBoardById(Long id);
    Board updateBoard(Long id, Board board);
    void deleteBoard(Long id);
    List<Board> getAllBoards();
    List<Board> getBoardsByName();
    List<Board> getBoardsByColumnName();
    List<Board> getBoardsByMemberName();
}