package com.kanban.kanbanapp.service.board;

import java.util.List;

import org.springframework.stereotype.Service;

import com.kanban.kanbanapp.Model.Board;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class boardService implements Inter_boardService {@Override
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
    
    // private final BoardRepository boardRepository;

    // @Override
    // public Board addBoard(AddBoardRequest board) {
    //     // check if the member and column exist in der DB

    //     // If yes, create the board

    //     // If no, throw exception or create new member/column

    //     // The set  as the board's member and columns


    //     Member member = null; 
    //     KanbanColumn columns = null; 
    //    return null;
    // }

    // private Board createBoard(AddBoardRequest request, KanbanColumn columns, Member member) {
    //   return new Board(
    //     request.getId(),
    //     request.getName(),
    //     columns,
    //     member
    //   );
    // }

    // @Override
    // public Board getBoardById(Long id) {
    //     return boardRepository.findById(id).orElseThrow(() -> new BoardNotFoundException("Board not found!"));
    // }

    // @Override
    // public Board updateBoard(Long id, Board board) {
    //     Board existing = boardRepository.findById(id).orElseThrow(() -> new BoardNotFoundException("Board not found!"));
    //     // update allowed fields (name, columns, members)
    //     existing.setName(board.getName());
    //     existing.setColumns(board.getColumns());
    //     existing.setMembers(board.getMembers());
    //     return boardRepository.save(existing);
    // }

    // // public Board updateExistingBoard(Board existingBoard, BoardUpdateRequest request) {
       
    // //     // update allowed fields (name, columns, members)
    // //     existingBoard.setName(request.getName());
    // //     existingBoard.setColumns(request.getColumns());
    // //     existingBoard.setMembers(request.getMembers());
    // //     return boardRepository.save(existingBoard);
    // // }

    // @Override
    // public void deleteBoard(Long id) {
    //     boardRepository.findById(id).ifPresentOrElse(boardRepository::delete, () -> {
    //         throw new BoardNotFoundException("Board not found!");
    //     });
    // }

    // @Override
    // public List<Board> getAllBoards() {
    //     return boardRepository.findAll();
    // }
    
    // @Override
    // public List<Board> getBoardsByName() {
    //     // No custom query implemented yet; return all as fallback
    //     return boardRepository.findAll();
    // }
    
    // @Override
    // public List<Board> getBoardsByColumnName() {
    //     // No custom query implemented yet; return all as fallback
    //     return boardRepository.findAll();
    // }

    // @Override
    // public List<Board> getBoardsByMemberName() {
    //     // No custom query implemented yet; return all as fallback
    //     return boardRepository.findAll();
    // }
    
}
