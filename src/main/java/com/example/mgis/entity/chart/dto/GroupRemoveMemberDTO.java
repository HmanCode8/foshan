package com.example.mgis.entity.chart.dto;

import lombok.Data;

@Data
public class GroupRemoveMemberDTO {
    private Long groupId;
    private String currentUser;
    private String targetUsername;
}
