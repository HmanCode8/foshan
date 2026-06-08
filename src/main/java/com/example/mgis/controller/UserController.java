package com.example.mgis.controller;

import com.example.mgis.common.result.Result;
import com.example.mgis.entity.user.LogoutDTO;
import com.example.mgis.entity.user.User;
import com.example.mgis.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public Result<?> login(@RequestBody User user){
        System.out.println("================ 成功进入 login 接口！================");
        System.out.println(user);
        return userService.login(user);
    }

    @PostMapping("/logout")
    public Result<?> logout(@RequestBody LogoutDTO dto){
        userService.updateOnlineStatus(dto.getUsername(),0);
        return Result.success("退出成功");
    }

    @PostMapping("/register")
    public Result<?> register(@RequestBody User user){
        return userService.register(user);
    }
    // 1. 发送找回密码验证码
    @PostMapping("/forgot-password/sendCode")
    public Result<?> sendForgetCode(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String email = params.get("email");
        if (username == null || email == null) {
            return Result.fail("账号和邮箱不能为空");
        }
        return userService.sendForgetCode(username, email);
    }

    // 2. 重置密码
    @PostMapping("/forgot-password/reset")
    public Result<?> resetPassword(@RequestBody Map<String, String> params) {
        // 1. 逐个取出参数
        String username = params.get("username");
        String email = params.get("email");
        String code = params.get("code");
        String newPassword = params.get("password"); // 前端字段是 password
        String confirmPassword = params.get("confirmPassword");

        // 2. 逐个字段校验，返回明确错误
        if (username == null || username.trim().isEmpty()) {
            return Result.fail("账号不能为空");
        }
        if (email == null || email.trim().isEmpty()) {
            return Result.fail("邮箱不能为空");
        }
        if (code == null || code.trim().isEmpty()) {
            return Result.fail("验证码不能为空");
        }
        if (newPassword == null || newPassword.trim().isEmpty()) {
            return Result.fail("新密码不能为空");
        }
        if (confirmPassword == null || confirmPassword.trim().isEmpty()) {
            return Result.fail("确认密码不能为空");
        }

        // 3. 密码一致性校验
        if (!newPassword.equals(confirmPassword)) {
            return Result.fail("两次输入的密码不一致");
        }

        // 4. 可选：增加密码复杂度校验（和注册保持一致）
        if (newPassword.length() < 6) {
            return Result.fail("新密码长度不能少于6位");
        }

        // 5. 调用 Service 层重置密码
        return userService.resetPassword(username, email, code, newPassword);
    }
}