package com.kanban.kanbanapp.Model;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.OneToMany;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "boards")
public class Board {

    @Id
    @GeneratedValue
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @EqualsAndHashCode.Include
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    // relation to User (many-to-one)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    // relation to KanbanColumn (one-to-many)
    @OneToMany(
        mappedBy = "board",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    private List<KanbanColumn> columns = new ArrayList<>();

    //  relation to Member (one-to-many)
    @OneToMany(
    mappedBy = "board",
    cascade = CascadeType.ALL,
    orphanRemoval = true,
    fetch = FetchType.LAZY
    )
    private List<Member> members = new ArrayList<>();

    // Helper methods to manage bi-directional relationships
    public void addColumn(KanbanColumn column) {
        columns.add(column);
        column.setBoard(this);
    }

    //  Helper method to remove a column from the board
    public void removeColumn(KanbanColumn column) {
        columns.remove(column);
        column.setBoard(null);
    }

    // Helper methods to manage bi-directional relationships for members
    public void addMember(Member member) {
        members.add(member);
        member.setBoard(this);
    }

    // Helper method to remove a member from the board
    public void removeMember(Member member) {
        members.remove(member);
        member.setBoard(null);
    }
}