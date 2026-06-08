package com.example.mgis.untils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.mgis.controller.mapper.user.UserMapper;
import com.example.mgis.entity.user.User;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserUtil {

    private final UserMapper userMapper;

    // 注入用户Mapper
    public CurrentUserUtil(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 获取当前登录用户ID
     * @return 用户ID，未登录或不存在返回null
     */
    public Long getCurrentUserId() {
        // 从SecurityContext获取当前用户名
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if ("anonymousUser".equals(username)) {
            return null;
        }
        // 根据用户名查询用户ID
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
        return user != null ? user.getId() : null;
    }

    /**
     * 获取当前登录用户名
     */
    public String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}