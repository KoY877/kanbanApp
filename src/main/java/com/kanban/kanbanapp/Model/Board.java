package com.kanban.kanbanapp.Model;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;

import jakarta.persistence.OneToMany; 
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.CascadeType; 
import jakarta.persistence.Entity;

import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter 
@Setter
@NoArgsConstructor 
@Table(name = "boards")
public class Board {

    @Id
    @GeneratedValue(generator = "UUID")
    private String id = "";
    private String name = "";
    private String selectedTask = "";
    private String globalOption = "";

    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<KanbanColumn> columns = new ArrayList<>();

    // Ajout du champ addedColumns
    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<KanbanColumn> addedColumns = new ArrayList<>();

    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Member> members = new ArrayList<>();

    // Constructeur flexible pour correspondre à la logique TypeScript
    public Board(String id, String name, List<KanbanColumn> columns, List<KanbanColumn> addedColumns, String selectedTask, String globalOption, List<Member> members) {
        this.id = id;
        this.name = name != null ? name : "";
        if (columns != null) this.columns = columns;
        if (addedColumns != null) this.addedColumns = addedColumns;
        this.selectedTask = selectedTask != null ? selectedTask : "";
        this.globalOption = globalOption != null ? globalOption : "";
        if (members != null) this.members = members;
    }

    // Constructeur pour un seul column et/ou member (similaire à la logique TypeScript)
    public Board(String id, String name, KanbanColumn column, String selectedTask, String globalOption, Member member) {
        this(id, name, column != null ? List.of(column) : null, null, selectedTask, globalOption, member != null ? List.of(member) : null);
    }

    
}
