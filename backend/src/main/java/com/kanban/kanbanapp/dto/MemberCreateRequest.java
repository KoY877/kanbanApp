package com.kanban.kanbanapp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for creating or updating a board member.
 *
 * @param memberEmail the member's email address
 * @param role        the member's role on the board
 * @param memberOrder the member's display order, may be null
 * @param boardId     the id of the board the member belongs to
 */
public record MemberCreateRequest(
        @NotBlank(message = "Member email is required") @Email(message = "Must be a valid email address") String memberEmail,

        @NotBlank(message = "Role is required") String role,

        Integer memberOrder,

        @NotBlank(message = "Board ID is required") String boardId) {
}
