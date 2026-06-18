package com.example.mgis.entity.chart.vo;

import lombok.Data;

import java.util.List;

@Data
public class GroupMsgReadVO {
    private Long groupMsgId;
    private String sendUsername;
    private String msgContent;
    private List<String> readUserList;
    private List<String> unreadUserList;
}
