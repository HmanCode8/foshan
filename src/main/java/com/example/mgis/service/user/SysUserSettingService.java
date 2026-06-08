package com.example.mgis.service.user;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.mgis.controller.mapper.SysUserSettingMapper;
import com.example.mgis.controller.mapper.user.UserMapper;
import com.example.mgis.entity.user.SysUserSetting;
import com.example.mgis.entity.user.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class SysUserSettingService {

    private final UserMapper userMapper;
    private final SysUserSettingMapper settingMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ============================
    // 按用户名获取设置
    // ============================
    public Map<String, Object> getSettingByUsername(String username) {
        try {
            // 1. 查用户
            User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
            if (user == null) return getDefaultSetting();
            System.out.println("user.getId() = " + user.getId());
            // 2. 查该用户的设置
            SysUserSetting setting = settingMapper.selectOne(
                    new QueryWrapper<SysUserSetting>().eq("user_id", user.getId())
            );
            System.out.println("setting = " + setting);
            // 3. 无设置 → 返回默认
            if (setting == null) {
                return getDefaultSetting();
            }

            // 4. 有设置 → 返回
            return objectMapper.readValue(setting.getSettings(), Map.class);

        } catch (Exception e) {
            return getDefaultSetting();
        }
    }

    // ============================
    // 保存设置（按用户名）
    // ============================
    public void saveSettingByUsername(String username, Map<String, Object> map) {
        try {
            User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
            if (user == null) return;

            Long userId = user.getId();
            String json = objectMapper.writeValueAsString(map);

            SysUserSetting setting = settingMapper.selectOne(
                    new QueryWrapper<SysUserSetting>().eq("user_id", userId)
            );

            if (setting == null) {
                setting = new SysUserSetting();
                setting.setUserId(userId);
                setting.setSettings(json);
                settingMapper.insert(setting);
            } else {
                setting.setSettings(json);
                settingMapper.updateById(setting);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ============================
    // 默认设置
    // ============================
    public Map<String, Object> getDefaultSetting() {
        return Map.of(
                "theme", "red-theme",
                "primaryColor", "#1890ff",
                "fontFamily", "Microsoft YaHei",
                "fontSize", 14
        );
    }
}