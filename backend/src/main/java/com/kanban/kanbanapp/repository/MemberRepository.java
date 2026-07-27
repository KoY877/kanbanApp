package com.kanban.kanbanapp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kanban.kanbanapp.Model.Member;

public interface MemberRepository extends JpaRepository<Member, String> {

    /**
     * Find a member by id, scoped to the board owner. Used to enforce
     * object-level authorization so a user cannot access another user's member.
     *
     * @param id     the member id
     * @param userId the id of the user who must own the member's board
     * @return the matching member, or empty if not found or not owned by the user
     */
    Optional<Member> findByIdAndBoard_User_Id(String id, String userId);

    /**
     * Find all members belonging to boards owned by the given user.
     *
     * @param userId the id of the board owner
     * @return the members of all boards owned by the user
     */
    List<Member> findAllByBoard_User_Id(String userId);
}
