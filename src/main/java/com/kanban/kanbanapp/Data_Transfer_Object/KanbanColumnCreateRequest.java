package com.kanban.kanbanapp.Data_Transfer_Object;

import com.kanban.kanbanapp.Model.Board;

import lombok.*; 

@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor

public class KanbanColumnCreateRequest {
    private String columnName;
    private Integer limitWorkInProgress;
    private String boardId;

}
