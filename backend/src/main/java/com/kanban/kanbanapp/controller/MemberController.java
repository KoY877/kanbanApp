package com.kanban.kanbanapp.controller;

import com.kanban.kanbanapp.dto.MemberCreateRequest;
import com.kanban.kanbanapp.Model.Board;
import com.kanban.kanbanapp.Model.Member;
import com.kanban.kanbanapp.Model.User;
import com.kanban.kanbanapp.Model.enums.Role;
import com.kanban.kanbanapp.repository.BoardRepository;
import com.kanban.kanbanapp.repository.MemberRepository;
import com.kanban.kanbanapp.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.*;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Tag(name = "Member Controller", description = "APIs for managing members")
@RestController
@RequiredArgsConstructor
@RequestMapping("/board/member")
@Validated
public class MemberController {

    private final MemberRepository memberRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;

    /**
     * Resolve the currently authenticated user from the security context.
     *
     * @return the authenticated User
     * @throws RuntimeException if no matching user is found
     */
    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    /**
     * Retrieve all members belonging to boards owned by the authenticated user.
     *
     * @return 200 with the list of members
     */
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @Operation(summary = "Get all members", description = "Retrieve a list of all members owned by the authenticated user")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Iterable<Member>> getAll() {
        User user = getAuthenticatedUser();

        List<Member> members = memberRepository.findAllByBoard_User_Id(user.getId());

        return ResponseEntity.status(HttpStatus.OK).body(members);
    }

    /**
     * Retrieve a single member by its ID.
     * Returns 404 if the member does not exist or does not belong to a
     * board owned by the authenticated user.
     *
     * @param id the member UUID
     * @return 200 with the member, or 404 if not found/unauthorized
     */
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @Operation(summary = "Get member by ID", description = "Retrieve a single member by its ID")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "ID of the member to retrieve", content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "member-id-123")))
    @GetMapping("/{id}")
    public ResponseEntity<Member> getMemberById(@PathVariable(value = "id") @NonNull String id) {
        User user = getAuthenticatedUser();
        Optional<Member> memberInDb = memberRepository.findByIdAndBoard_User_Id(id, user.getId());
        return memberInDb.map(member -> new ResponseEntity<>(member, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * Create a new member and associate them with a board.
     * If the given email matches an existing user, the member is linked to that
     * user.
     *
     * @param request creation payload (memberEmail, role, boardId)
     * @return 201 with the created member, 400 if email is blank, or 404 if the
     *         board does not exist or is not owned by the authenticated user
     */
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @Operation(summary = "Create a new member", description = "Create a new member with specified details")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Member> create(@RequestBody @NonNull MemberCreateRequest request) {
        User authenticatedUser = getAuthenticatedUser();

        Member member = new Member();
        member.setMemberEmail(Optional.ofNullable(request.memberEmail()).orElse(""));
        member.setMemberOrder(Optional.ofNullable(request.memberOrder()).orElse(0));

        String roleRequest = request.role();

        if (roleRequest != null) {
            try {
                member.setRole(Role.valueOf(roleRequest.toUpperCase()));
            } catch (IllegalArgumentException e) {
                member.setRole(Role.STANDARD);
            }
        } else {
            member.setRole(Role.STANDARD);
        }

        // Associate with board if boardId is provided, scoped to boards owned by the
        // authenticated user so members cannot be created on another user's board
        if (request.boardId() != null && !request.boardId().isEmpty()) {
            String boardId = java.util.Objects.requireNonNull(request.boardId());
            Optional<Board> board = boardRepository.findByIdAndUserId(boardId, authenticatedUser.getId());
            if (board.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            member.setBoard(board.get());
        }

        // Resolve user by email (optional — user_id can be null for unregistered
        // invites)
        String email = request.memberEmail();
        if (email != null && !email.isBlank()) {
            Optional<User> user = userRepository.findByEmail(email);
            user.ifPresent(member::setUser);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        Member saved = memberRepository.save(member);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * Delete a member by its ID.
     * Returns 404 if the member does not exist or does not belong to a
     * board owned by the authenticated user.
     *
     * @param id the member UUID
     * @return 204 on success, or 404 if not found/unauthorized
     */
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "ID of the member to delete", content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "member-id-123")))
    @Operation(summary = "Delete a member", description = "Delete a member by its ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable(value = "id") @NonNull String id) {
        User user = getAuthenticatedUser();
        Optional<Member> memberInDb = memberRepository.findByIdAndBoard_User_Id(id, user.getId());
        if (memberInDb.isPresent()) {
            memberRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Full replacement update of a member.
     *
     * @param id      the member UUID
     * @param request updated member data (memberEmail, role, boardId)
     * @return 200 with the updated member, or 404 if not found or the referenced
     *         board does not exist or is not owned by the authenticated user
     */
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update a member", description = "Update a member by its ID")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Fields to update (memberEmail, role, boardId)", content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"memberEmail\": \"newEmail@example.com\", \"role\": \"newRole\", \"boardId\": \"newBoardId\"}")))
    @PutMapping("/{id}")
    public ResponseEntity<Member> update(@PathVariable(value = "id") @NonNull String id,
            @RequestBody @NonNull MemberCreateRequest request) {

        User user = getAuthenticatedUser();
        Optional<Member> memberInDb = memberRepository.findByIdAndBoard_User_Id(id, user.getId());

        if (memberInDb.isPresent()) {
            Member memberToUpdate = memberInDb.get();
            memberToUpdate.setMemberEmail(
                    Optional.ofNullable(request.memberEmail()).orElse(memberToUpdate.getMemberEmail()));
            String roleRequest = request.role();
            if (roleRequest != null) {
                try {
                    memberToUpdate.setRole(Role.valueOf(roleRequest.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    memberToUpdate.setRole(Role.STANDARD);
                }
            } else {
                memberToUpdate.setRole(Role.STANDARD);
            }

            // Update board if boardId is provided, scoped to boards owned by the
            // authenticated user so a member cannot be moved to another user's board
            if (request.boardId() != null && !request.boardId().isEmpty()) {
                String boardId = java.util.Objects.requireNonNull(request.boardId());
                Optional<Board> board = boardRepository.findByIdAndUserId(boardId, user.getId());
                if (board.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
                }
                memberToUpdate.setBoard(board.get());
            }

            Member updatedMember = memberRepository.save(memberToUpdate);
            return ResponseEntity.status(HttpStatus.OK).body(updatedMember);
        }

        return ResponseEntity.notFound().build();

    }

    /**
     * Partial update of a member. Currently unimplemented (see commented-out
     * logic below) and always returns 404.
     *
     * @param id      the member UUID
     * @param updates map of fields to update
     * @return 404 (not implemented)
     */
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Patch a member", description = "Partially update a member by its ID")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Fields to update (memberEmail, role, boardId)", content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"memberEmail\": \"newEmail@example.com\", \"role\": \"newRole\", \"boardId\": \"newBoardId\"}")))
    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Member> patch(@PathVariable(value = "id") String id,
        @RequestBody Map<String, Object> updates) 
        {

        return ResponseEntity.notFound().build();
    }
}