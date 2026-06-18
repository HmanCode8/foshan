package com.example.mgis.entity.chart.vo;

import lombok.Data;

@Data
public class GroupUnreadVO {
    private Long groupId;
    private Integer unreadCount; // 当前用户未读消息总数
}
