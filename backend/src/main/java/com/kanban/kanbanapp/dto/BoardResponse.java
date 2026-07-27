package com.kanban.kanbanapp.dto;

/**
 * Response DTO representing a board.
 *
 * @param id          the board id
 * @param name        the board name
 * @param description the board description
 */
public record BoardResponse(String id, String name, String description) {
}
