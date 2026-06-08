package com.example.mgis.entity.chart;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("chat_msg")
public class ChatMsg {
    private Long id;
    private String sendUsername;
    private String receiveUsername;
    private String msgContent;
    private Integer msgType;
    private LocalDateTime createTime;
    private Integer isRead;
}