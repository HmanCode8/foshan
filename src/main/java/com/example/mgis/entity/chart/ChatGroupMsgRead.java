package com.example.mgis.entity.chart;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("chat_group_msg_read")
public class ChatGroupMsgRead {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long groupMsgId;
    private Long groupId;
    private String username;
    private Integer isRead;
    private LocalDateTime readTime;
}