package com.example.mgis.entity.chart.dto;
import lombok.Data;
import java.util.List;

@Data
public class GroupMemberInviteDTO {
    private Long groupId;
    private String currentUser;
    // 数组：单个用户传 [xxx]，多个传 [a,b,c]
    private List<String> targetUserList;
}