package com.kanban.kanbanapp.service.board;

import java.util.List;
import com.kanban.kanbanapp.Model.Board;

public interface Inter_boardService {

    /**
     * Create a new board.
     *
     * @param board the board to create
     * @return the created board
     */
    Board addBoard(Board board);

    /**
     * Retrieve a board by its id.
     *
     * @param id the board id
     * @return the matching board
     */
    Board getBoardById(Long id);

    /**
     * Update an existing board.
     *
     * @param id    the board id
     * @param board the new board data
     * @return the updated board
     */
    Board updateBoard(Long id, Board board);

    /**
     * Delete a board by its id.
     *
     * @param id the board id
     */
    void deleteBoard(Long id);

    /**
     * Retrieve all boards.
     *
     * @return the list of boards
     */
    List<Board> getAllBoards();

    /**
     * Retrieve boards matching a name filter.
     *
     * @return the list of matching boards
     */
    List<Board> getBoardsByName();

    /**
     * Retrieve boards containing a column matching a name filter.
     *
     * @return the list of matching boards
     */
    List<Board> getBoardsByColumnName();

    /**
     * Retrieve boards containing a member matching a name filter.
     *
     * @return the list of matching boards
     */
    List<Board> getBoardsByMemberName();
}