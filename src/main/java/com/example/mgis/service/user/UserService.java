package com.example.mgis.service.user;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.mgis.common.result.Result;
import com.example.mgis.entity.user.User;
import java.util.List;

// 继承 IService<User>，自带 list()、list(queryWrapper) 等方法
public interface UserService extends IService<User> {
    Result<?> login(User user);
    Result<?> register(User user);
    Result<?> sendForgetCode(String username, String email);
    Result<?> resetPassword(String username, String email, String code, String newPassword);

    User getByUsername(String username);
    List<User> getAllUser();

    default Result<?> updateOnlineStatus(String username, Integer online) {
        return null;
    }
    Result<?> updateUserAvatar(String username, String avatarUrl);
    Result<?> getUserAvatar(String username);
}