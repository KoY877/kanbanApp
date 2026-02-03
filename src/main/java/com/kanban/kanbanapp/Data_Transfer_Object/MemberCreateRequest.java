package com.kanban.kanbanapp.Data_Transfer_Object;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MemberCreateRequest {
    private String memberEmail;
    private String role;
    private String boardId;
}
