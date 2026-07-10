package com.kanban.kanbanapp.exception;

/**
 * Thrown when adding or moving a task into a column would push its task
 * count past the column's configured Work-In-Progress limit.
 */
public class WipLimitExceededException extends RuntimeException {
    /**
     * @param message the error message
     */
    public WipLimitExceededException(String message) {
        super(message);
    }
}
