package com.kanban.kanbanapp.Model;

import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter 
@Setter
@NoArgsConstructor 
@AllArgsConstructor
@Entity   
@Table(name = "members")

public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String memberEmail;
    private String role;

    @ManyToOne
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

}