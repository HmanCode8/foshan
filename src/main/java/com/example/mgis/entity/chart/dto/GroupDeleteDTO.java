package com.example.mgis.entity.chart.dto;

import lombok.Data;

// 解散群 DTO
@Data
public class GroupDeleteDTO {
    private Long groupId;
    private String currentUser; // 校验是否群主
}