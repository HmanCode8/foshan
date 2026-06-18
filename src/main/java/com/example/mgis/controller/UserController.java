package com.example.mgis.controller;

import com.example.mgis.common.result.Result;
import com.example.mgis.entity.user.LogoutDTO;
import com.example.mgis.entity.user.User;
import com.example.mgis.service.user.UserService;
import com.example.mgis.untils.FileUtil;
import com.example.mgis.websocket.ChatServer;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Map;


@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;

    // 新增：注入 WebSocket
    @Autowired
    private ChatServer chatServer;

    // ✅ 修复：导入正确的 @Value，并且动态拼接项目根目录
    @Value("${file.upload.avatar-path}")
    private String avatarRelativePath;

    @Value("${file.upload.virtual-prefix}")
    private String virtualPrefix;

    @PostMapping("/login")
    public Result<?> login(@RequestBody User user){
        System.out.println("================ 成功进入 login 接口！================");
        System.out.println(user);
        return userService.login(user);
    }

    @PostMapping("/logout")
    public Result<?> logout(@RequestBody LogoutDTO dto){
        String username = dto.getUsername();
        userService.updateOnlineStatus(username,0);
        // 新增：推送下线状态给所有好友
        chatServer.broadcastStatus(username, false);
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

    // 头像上传接口（含旧文件删除逻辑）
    @PostMapping("/uploadAvatar")
    public Result<?> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            @RequestParam("username") String username
    ) {
        // 1. 校验文件
        if (file.isEmpty()) {
            return Result.fail("请选择图片");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.fail("只能上传图片格式");
        }

        // 2. 动态拼接项目根目录 + 头像目录
        String projectRoot = System.getProperty("user.dir");
        String localAvatarPath = projectRoot + File.separator + avatarRelativePath;
        File dir = new File(localAvatarPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 3. 先查询用户当前头像，删除旧文件
        User dbUser = userService.getByUsername(username);
        if (dbUser != null && dbUser.getAvatar() != null && !dbUser.getAvatar().isBlank()) {
            // 从数据库路径里提取文件名（如 /avatar/xxx.png → xxx.png）
            String oldAvatarUrl = dbUser.getAvatar();
            String oldFileName = oldAvatarUrl.replace(virtualPrefix, "");
            File oldFile = new File(localAvatarPath, oldFileName);
            if (oldFile.exists()) {
                oldFile.delete(); // 删除旧文件
            }
        }

        // 4. 生成唯一文件名，保存新文件
        String fileName = FileUtil.getUniqueFileName(file.getOriginalFilename());
        File targetFile = new File(dir, fileName);
        try {
            file.transferTo(targetFile);
        } catch (IOException e) {
            e.printStackTrace();
            return Result.fail("图片保存失败");
        }

        // 5. 拼接新的访问URL
        String newAvatarUrl = virtualPrefix + fileName;

        // 6. 更新数据库用户头像字段
        userService.updateUserAvatar(username, newAvatarUrl);

        // 7. 返回给前端
        return Result.success(newAvatarUrl, "头像上传成功");
    }
    /**
     * 根据用户名获取用户头像
     * @param username 用户名
     * @return 头像地址
     */
    @GetMapping("/getAvatar")
    public Result<?> getUserAvatar(@RequestParam String username) {
        return userService.getUserAvatar(username);
    }
}