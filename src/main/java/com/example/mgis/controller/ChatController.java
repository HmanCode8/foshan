package com.example.mgis.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.mgis.common.result.Result;
import com.example.mgis.controller.mapper.chart.ChatMsgMapper;
import com.example.mgis.controller.mapper.chart.UserFriendMapper;
import com.example.mgis.entity.chart.*;
import com.example.mgis.entity.user.User;
import com.example.mgis.service.chart.UserFriendService;
import com.example.mgis.service.user.UserService;
import com.example.mgis.websocket.ChatServer;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/chart")
public class ChatController {
    @Resource
    private UserService userService;
    @Resource
    private ChatMsgMapper chatMsgMapper;
    @Resource
    private ChatServer chatServer;
    @Resource
    private UserFriendService friendService;
    @Resource
    private UserFriendMapper friendMapper;

    //1、获取可添加用户列表
    @GetMapping("/user/canAddFriend")
    public Result<List<User>> getCanAddUser(@RequestParam String currentUser) {
        List<User> all = userService.getAllUser();
        List<String> rel = friendService.getAllRelation(currentUser);
        List<User> res = all.stream()
                .filter(u -> !currentUser.equals(u.getUsername()))
                .filter(u -> !rel.contains(u.getUsername()))
                .collect(Collectors.toList());
        return Result.success(res);
    }

    //2、发起添加好友申请
    @PostMapping("/friend/add")
    public Result<String> addFriend(@RequestBody FriendApplyDTO dto) {
        if (dto == null || dto.getCurrentUser() == null || dto.getFriendUser() == null
                || dto.getCurrentUser().isBlank() || dto.getFriendUser().isBlank()) {
            return Result.fail("参数不能为空");
        }
        String apply = dto.getCurrentUser().trim();
        String target = dto.getFriendUser().trim();
        if (apply.equals(target)) return Result.fail("不能添加自己");
        User targetUser = userService.getByUsername(target);
        if (targetUser == null) return Result.fail("用户不存在");
        List<String> rel = friendService.getAllRelation(apply);
        if (rel.contains(target)) return Result.fail("已存在好友/申请记录");

        UserFriend uf = new UserFriend();
        uf.setUid(apply);
        uf.setFriendUid(target);
        uf.setStatus(0);
        friendMapper.insert(uf);
        chatServer.sendSingleUserMsg(target, "【好友申请】" + apply + "申请添加您好友");
        return Result.success("申请已发送");
    }

    //3、查询我收到的好友申请
    @GetMapping("/friend/applyList")
    public Result<List<FriendApplyVO>> getApplyList(@RequestParam String currentUser) {
        List<String> applyNames = friendService.getMyApplyUser(currentUser);
        List<FriendApplyVO> voList = new ArrayList<>();
        for (String name : applyNames) {
            FriendApplyVO vo = new FriendApplyVO();
            vo.setApplyUser(name);
            voList.add(vo);
        }
        return Result.success(voList);
    }

    /**
     * 同意好友申请 POST
     * body:{applyUser:"申请人",targetUser:"当前登录人"}
     */
    @PostMapping("/friend/agree")
    public Result<String> agreeFriend(@RequestBody FriendApplyDTO dto) {
        String applyUser = dto.getCurrentUser();
        String targetUser = dto.getFriendUser();

        LambdaUpdateWrapper<UserFriend> uw = new LambdaUpdateWrapper<>();
        uw.eq(UserFriend::getUid, applyUser)
                .eq(UserFriend::getFriendUid, targetUser)
                .eq(UserFriend::getStatus, 0);
        UserFriend uf = new UserFriend();
        uf.setStatus(1);
        int count = friendMapper.update(uf, uw);
        if(count <= 0){
            return Result.fail("没有该条好友申请");
        }
        return Result.success("已同意好友");
    }
    /**
     * 拉黑好友 POST
     * body:{u1:"自己账号",u2:"好友账号"}
     */
//    @PostMapping("/friend/black")
//    public Result<String> blackFriend(@RequestBody BlackDTO dto) {
//        String u1 = dto.getU1();
//        String u2 = dto.getU2();
//        LambdaUpdateWrapper<UserFriend> uw = new LambdaUpdateWrapper<>();
//        uw.and(w -> w.eq(UserFriend::getUid, u1).eq(UserFriend::getFriendUid, u2))
//                .or(w -> w.eq(UserFriend::getUid, u2).eq(UserFriend::getFriendUid, u1));
//        UserFriend uf = new UserFriend();
//        uf.setStatus(2);
//        friendMapper.update(uf, uw);
//        return Result.success("拉黑成功");
//    }

