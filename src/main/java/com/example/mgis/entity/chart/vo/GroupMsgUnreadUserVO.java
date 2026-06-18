package com.example.mgis.entity.chart.vo;

import lombok.Data;

import java.util.List;

@Data
public class GroupMsgUnreadUserVO {
    private Long groupMsgId;
    private String sendUsername;
    private List<String> unreadUserList; // 未读该消息的成员账号
    private List<String> readUserList; // 已读成员账号
}
