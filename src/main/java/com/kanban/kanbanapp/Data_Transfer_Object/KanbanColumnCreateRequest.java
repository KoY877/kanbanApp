package com.kanban.kanbanapp.Data_Transfer_Object;

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
