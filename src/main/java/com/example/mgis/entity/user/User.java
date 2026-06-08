package com.example.mgis.entity.user;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("user")
public class User {
    private Long id;
    private Boolean agree;
    private String username;
    private String email;
    private String password;
    // confirmPassword 数据库无此字段，仅前端传参使用，保留即可
    private String confirmPassword;

    // 新增：在线状态 1=在线 0=离线
    private Integer isOnline;
}