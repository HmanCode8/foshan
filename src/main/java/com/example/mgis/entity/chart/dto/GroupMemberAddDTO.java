package com.example.mgis.entity.chart.dto;

import lombok.Data;

@Data
public class GroupMemberAddDTO {
    private Long groupId;
    private String targetUser;
    private String currentUser;
}
