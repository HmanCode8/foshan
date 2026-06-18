package com.example.mgis.entity.chart.vo;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class GroupVO {
    private Long groupId;
    private String groupName;
    private String originalGroupName;
    private String groupNickname;
    private String notice;
    // 废弃单字符串头像
    // private String groupAvatar;
    // 新增：前4位群成员头像数组
    private List<String> groupAvatarList;
    private String ownerUsername;
    private String remark;
    private LocalDateTime createTime;
    private String myGroupNick;
    private Integer unreadCount;
    private Integer groupChatStatus;

    private Integer memberTotal; // 群总人数
    private String lastMsgContent; // 最后一条消息内容
    private LocalDateTime lastMsgTime; // 最后消息时间
    private String lastMsgSendUser; // 最后消息发送人账号
}