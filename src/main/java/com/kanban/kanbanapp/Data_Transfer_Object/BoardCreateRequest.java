package com.kanban.kanbanapp.Data_Transfer_Object;

import lombok.*;
import java.util.List;

@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor
public class BoardCreateRequest {
    private String name;
    private String selectedTask;
    private String globalOption;
    private String userId;
    private List<ColumnNameDto> columns;
    private List<AddedColumnDto> added_columns;
    private List<MemberDto> members;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColumnNameDto {
        private String columnName;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddedColumnDto {
        private String columnName;
        private Integer limitWorkInProgress;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberDto {
        private String memberEmail;
        private String role;
    }
}