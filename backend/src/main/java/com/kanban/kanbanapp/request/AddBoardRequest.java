package com.kanban.kanbanapp.request;

import java.lang.reflect.Member;
import com.kanban.kanbanapp.Model.KanbanColumn;
import lombok.Data;

@Data
public class AddBoardRequest {
    
    private Long id;
    private String name = "";
    private KanbanColumn columns;
    private Member members;
}
