package com.kanban.kanbanapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BoardDetailResponse {

    private String id;
    private String name;
    private String description;
    private List<ColumnDto> columns;
    private List<MemberDto> members;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColumnDto {
        private String id;
        private String columnName;
        private Integer columnOrder;
        private Integer limitWorkInProgress;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberDto {
        private String id;
        private String memberEmail;
        private String role;
        private Integer memberOrder;
    }
}
