package com.example.mgis.controller;

import com.example.mgis.common.result.Result;
import com.example.mgis.service.user.SysUserSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/user/setting")
@RequiredArgsConstructor
public class UserSettingController {

    private final SysUserSettingService settingService;

    // ============================
    // 获取用户设置
    // 前端传：?username=admin
    // ============================
    @GetMapping
    public Map<String, Object> getMySetting(@RequestParam String username) {
        // 直接用前端传的用户名查询
        return settingService.getSettingByUsername(username);
    }

    // ============================
    // 保存用户设置
    // 前端传：{ "username":"admin", "theme":"dark", ... }
    // ============================
    @PostMapping
    public Result<String> saveMySetting(@RequestBody Map<String, Object> settingMap) {
        // 从前端传的参数里拿用户名
        String username = (String) settingMap.get("username");

        if (username == null || username.isBlank()) {
            return Result.fail("用户名不能为空");
        }

        settingService.saveSettingByUsername(username, settingMap);
        return Result.success(username,"ok");
    }
}