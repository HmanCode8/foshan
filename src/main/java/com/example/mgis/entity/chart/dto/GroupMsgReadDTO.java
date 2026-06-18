package com.example.mgis.entity.chart.dto;

import lombok.Data;

@Data
public class GroupMsgReadDTO {
    private Long groupId;
    private String currentUser;
    // 标记单条消息已读：传msgId；标记全部不传
    private Long groupMsgId;
}
