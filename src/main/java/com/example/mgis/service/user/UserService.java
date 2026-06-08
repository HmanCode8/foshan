package com.example.mgis.service.user;

import com.example.mgis.common.result.Result;
import com.example.mgis.entity.user.User;
import java.util.List;

public interface UserService {
    Result<?> login(User user);
    Result<?> register(User user);
    Result<?> sendForgetCode(String username, String email);
    Result<?> resetPassword(String username, String email, String code, String newPassword);

    //新增：根据用户名查询单个用户
    User getByUsername(String username);
    //新增：查询系统全部用户
    List<User> getAllUser();

    //【新增】修改用户在线状态 1上线/0下线
    default Result<?> updateOnlineStatus(String username, Integer online) {
        return null;
    }
}