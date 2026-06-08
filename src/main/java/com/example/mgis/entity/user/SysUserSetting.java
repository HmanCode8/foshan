package com.example.mgis.entity.user;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_user_setting")
public class SysUserSetting {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    // 存JSON，用String即可
    private String settings;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}