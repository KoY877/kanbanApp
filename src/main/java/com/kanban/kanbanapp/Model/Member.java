package com.kanban.kanbanapp.Model;

import jakarta.persistence.ManyToOne;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
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
    @GeneratedValue
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.VARCHAR)    // <— stocké en VARCHAR
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

   
    private String memberEmail;
    private String role;

    @ManyToOne
    @JsonIgnore
    private Board board;

}