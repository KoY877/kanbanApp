package com.kanban.kanbanapp.controller;

import com.kanban.kanbanapp.Data_Transfer_Object.MemberCreateRequest;
import com.kanban.kanbanapp.Model.Board;
import com.kanban.kanbanapp.Model.Member;
import com.kanban.kanbanapp.repository.BoardRepository;
import com.kanban.kanbanapp.repository.MemberRepository;

import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Tag(name = "Member Controller", description = "APIs for managing members")
@CrossOrigin(origins = "http://localhost:4200 , http://localhost:8081")
@RestController
@RequiredArgsConstructor
@RequestMapping("/board/member")
@Validated
public class MemberController {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private BoardRepository boardRepository;


    // GET /board -> list all boards
    @Operation(summary = "Get all members", description = "Retrieve a list of all members")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Iterable<Member>> getAll() {

        Iterable<Member> membersIterable = memberRepository.findAll();

        return ResponseEntity.status(HttpStatus.OK).body(membersIterable);
    }

    // GET /board/{id} -> get a single member by id
    @SuppressWarnings("null")
    @Operation(summary = "Get member by ID", description = "Retrieve a single member by its ID")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "ID of the member to retrieve", content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "member-id-123")))
    @GetMapping("/{id}")
    public ResponseEntity<Member> getMemberById(@PathVariable(value = "id") String id) {
        Optional<Member> memberInDb = memberRepository.findById(id);
        return memberInDb.map(member -> new ResponseEntity<>(member, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    // POST /member -> create a new member
    @SuppressWarnings("null")
    @Operation(summary = "Create a new member", description = "Create a new member with specified details")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Member> create(@RequestBody MemberCreateRequest request) {
        Member member = new Member();
        member.setMemberEmail(Optional.ofNullable(request.getMemberEmail()).orElse(""));
        member.setRole(Optional.ofNullable(request.getRole()).orElse(""));

        // Associate with board if boardId is provided
        if (request.getBoardId() != null && !request.getBoardId().isEmpty()) {
            Optional<Board> board = boardRepository.findById(request.getBoardId());
            if (board.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            member.setBoard(board.get());
        }

        Member saved = memberRepository.save(member);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // DELETE /board/member/{id} -> delete a member by id
    @SuppressWarnings("null")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "ID of the member to delete", content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "member-id-123")))
    @Operation(summary = "Delete a member", description = "Delete a member by its ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable(value = "id") String id) {
        if (memberRepository.existsById(id)) {
            memberRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    // PUT /board/member/{id} -> update a member by id
    @SuppressWarnings("null")
    @Operation(summary = "Update a member", description = "Update a member by its ID")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Fields to update (memberEmail, role, boardId)", content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"memberEmail\": \"newEmail@example.com\", \"role\": \"newRole\", \"boardId\": \"newBoardId\"}")))
    @PutMapping("/{id}")
    public ResponseEntity<Member> update(@PathVariable(value = "id") String id, @RequestBody MemberCreateRequest request) {

        Optional<Member> memberInDb = memberRepository.findById(id);

        if (memberInDb.isPresent()) {
            Member memberToUpdate = memberInDb.get();
            memberToUpdate.setMemberEmail(
                    Optional.ofNullable(request.getMemberEmail()).orElse(memberToUpdate.getMemberEmail()));
            memberToUpdate.setRole(Optional.ofNullable(request.getRole()).orElse(memberToUpdate.getRole()));

            // Update board if boardId is provided
            if (request.getBoardId() != null && !request.getBoardId().isEmpty()) {
                Optional<Board> board = boardRepository.findById(request.getBoardId());
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

    // PATCH /member/{id} -> patch a member by id
    @SuppressWarnings("null")
    @Operation(summary = "Patch a member", description = "Partially update a member by its ID")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Fields to update (memberEmail, role, boardId)", content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(example = "{\"memberEmail\": \"newEmail@example.com\", \"role\": \"newRole\", \"boardId\": \"newBoardId\"}")))
    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Member> patch(@PathVariable(value = "id") String id,
           @RequestBody Map<String, Object> updates) {

        Optional<Member> memberInDb = memberRepository.findById(id);

        if (memberInDb.isPresent()) {
            Member memberToPatch = memberInDb.get();

            if (updates.containsKey("memberEmail")) {
                memberToPatch.setMemberEmail((String) updates.get("memberEmail"));
            }
            if (updates.containsKey("role")) {
                memberToPatch.setRole((String) updates.get("role"));
            }

            if (updates.containsKey("boardId")) {
                String boardId = (String) updates.get("boardId");
                if (boardId != null && !boardId.isEmpty()) {
                    Optional<Board> board = boardRepository.findById(boardId);
                    if (board.isEmpty()) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
                    }
                    memberToPatch.setBoard(board.get());
                } else {
                    memberToPatch.setBoard(null);
                }
            }

       
            // Note: 'secret' is not updatable for security reasons

            Member patchedMember = memberRepository.save(memberToPatch);
            return ResponseEntity.status(HttpStatus.OK).body(patchedMember);
        }

        return ResponseEntity.notFound().build();
    }
}