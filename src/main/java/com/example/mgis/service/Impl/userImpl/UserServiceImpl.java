package com.example.mgis.service.Impl.userImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.mgis.common.result.Result;
import com.example.mgis.entity.user.User;
import com.example.mgis.controller.mapper.user.UserMapper;
import com.example.mgis.service.user.UserService;
import com.example.mgis.untils.EmailUtil;
import com.example.mgis.untils.JwtUtil;
import com.example.mgis.websocket.ChatServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private EmailUtil emailUtil;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    // 新增：注入 WebSocket 服务
    @Autowired
    private ChatServer chatServer;

    // ====================== 登录 ======================
    @Override
    public Result<?> login(User user) {
        User dbUser = userMapper.selectOne(Wrappers.lambdaQuery(User.class).eq(User::getUsername, user.getUsername()));
        if (dbUser == null) {
            return Result.fail("用户不存在");
        }
        if (!passwordEncoder.matches(user.getPassword(), dbUser.getPassword())) {
            return Result.fail("密码错误");
        }
        // 登录设置在线=1
        updateOnlineStatus(dbUser.getUsername(), 1);

        // 新增：登录成功，向所有好友推送【上线】状态
        chatServer.broadcastStatus(dbUser.getUsername(), true);

        String token = jwtUtil.generateToken(dbUser.getId(), dbUser.getUsername());
        Map<String, Object> map = new HashMap<>();
        map.put("token", token);
        map.put("userName", dbUser.getUsername());
        return Result.success(map, "登录成功");
    }

    // ====================== 注册 ======================
    @Override
    public Result<?> register(User user) {
        Long count = userMapper.selectCount(Wrappers.lambdaQuery(User.class).eq(User::getUsername, user.getUsername()));
        if (count > 0) {
            return Result.fail("用户名已存在");
        }
        if (!user.getPassword().equals(user.getConfirmPassword())) {
            return Result.fail("两次密码不一致");
        }
        String encryptPwd = passwordEncoder.encode(user.getPassword());
        user.setPassword(encryptPwd);
        user.setConfirmPassword(null);
        userMapper.insert(user);
        return Result.success("注册成功");
    }

    // ====================== 发送验证码（Redis版） ======================
    @Override
    public Result<?> sendForgetCode(String username, String email) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username).eq("email", email);
        User user = userMapper.selectOne(wrapper);
        if (user == null) {
            return Result.fail("账号与邮箱不匹配，请核对后重试");
        }

        String code = String.format("%06d", new Random().nextInt(1000000));
        String redisKey = "user:forget:code:" + username + ":" + email;
        redisTemplate.opsForValue().set(redisKey, code, 5, TimeUnit.MINUTES);

        try {
            emailUtil.sendForgetCode(email, code);
        } catch (Exception e) {
            e.printStackTrace();
            redisTemplate.delete(redisKey);
            return Result.fail("验证码发送失败，请稍后重试");
        }
        return Result.success("验证码已发送到邮箱");
    }

    // ====================== 重置密码（Redis版） ======================
    @Override
    public Result<?> resetPassword(String username, String email, String code, String newPassword) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username).eq("email", email);
        User user = userMapper.selectOne(wrapper);
        if (user == null) {
            return Result.fail("账号与邮箱不匹配");
        }

        String redisKey = "user:forget:code:" + username + ":" + email;
        String correctCode = redisTemplate.opsForValue().get(redisKey);
        if (correctCode == null || !correctCode.equals(code)) {
            return Result.fail("验证码错误或已过期");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
        redisTemplate.delete(redisKey);
        return Result.success("密码重置成功，请使用新密码登录");
    }

    //根据用户名查询用户
    @Override
    public User getByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        return userMapper.selectOne(wrapper);
    }

    //查询全部系统用户
    @Override
    public List<User> getAllUser() {
        return userMapper.selectList(null);
    }

    //【新增实现：修改在线状态 1上线 0下线】
    @Override
    public Result<?> updateOnlineStatus(String username, Integer online) {
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getUsername, username);
        User user = new User();
        user.setIsOnline(online);
        userMapper.update(user, updateWrapper);
        return Result.success("在线状态修改成功");
    }

    @Override
    public Result<?> updateUserAvatar(String username, String avatarUrl) {
        LambdaUpdateWrapper<User> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(User::getUsername, username).set(User::getAvatar, avatarUrl);
        userMapper.update(null, wrapper);
        return Result.success("头像更新成功");
    }
    /**
     * 获取用户头像接口实现
     */
    @Override
    public Result<?> getUserAvatar(String username) {
        User user = getByUsername(username);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        // 头像为空返回空字符串，前端做默认图兜底
        String avatar = user.getAvatar() == null ? "" : user.getAvatar();
        return Result.success(avatar);
    }
}