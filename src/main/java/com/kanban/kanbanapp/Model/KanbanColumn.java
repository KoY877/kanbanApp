package com.kanban.kanbanapp.Model;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.ManyToOne;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "kanban_columns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KanbanColumn {

    @Id
    @GeneratedValue
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    
    @Column(name = "columnName")
    private String columnName;

    @Column(name = "limit_work_in_progress")
    private Integer limitWorkInProgress;

    @ManyToOne
    @JoinColumn(name = "board_id", nullable = false)
    @JsonIgnore
    private Board board;
}
