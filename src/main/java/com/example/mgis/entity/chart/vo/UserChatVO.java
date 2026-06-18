package com.example.mgis.entity.chart.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserChatVO {
    private String username; //好友账号
    private Boolean online; //是否在线（从WebSocket在线Map拿）
    private String lastMsg; //最后一条消息内容
    private LocalDateTime lastTime; //最后消息时间
    private Integer unReadNum; //未读消息数量（小红点数字）
    // 新增：是否正在聊天
    private Integer isChatting;
    // 新增头像字段
    private String avatar;
}