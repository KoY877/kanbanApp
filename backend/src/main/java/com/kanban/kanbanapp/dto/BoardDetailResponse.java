package com.kanban.kanbanapp.dto;

import java.util.List;

/**
 * Response DTO representing a board with its columns and members.
 *
 * @param id          the board id
 * @param name        the board name
 * @param description the board description
 * @param columns     the board's columns
 * @param members     the board's members
 */
public record BoardDetailResponse(
        String id,
        String name,
        String description,
        List<ColumnDto> columns,
        List<MemberDto> members) {

    /**
     * @param id                  the column id
     * @param columnName          the column name
     * @param columnOrder         the column's display order
     * @param limitWorkInProgress the column's WIP limit, or null for no limit
     */
    public record ColumnDto(String id, String columnName, Integer columnOrder, Integer limitWorkInProgress) {
    }

    /**
     * @param id          the member id
     * @param memberEmail the member's email
     * @param role        the member's role
     * @param memberOrder the member's display order
     */
    public record MemberDto(String id, String memberEmail, String role, Integer memberOrder) {
    }
}
