package com.kanban.kanbanapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kanban.kanbanapp.Model.Member;

public interface MemberRepository extends JpaRepository<Member, String> {
    
}
