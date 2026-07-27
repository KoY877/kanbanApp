package com.kanban.kanbanapp.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.kanban.kanbanapp.Model.Board;
import com.kanban.kanbanapp.Model.Member;
import com.kanban.kanbanapp.Model.User;
import com.kanban.kanbanapp.Model.enums.Role;
import com.kanban.kanbanapp.dto.MemberCreateRequest;
import com.kanban.kanbanapp.repository.BoardRepository;
import com.kanban.kanbanapp.repository.MemberRepository;
import com.kanban.kanbanapp.repository.UserRepository;

/**
 * Unit tests for {@link MemberController}, focused on correct behavior and
 * the object-level authorization checks that scope every operation to
 * boards owned by the authenticated user.
 */
@ExtendWith(MockitoExtension.class)
class MemberControllerTest {

    private static final String OWNER_ID = "owner-1";
    private static final String OTHER_USER_ID = "intruder-1";

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private UserRepository userRepository;

    private MemberController memberController;

    private User owner;

    @BeforeEach
    void setUp() {
        memberController = new MemberController(memberRepository, boardRepository, userRepository);

        owner = new User();
        owner.setId(OWNER_ID);
        owner.setEmail("owner@example.com");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /** Push a mocked SecurityContext so getAuthenticatedUser() resolves to the given user. */
    private void authenticateAs(User user) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(user.getEmail());

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    }

    // --- getAll ---

    @Test
    void getAll_returnsMembersScopedToAuthenticatedUsersBoards() {
        authenticateAs(owner);
        Member member = new Member();
        member.setId("member-1");
        when(memberRepository.findAllByBoard_User_Id(OWNER_ID)).thenReturn(List.of(member));

        ResponseEntity<Iterable<Member>> response = memberController.getAll();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(member);
    }

    // --- getMemberById ---

