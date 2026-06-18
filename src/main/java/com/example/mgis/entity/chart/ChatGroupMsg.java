package com.example.mgis.entity.chart;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_group_msg")
public class ChatGroupMsg {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long groupId;
    private String sendUsername;
    private String msgContent;
    private Integer msgType;
    private String fileUrl;
    private String fileName;
    private String fileSize;
    private LocalDateTime createTime;
}