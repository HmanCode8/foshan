package com.example.mgis.entity.chart;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user_friend")
public class UserFriend {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String uid;
    private String friendUid;
    private Integer status;
    private LocalDateTime createTime;
}