    @Test
    void getMemberById_returnsMember_whenFoundAndOwnedByUser() {
        authenticateAs(owner);
        Member member = new Member();
        member.setId("member-1");
        when(memberRepository.findByIdAndBoard_User_Id("member-1", OWNER_ID)).thenReturn(Optional.of(member));

        ResponseEntity<Member> response = memberController.getMemberById("member-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(member);
    }

    @Test
    void getMemberById_returnsNotFound_whenMissingOrNotOwnedByUser() {
        authenticateAs(owner);
        when(memberRepository.findByIdAndBoard_User_Id("member-1", OWNER_ID)).thenReturn(Optional.empty());

        ResponseEntity<Member> response = memberController.getMemberById("member-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getMemberById_returnsNotFound_whenAnotherUserTriesToAccessMemberTheyDoNotOwn() {
        User intruder = new User();
        intruder.setId(OTHER_USER_ID);
        intruder.setEmail("intruder@example.com");
        authenticateAs(intruder);
        // The member exists, but it belongs to a board owned by someone else,
        // so the ownership-scoped repository lookup correctly finds nothing.
        when(memberRepository.findByIdAndBoard_User_Id("member-1", OTHER_USER_ID)).thenReturn(Optional.empty());

        ResponseEntity<Member> response = memberController.getMemberById("member-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // --- create ---

    @Test
    void create_savesMember_whenBoardIsOwnedAndEmailMatchesExistingUser() {
        authenticateAs(owner);
        Board board = new Board();
        board.setId("board-1");
        User invitedUser = new User();
        invitedUser.setEmail("member@example.com");
        MemberCreateRequest request = new MemberCreateRequest("member@example.com", "STANDARD", 2, "board-1");

        when(boardRepository.findByIdAndUserId("board-1", OWNER_ID)).thenReturn(Optional.of(board));
        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(invitedUser));
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<Member> response = memberController.create(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Member saved = response.getBody();
        assertThat(saved).isNotNull();
        assertThat(saved.getMemberEmail()).isEqualTo("member@example.com");
        assertThat(saved.getMemberOrder()).isEqualTo(2);
        assertThat(saved.getRole()).isEqualTo(Role.STANDARD);
        assertThat(saved.getBoard()).isEqualTo(board);
        assertThat(saved.getUser()).isEqualTo(invitedUser);
    }

    @Test
    void create_createsUnlinkedMember_whenEmailDoesNotMatchAnyExistingUser() {
        authenticateAs(owner);
        MemberCreateRequest request = new MemberCreateRequest("invite@example.com", "STANDARD", null, null);
        when(userRepository.findByEmail("invite@example.com")).thenReturn(Optional.empty());
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<Member> response = memberController.create(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getUser()).isNull();
        assertThat(response.getBody().getBoard()).isNull();
    }

    @Test
    void create_returnsNotFound_whenBoardDoesNotExistOrIsNotOwnedByUser() {
        authenticateAs(owner);
        MemberCreateRequest request = new MemberCreateRequest("member@example.com", "STANDARD", null, "someone-elses-board");
        when(boardRepository.findByIdAndUserId("someone-elses-board", OWNER_ID)).thenReturn(Optional.empty());

        ResponseEntity<Member> response = memberController.create(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(memberRepository, never()).save(any());
    }

    @Test
    void create_returnsBadRequest_whenEmailIsBlank() {
        authenticateAs(owner);
        MemberCreateRequest request = new MemberCreateRequest("", "STANDARD", null, null);

        ResponseEntity<Member> response = memberController.create(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(memberRepository, never()).save(any());
    }

    // --- delete ---

    @Test
    void delete_removesMember_whenFoundAndOwnedByUser() {
        authenticateAs(owner);
        Member member = new Member();
        member.setId("member-1");
        when(memberRepository.findByIdAndBoard_User_Id("member-1", OWNER_ID)).thenReturn(Optional.of(member));

        ResponseEntity<Void> response = memberController.delete("member-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(memberRepository).deleteById("member-1");
    }

    @Test
    void delete_returnsNotFound_whenMissingOrNotOwnedByUser() {
        authenticateAs(owner);
        when(memberRepository.findByIdAndBoard_User_Id("member-1", OWNER_ID)).thenReturn(Optional.empty());

        ResponseEntity<Void> response = memberController.delete("member-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(memberRepository, never()).deleteById(any());
    }

    // --- update ---

    @Test
    void update_updatesMember_whenFoundAndTargetBoardOwnedByUser() {
        authenticateAs(owner);
        Member existing = new Member();
        existing.setId("member-1");
        existing.setRole(Role.STANDARD);
        Board newBoard = new Board();
        newBoard.setId("board-2");
        MemberCreateRequest request = new MemberCreateRequest("updated@example.com", "ADMINISTRATOR", 5, "board-2");

        when(memberRepository.findByIdAndBoard_User_Id("member-1", OWNER_ID)).thenReturn(Optional.of(existing));
        when(boardRepository.findByIdAndUserId("board-2", OWNER_ID)).thenReturn(Optional.of(newBoard));
        when(memberRepository.save(existing)).thenReturn(existing);

        ResponseEntity<Member> response = memberController.update("member-1", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(existing.getMemberEmail()).isEqualTo("updated@example.com");
        assertThat(existing.getRole()).isEqualTo(Role.ADMINISTRATOR);
        assertThat(existing.getBoard()).isEqualTo(newBoard);
    }

    @Test
    void update_returnsNotFound_whenMemberMissingOrNotOwnedByUser() {
        authenticateAs(owner);
        MemberCreateRequest request = new MemberCreateRequest("updated@example.com", "STANDARD", null, null);
        when(memberRepository.findByIdAndBoard_User_Id("member-1", OWNER_ID)).thenReturn(Optional.empty());

        ResponseEntity<Member> response = memberController.update("member-1", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(memberRepository, never()).save(any());
    }

    @Test
    void update_returnsNotFound_whenTargetBoardNotOwnedByUser() {
        authenticateAs(owner);
        Member existing = new Member();
        existing.setId("member-1");
        MemberCreateRequest request = new MemberCreateRequest("updated@example.com", "STANDARD", null, "intruders-target-board");

        when(memberRepository.findByIdAndBoard_User_Id("member-1", OWNER_ID)).thenReturn(Optional.of(existing));
        when(boardRepository.findByIdAndUserId("intruders-target-board", OWNER_ID)).thenReturn(Optional.empty());

        ResponseEntity<Member> response = memberController.update("member-1", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(memberRepository, never()).save(any());
    }

    // --- patch (not yet implemented) ---

    @Test
    void patch_returnsNotFound_becauseUnimplemented() {
        ResponseEntity<Member> response = memberController.patch("member-1", java.util.Map.of("memberEmail", "x@example.com"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
