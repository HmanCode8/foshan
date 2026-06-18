package com.example.mgis.entity.chart.dto;

import lombok.Data;

@Data
public class GroupUpdateDTO {
    private Long groupId;
    private String currentUser;
    private String groupNickname; // 新群名称
    private String remark; // 群简介
    private String notice; // 群公告
}