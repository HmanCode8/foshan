package com.example.mgis.entity.chart;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_group_member") // 对应数据库表名
public class ChatGroupMember {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long groupId;
    private String username;
    // 0群主 1普通 2管理员
    private Integer memberType;
    private LocalDateTime joinTime;
    private Integer isMute;
    private String memberNick;
    private Integer isChatting;
}
