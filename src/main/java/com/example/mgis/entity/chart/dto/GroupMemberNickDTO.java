package com.example.mgis.entity.chart.dto;

import lombok.Data;

@Data
public class GroupMemberNickDTO {
    private Long groupId;
    private String currentUser;
    private String targetUsername; // 要修改的成员，自己改自己填自己账号
    private String memberNick; // 群内新昵称，空则恢复用户名
}