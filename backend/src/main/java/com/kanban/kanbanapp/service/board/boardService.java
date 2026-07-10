package com.kanban.kanbanapp.service.board;

import java.util.List;

import org.springframework.stereotype.Service;

import com.kanban.kanbanapp.Model.Board;

import lombok.RequiredArgsConstructor;

/**
 * Placeholder implementation of {@link Inter_boardService}.
 * None of the methods are implemented yet; the actual board CRUD logic
 * currently lives in {@link com.kanban.kanbanapp.controller.BoardController}.
 */
@Service
@RequiredArgsConstructor
public class boardService implements Inter_boardService {

    /**
     * {@inheritDoc}
     *
     * @throws UnsupportedOperationException always, not yet implemented
     */
    @Override
    public Board addBoard(Board board) {
        throw new UnsupportedOperationException("Unimplemented method 'addBoard'");
    }

    /**
     * {@inheritDoc}
     *
     * @throws UnsupportedOperationException always, not yet implemented
     */
    @Override
    public Board getBoardById(Long id) {
        throw new UnsupportedOperationException("Unimplemented method 'getBoardById'");
    }

    /**
     * {@inheritDoc}
     *
     * @throws UnsupportedOperationException always, not yet implemented
     */
    @Override
    public Board updateBoard(Long id, Board board) {
        throw new UnsupportedOperationException("Unimplemented method 'updateBoard'");
    }

    /**
     * {@inheritDoc}
     *
     * @throws UnsupportedOperationException always, not yet implemented
     */
    @Override
    public void deleteBoard(Long id) {
        throw new UnsupportedOperationException("Unimplemented method 'deleteBoard'");
    }

    /**
     * {@inheritDoc}
     *
     * @throws UnsupportedOperationException always, not yet implemented
     */
    @Override
    public List<Board> getAllBoards() {
        throw new UnsupportedOperationException("Unimplemented method 'getAllBoards'");
    }

    /**
     * {@inheritDoc}
     *
     * @throws UnsupportedOperationException always, not yet implemented
     */
    @Override
    public List<Board> getBoardsByName() {
        throw new UnsupportedOperationException("Unimplemented method 'getBoardsByName'");
    }

    /**
     * {@inheritDoc}
     *
     * @throws UnsupportedOperationException always, not yet implemented
     */
    @Override
    public List<Board> getBoardsByColumnName() {
        throw new UnsupportedOperationException("Unimplemented method 'getBoardsByColumnName'");
    }

    /**
     * {@inheritDoc}
     *
     * @throws UnsupportedOperationException always, not yet implemented
     */
    @Override
    public List<Board> getBoardsByMemberName() {
        throw new UnsupportedOperationException("Unimplemented method 'getBoardsByMemberName'");
    }
}
