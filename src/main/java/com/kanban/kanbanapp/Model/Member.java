package com.kanban.kanbanapp.Model;

import com.kanban.kanbanapp.Model.enums.Role;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.GeneratedValue;

import org.hibernate.annotations.UuidGenerator;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "members")
public class Member {

    @Id
    @GeneratedValue
    @UuidGenerator
    @EqualsAndHashCode.Include
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    // email of the member (can be null for invited members who haven't accepted yet)
    @Column(nullable = true)
    private String memberEmail;
    
    // role in the board
    @Enumerated(EnumType.STRING) 
    @Column(nullable = false)
    private Role role;

    // order of the member in the board
    @Column(nullable = false)
    private Integer memberOrder = 0;

    // relation to User (IMPORTANT)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // relation to Board
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    @JsonIgnore
    private Board board;

   
}