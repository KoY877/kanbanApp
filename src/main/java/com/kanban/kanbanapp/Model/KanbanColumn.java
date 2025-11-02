package com.kanban.kanbanapp.Model;


import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.OneToMany;       
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.CascadeType; 
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
@Table  (name = "kanban_columns")
public class KanbanColumn {
    @Id
   @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
 
    private String name;

    private Integer limitWork;

    @ManyToOne
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @OneToMany(mappedBy = "column", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Todo> todos = new ArrayList<>();

    // Ajout du champ addedTodos pour suivre la logique de Board
    @OneToMany(mappedBy = "column", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Todo> addedTodos = new ArrayList<>();

    // Constructeur flexible
    public KanbanColumn(String id, String name, Board board, List<Todo> todos, List<Todo> addedTodos, Integer limitWork) {
        this.id = id;
        this.name = name != null ? name : "";
        this.board = board;
        if (todos != null) this.todos = todos;
        if (addedTodos != null) this.addedTodos = addedTodos;
        this.limitWork = limitWork;
    }

    // Constructeur pour un seul todo (optionnel)
    public KanbanColumn(String id, String name, Board board, Todo todo, Integer limitWork) {
        this(id, name, board, todo != null ? List.of(todo) : null, null, limitWork);
    }

    // Constructeur par défaut généré par Lombok (@NoArgsConstructor)

   
}
