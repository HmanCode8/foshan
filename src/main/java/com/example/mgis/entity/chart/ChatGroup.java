package com.example.mgis.entity.chart;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ChatGroup {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String groupName;
    private String groupAvatar;
    private String ownerUsername;
    private LocalDateTime createTime;
    private String remark;
    private String groupNickname;
    private String notice;
}