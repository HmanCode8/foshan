package com.example.mgis.entity.chart.vo;
import lombok.Data;
@Data
public class GroupMemberVO {
    private String username;
    private String avatar;
    private Boolean online;
    // 0群主 1普通成员
    private Integer memberType;
    private Integer isMute;
    // 成员在本群专属昵称
    private String memberNick;
    private Integer isChatting;
}