    //6、获取好友列表（在线从数据库isOnline读取）
    @GetMapping("/friends")
    public Result<List<UserChatVO>> getFriendList(@RequestParam String currentUser) {
        List<String> friendNames = friendService.getPassFriend(currentUser);
        List<UserChatVO> voList = new ArrayList<>();
        for (String fname : friendNames) {
            UserChatVO vo = new UserChatVO();
            vo.setUsername(fname);
            User fu = userService.getByUsername(fname);
            vo.setOnline(fu != null && fu.getIsOnline() != null && fu.getIsOnline() == 1);

            //最新消息
            LambdaQueryWrapper<ChatMsg> lastWrap = new LambdaQueryWrapper<>();
            lastWrap.and(w -> w.eq(ChatMsg::getSendUsername, currentUser).eq(ChatMsg::getReceiveUsername, fname))
                    .or(w -> w.eq(ChatMsg::getSendUsername, fname).eq(ChatMsg::getReceiveUsername, currentUser))
                    .orderByDesc(ChatMsg::getCreateTime).last("limit 1");
            ChatMsg lastMsg = chatMsgMapper.selectOne(lastWrap);
            if (lastMsg != null) {
                vo.setLastMsg(lastMsg.getMsgContent());
                vo.setLastTime(lastMsg.getCreateTime());
            }

            //未读数
            LambdaQueryWrapper<ChatMsg> unReadWrap = new LambdaQueryWrapper<>();
            unReadWrap.eq(ChatMsg::getSendUsername, fname).eq(ChatMsg::getReceiveUsername, currentUser).eq(ChatMsg::getIsRead, 0);
            Long cnt = chatMsgMapper.selectCount(unReadWrap);
            vo.setUnReadNum(cnt.intValue());
            voList.add(vo);
        }
        return Result.success(voList);
    }

    //7、聊天历史记录
    @GetMapping("/history")
    public Result<List<ChatMsg>> getHistory(@RequestParam String fromUser, @RequestParam String toUser) {
        LambdaQueryWrapper<ChatMsg> wrap = new LambdaQueryWrapper<>();
        wrap.and(w -> w.eq(ChatMsg::getSendUsername, fromUser).eq(ChatMsg::getReceiveUsername, toUser))
                .or(w -> w.eq(ChatMsg::getSendUsername, toUser).eq(ChatMsg::getReceiveUsername, fromUser))
                .orderByAsc(ChatMsg::getCreateTime);
        return Result.success(chatMsgMapper.selectList(wrap));
    }

    //8、打开聊天标记已读
    @PostMapping("/read")
    public Result<?> readMsg(@RequestBody ReadDTO dto) {
        LambdaQueryWrapper<ChatMsg> wrap = new LambdaQueryWrapper<>();
        wrap.eq(ChatMsg::getSendUsername, dto.getFriendUser()).eq(ChatMsg::getReceiveUsername, dto.getCurrentUser()).eq(ChatMsg::getIsRead, 0);
        ChatMsg upd = new ChatMsg();
        upd.setIsRead(1);
        chatMsgMapper.update(upd, wrap);
        return Result.success();
    }

    //用户下线接口(ws断开调用)
    @PostMapping("/user/offline")
    public Result<?> userOffline(@RequestParam String username) {
        return userService.updateOnlineStatus(username, 0);
    }

    //调试：单发消息
    @GetMapping("/push/user")
    public Result<String> pushSingle(@RequestParam String userName, @RequestParam String msg) {
        chatServer.sendSingleUserMsg(userName, "【系统】" + msg);
        return Result.success();
    }

    //调试：全员推送
    @GetMapping("/push/all")
    public Result<String> pushAll(@RequestParam String msg) {
        chatServer.sendAllMsg("【全员】" + msg);
        return Result.success();
    }
}