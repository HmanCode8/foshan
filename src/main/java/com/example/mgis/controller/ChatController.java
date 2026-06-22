package com.example.mgis.controller;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.mgis.common.result.Result;
import com.example.mgis.controller.mapper.chart.ChatGroupMapper;
import com.example.mgis.controller.mapper.chart.ChatGroupMemberMapper;
import com.example.mgis.controller.mapper.chart.ChatGroupMsgMapper;
import com.example.mgis.controller.mapper.chart.ChatGroupMsgReadMapper;
import com.example.mgis.controller.mapper.chart.ChatMsgMapper;
import com.example.mgis.controller.mapper.chart.UserFriendMapper;
import com.example.mgis.entity.chart.*;
import com.example.mgis.entity.chart.dto.CreateGroupDTO;
import com.example.mgis.entity.chart.dto.FriendApplyDTO;
import com.example.mgis.entity.chart.dto.GroupMemberInviteDTO;
import com.example.mgis.entity.chart.dto.ReadDTO;
import com.example.mgis.entity.chart.vo.*;
import com.example.mgis.entity.user.User;
import com.example.mgis.service.chart.UserFriendService;
import com.example.mgis.service.user.UserService;
import com.example.mgis.untils.FileUtil;
import com.example.mgis.websocket.ChatServer;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    // ========== 群聊Mapper注入（基础群查询/创建仍需要） ==========
    @Resource
    private ChatGroupMapper groupMapper;
    @Resource
    private ChatGroupMemberMapper groupMemberMapper;
    @Resource
    private ChatGroupMsgMapper groupMsgMapper;
    @Resource
    private ChatGroupMsgReadMapper groupMsgReadMapper;

    // 注入聊天文件路径
    @Value("${file.upload.chartfile-path}")
    private String chartfileRelativePath;

    @Value("${file.upload.chartfile-prefix}")
    private String chartfileVirtualPrefix;

    // ===================== 【私聊、好友相关接口 全部保留】 =====================
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

    /**
     * 纯模糊查询用户接口
     * 根据关键字模糊匹配所有用户名，返回所有包含该字符串的用户（不做好友关系过滤）
     *
     * @param keyword 搜索关键字
     * @return 匹配的用户列表（仅返回用户名、头像、在线状态）
     */
    @GetMapping("/user/search")
    public Result<List<UserChatVO>> searchUser(@RequestParam String keyword) {
        // 1. 非空校验
        if (keyword == null || keyword.isBlank()) {
            return Result.fail("搜索关键字不能为空");
        }

        // 2. 模糊查询所有用户名包含关键字的用户
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(User::getUsername, keyword.trim());
        List<User> allMatchUser = userService.list(queryWrapper);

        // 3. 精简字段，只返回核心信息
        List<UserChatVO> result = allMatchUser.stream()
                .map(user -> {
                    UserChatVO vo = new UserChatVO();
                    vo.setUsername(user.getUsername());
                    vo.setAvatar(user.getAvatar());
                    vo.setOnline(user.getIsOnline() != null && user.getIsOnline() == 1);
                    return vo;
                })
                .collect(Collectors.toList());

        return Result.success(result);
    }

    //2、发起添加好友申请
    @PostMapping("/friend/add")
    public Result<String> addFriend(@RequestBody FriendApplyDTO dto) {
        if (dto == null || dto.getApplyUser() == null || dto.getFriendUser() == null
                || dto.getApplyUser().isBlank() || dto.getFriendUser().isBlank()) {
            return Result.fail("参数不能为空");
        }
        String apply = dto.getApplyUser().trim();
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
        uf.setIsChatting(0); // 默认未聊天
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
        String applyUser = dto.getApplyUser();
        String targetUser = dto.getFriendUser();

        LambdaUpdateWrapper<UserFriend> uw = new LambdaUpdateWrapper<>();
        uw.eq(UserFriend::getUid, applyUser)
                .eq(UserFriend::getFriendUid, targetUser)
                .eq(UserFriend::getStatus, 0);
        UserFriend uf = new UserFriend();
        uf.setStatus(1);
        uf.setIsChatting(0); // 新增好友默认未聊天
        int count = friendMapper.update(uf, uw);
        if (count <= 0) {
            return Result.fail("没有该条好友申请");
        }
        return Result.success("已同意好友");
    }

    /**
     * 切换好友「正在聊天」状态
     * 前端选中好友打开聊天页调用：status=1
     * 切换/关闭聊天页调用：status=0
     */
    @PostMapping("/friend/chat/status")
    public Result<String> setChatStatus(
            @RequestParam String currentUser,
            @RequestParam String friendUser,
            @RequestParam Integer status) {
        if (currentUser.isBlank() || friendUser.isBlank()) {
            return Result.fail("账号不能为空");
        }
        if (status != 0 && status != 1) {
            return Result.fail("状态只能为 0 或 1");
        }
        // 双向好友关系同步更新状态
        LambdaUpdateWrapper<UserFriend> wrapper = new LambdaUpdateWrapper<>();
        wrapper.and(w -> w.eq(UserFriend::getUid, currentUser).eq(UserFriend::getFriendUid, friendUser))
                .or(w -> w.eq(UserFriend::getUid, friendUser).eq(UserFriend::getFriendUid, currentUser))
                .set(UserFriend::getIsChatting, status);
        friendMapper.update(null, wrapper);
        return Result.success("状态设置成功");
    }

    //6、获取好友列表（新增返回 isChatting 聊天状态）
    @GetMapping("/friends")
    public Result<List<UserChatVO>> getFriendList(@RequestParam String currentUser) {
        // 查询当前用户所有已通过好友（携带聊天状态）
        LambdaQueryWrapper<UserFriend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFriend::getUid, currentUser).eq(UserFriend::getStatus, 1);
        List<UserFriend> selfFriend = friendMapper.selectList(wrapper);

        LambdaQueryWrapper<UserFriend> wrapper2 = new LambdaQueryWrapper<>();
        wrapper2.eq(UserFriend::getFriendUid, currentUser).eq(UserFriend::getStatus, 1);
        List<UserFriend> beFriend = friendMapper.selectList(wrapper2);

        List<UserFriend> allFriend = new ArrayList<>();
        allFriend.addAll(selfFriend);
        allFriend.addAll(beFriend);

        List<UserChatVO> voList = new ArrayList<>();
        for (UserFriend uf : allFriend) {
            // 拿到对方用户名
            String fname = uf.getUid().equals(currentUser) ? uf.getFriendUid() : uf.getUid();

            UserChatVO vo = new UserChatVO();
            vo.setUsername(fname);
            // 赋值聊天状态
            vo.setIsChatting(uf.getIsChatting());

            User fu = userService.getByUsername(fname);
            vo.setOnline(fu != null && fu.getIsOnline() != null && fu.getIsOnline() == 1);

            // 头像
            if (fu != null) {
                vo.setAvatar(fu.getAvatar());
            }

            // 最新消息
            LambdaQueryWrapper<ChatMsg> lastWrap = new LambdaQueryWrapper<>();
            lastWrap.and(w -> w.eq(ChatMsg::getSendUsername, currentUser).eq(ChatMsg::getReceiveUsername, fname))
                    .or(w -> w.eq(ChatMsg::getSendUsername, fname).eq(ChatMsg::getReceiveUsername, currentUser))
                    .orderByDesc(ChatMsg::getCreateTime).last("limit 1");
            ChatMsg lastMsg = chatMsgMapper.selectOne(lastWrap);
            if (lastMsg != null) {
                vo.setLastMsg(lastMsg.getMsgContent());
                vo.setLastTime(lastMsg.getCreateTime());
            }

            // 未读数
            LambdaQueryWrapper<ChatMsg> unReadWrap = new LambdaQueryWrapper<>();
            unReadWrap.eq(ChatMsg::getSendUsername, fname)
                    .eq(ChatMsg::getReceiveUsername, currentUser)
                    .eq(ChatMsg::getIsRead, 0);
            Long cnt = chatMsgMapper.selectCount(unReadWrap);
            vo.setUnReadNum(cnt.intValue());

            voList.add(vo);
        }
        return Result.success(voList);
    }

    //7、私聊历史记录
    @GetMapping("/history")
    public Result<List<ChatMsg>> getHistory(@RequestParam String fromUser, @RequestParam String toUser) {
        LambdaQueryWrapper<ChatMsg> wrap = new LambdaQueryWrapper<>();
        wrap.and(w -> w.eq(ChatMsg::getSendUsername, fromUser).eq(ChatMsg::getReceiveUsername, toUser))
                .or(w -> w.eq(ChatMsg::getSendUsername, toUser).eq(ChatMsg::getReceiveUsername, fromUser))
                .orderByAsc(ChatMsg::getCreateTime);
        return Result.success(chatMsgMapper.selectList(wrap));
    }

    //8、打开私聊窗口标记消息已读
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

    /**
     * 聊天文件上传，保存到 upload/chartfile
     * 前端先调用此接口上传文件，拿到url后再通过websocket发送
     */
    @PostMapping("/upload/file")
    public Result<JSONObject> uploadChartFile(@RequestParam("file") MultipartFile file) {
        // 1. 基础校验
        if (file.isEmpty()) {
            return Result.fail("文件不能为空");
        }

        // 2. 动态拼接【项目根目录 + 相对路径】= 完整绝对路径（和头像逻辑一致）
        String projectRoot = System.getProperty("user.dir");
        String localFilePath = projectRoot + File.separator + chartfileRelativePath;
        File dir = new File(localFilePath);
        // 3. 递归创建目录，兜底防路径不存在
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 4. 调用你自己的 FileUtil 生成唯一文件名
        String fileName = FileUtil.getUniqueFileName(file.getOriginalFilename());
        File targetFile = new File(dir, fileName);

        try {
            // 5. 保存文件
            file.transferTo(targetFile);
        } catch (IOException e) {
            e.printStackTrace();
            return Result.fail("文件保存失败");
        }

        // 6. 获取文件大小（字节）
        long fileSize = file.getSize();

        // 7. 拼接前端访问URL
        String fileUrl = chartfileVirtualPrefix + fileName;

        // 8. 组装返回数据
        JSONObject res = new JSONObject();
        res.put("fileUrl", fileUrl);
        res.put("fileName", file.getOriginalFilename());
        res.put("fileSize", fileSize);
        return Result.success(res);
    }

    // ======================== 【基础群接口：创建/加人/退群/列表/详情/成员/历史消息 保留】 ========================

    /**
     * 创建群聊，自动将创建人设置为群主并加入群
     */
    @PostMapping("/group/create")
    public Result<Long> createGroup(@RequestBody CreateGroupDTO dto) {
        String owner = dto.getOwnerUsername().trim();
        String groupName = dto.getGroupName().trim();
        if (groupName.isBlank()) {
            return Result.fail("群名称不能为空");
        }
        // 1. 插入群基础信息
        ChatGroup group = new ChatGroup();
        group.setGroupName(groupName);
        group.setRemark(dto.getRemark());
        group.setOwnerUsername(owner);
        // 初始化自定义群名、公告为空
        group.setGroupNickname("");
        group.setNotice("");
        groupMapper.insert(group);
        Long groupId = group.getId();

        // 2. 插入群主成员记录
        ChatGroupMember selfMember = new ChatGroupMember();
        selfMember.setGroupId(groupId);
        selfMember.setUsername(owner);
        selfMember.setMemberType(0);
        selfMember.setMemberNick("");
        groupMemberMapper.insert(selfMember);
        return Result.success(groupId, "群创建成功");
    }

    /**
     * 获取当前用户加入的所有群列表（含展示群名、本人昵称、未读数量）
     */
    @GetMapping("/group/my")
    public Result<List<GroupVO>> getMyGroup(@RequestParam String currentUser) {
        LambdaQueryWrapper<ChatGroupMember> memberWrap = new LambdaQueryWrapper<>();
        memberWrap.eq(ChatGroupMember::getUsername, currentUser);
        List<ChatGroupMember> memberList = groupMemberMapper.selectList(memberWrap);
        if (memberList.isEmpty()) {
            return Result.success(new ArrayList<>());
        }
        List<Long> groupIdList = memberList.stream()
                .map(ChatGroupMember::getGroupId)
                .collect(Collectors.toList());
        LambdaQueryWrapper<ChatGroup> groupWrap = new LambdaQueryWrapper<>();
        groupWrap.in(ChatGroup::getId, groupIdList);
        List<ChatGroup> groupList = groupMapper.selectList(groupWrap);

        Map<Long, ChatGroupMember> groupMemberMap = memberList.stream()
                .collect(Collectors.toMap(ChatGroupMember::getGroupId, m -> m));

        List<GroupVO> voList = groupList.stream().map(g -> {
            GroupVO vo = new GroupVO();
            vo.setGroupId(g.getId());
            String showName = (g.getGroupNickname() == null || g.getGroupNickname().isBlank())
                    ? g.getGroupName() : g.getGroupNickname();
            vo.setGroupName(showName);
            vo.setOriginalGroupName(g.getGroupName());
            vo.setGroupNickname(g.getGroupNickname());
            vo.setNotice(g.getNotice());
            vo.setOwnerUsername(g.getOwnerUsername());
            vo.setRemark(g.getRemark());
            vo.setCreateTime(g.getCreateTime());

            ChatGroupMember member = groupMemberMap.get(g.getId());
            vo.setMyGroupNick(member.getMemberNick() == null ? "" : member.getMemberNick());
            vo.setGroupChatStatus(member.getIsChatting() == null ? 0 : member.getIsChatting());
            int unreadCount = groupMsgReadMapper.countUnreadMsg(g.getId(), currentUser);
            vo.setUnreadCount(unreadCount);

            // 1、群前4成员头像数组
            LambdaQueryWrapper<ChatGroupMember> top4MemberWrap = new LambdaQueryWrapper<>();
            top4MemberWrap.eq(ChatGroupMember::getGroupId, g.getId()).last("LIMIT 4");
            List<ChatGroupMember> top4Member = groupMemberMapper.selectList(top4MemberWrap);
            List<String> avatarList = new ArrayList<>();
            for (ChatGroupMember m : top4Member) {
                User user = userService.getByUsername(m.getUsername());
                String avatar = (user != null && user.getAvatar() != null) ? user.getAvatar() : "";
                avatarList.add(avatar);
            }
            vo.setGroupAvatarList(avatarList);

            // ========== 新增字段赋值逻辑 ==========
            // ① 群总人数
            LambdaQueryWrapper<ChatGroupMember> totalWrap = new LambdaQueryWrapper<>();
            totalWrap.eq(ChatGroupMember::getGroupId, g.getId());
            Long totalMember = groupMemberMapper.selectCount(totalWrap);
            vo.setMemberTotal(totalMember.intValue());

            // ② 查询该群最后一条聊天消息
            LambdaQueryWrapper<ChatGroupMsg> lastMsgWrap = new LambdaQueryWrapper<>();
            lastMsgWrap.eq(ChatGroupMsg::getGroupId, g.getId())
                    .orderByDesc(ChatGroupMsg::getCreateTime)
                    .last("LIMIT 1");
            ChatGroupMsg lastMsg = groupMsgMapper.selectOne(lastMsgWrap);
            if (lastMsg != null) {
                vo.setLastMsgContent(lastMsg.getMsgContent());
                vo.setLastMsgTime(lastMsg.getCreateTime());
                vo.setLastMsgSendUser(lastMsg.getSendUsername());
            } else {
                // 无任何群消息时置空
                vo.setLastMsgContent("");
                vo.setLastMsgTime(null);
                vo.setLastMsgSendUser("");
            }
            return vo;
        }).collect(Collectors.toList());
        return Result.success(voList);
    }

    /**
     * 根据群ID获取群基础详情
     */
    @GetMapping("/group/info")
    public Result<GroupVO> getGroupInfo(@RequestParam Long groupId, @RequestParam String currentUser) {
        LambdaQueryWrapper<ChatGroupMember> memberWrap = new LambdaQueryWrapper<>();
        memberWrap.eq(ChatGroupMember::getGroupId, groupId)
                .eq(ChatGroupMember::getUsername, currentUser);
        ChatGroupMember member = groupMemberMapper.selectOne(memberWrap);
        if (member == null) {
            return Result.fail("你不在该群中，无权限查看");
        }
        ChatGroup group = groupMapper.selectById(groupId);
        if (group == null) {
            return Result.fail("群不存在");
        }
        GroupVO vo = new GroupVO();
        vo.setGroupId(group.getId());
        String showName = (group.getGroupNickname() == null || group.getGroupNickname().isBlank())
                ? group.getGroupName() : group.getGroupNickname();
        vo.setGroupName(showName);
        vo.setOriginalGroupName(group.getGroupName());
        vo.setGroupNickname(group.getGroupNickname());
        vo.setNotice(group.getNotice());
        vo.setOwnerUsername(group.getOwnerUsername());
        vo.setRemark(group.getRemark());
        vo.setCreateTime(group.getCreateTime());
        vo.setMyGroupNick(member.getMemberNick() == null ? "" : member.getMemberNick());
        vo.setGroupChatStatus(member.getIsChatting() == null ? 0 : member.getIsChatting());

        // 前4成员头像数组
        LambdaQueryWrapper<ChatGroupMember> allMemberWrap = new LambdaQueryWrapper<>();
        allMemberWrap.eq(ChatGroupMember::getGroupId, groupId).last("LIMIT 4");
        List<ChatGroupMember> top4Member = groupMemberMapper.selectList(allMemberWrap);
        List<String> avatarList = new ArrayList<>();
        for (ChatGroupMember m : top4Member) {
            User user = userService.getByUsername(m.getUsername());
            String avatar = (user != null && user.getAvatar() != null) ? user.getAvatar() : "";
            avatarList.add(avatar);
        }
        vo.setGroupAvatarList(avatarList);

        // 群总人数
        LambdaQueryWrapper<ChatGroupMember> totalWrap = new LambdaQueryWrapper<>();
        totalWrap.eq(ChatGroupMember::getGroupId, groupId);
        Long totalMember = groupMemberMapper.selectCount(totalWrap);
        vo.setMemberTotal(totalMember.intValue());

        // 最后一条群消息
        LambdaQueryWrapper<ChatGroupMsg> lastMsgWrap = new LambdaQueryWrapper<>();
        lastMsgWrap.eq(ChatGroupMsg::getGroupId, groupId)
                .orderByDesc(ChatGroupMsg::getCreateTime)
                .last("LIMIT 1");
        ChatGroupMsg lastMsg = groupMsgMapper.selectOne(lastMsgWrap);
        if (lastMsg != null) {
            vo.setLastMsgContent(lastMsg.getMsgContent());
            vo.setLastMsgTime(lastMsg.getCreateTime());
            vo.setLastMsgSendUser(lastMsg.getSendUsername());
        } else {
            vo.setLastMsgContent("");
            vo.setLastMsgTime(null);
            vo.setLastMsgSendUser("");
        }

        return Result.success(vo);
    }

    /**
     * 邀请用户进群，支持单人/批量
     */
    @PostMapping("/group/member/add")
    public Result<?> inviteGroupMember(@RequestBody GroupMemberInviteDTO dto) {
        Long groupId = dto.getGroupId();
        String currentUser = dto.getCurrentUser();
        List<String> targetUserList = dto.getTargetUserList();

        // 参数校验
        if (targetUserList == null || targetUserList.isEmpty()) {
            return Result.fail("请选择需要邀请的用户");
        }

        // 1.校验操作人是否在群内
        LambdaQueryWrapper<ChatGroupMember> inviterWrap = new LambdaQueryWrapper<>();
        inviterWrap.eq(ChatGroupMember::getGroupId, groupId)
                .eq(ChatGroupMember::getUsername, currentUser);
        ChatGroupMember inviter = groupMemberMapper.selectOne(inviterWrap);
        if (inviter == null) {
            return Result.fail("你不在该群，无法邀请成员");
        }

        // 2.循环处理每一个待邀请用户
        for (String targetUser : targetUserList) {
            // 用户不存在则跳过
            User target = userService.getByUsername(targetUser);
            if (target == null) continue;
            // 已在群内则跳过
            LambdaQueryWrapper<ChatGroupMember> existWrap = new LambdaQueryWrapper<>();
            existWrap.eq(ChatGroupMember::getGroupId, groupId)
                    .eq(ChatGroupMember::getUsername, targetUser);
            Long existCount = groupMemberMapper.selectCount(existWrap);
            if (existCount > 0) continue;

            // 新增群成员
            ChatGroupMember newMember = new ChatGroupMember();
            newMember.setGroupId(groupId);
            newMember.setUsername(targetUser);
            newMember.setMemberType(1);
            newMember.setMemberNick("");
            groupMemberMapper.insert(newMember);
        }
        return Result.success("邀请操作完成");
    }

    /**
     * 查询群所有成员列表
     */
    @GetMapping("/group/member/list")
    public Result<List<GroupMemberVO>> getGroupMemberList(
            @RequestParam Long groupId,
            @RequestParam String currentUser
    ) {
        // 校验当前用户是否在群内
        LambdaQueryWrapper<ChatGroupMember> selfWrap = new LambdaQueryWrapper<>();
        selfWrap.eq(ChatGroupMember::getGroupId, groupId)
                .eq(ChatGroupMember::getUsername, currentUser);
        ChatGroupMember self = groupMemberMapper.selectOne(selfWrap);
        if (self == null) {
            return Result.fail("你不在该群，无权限查看成员");
        }
        // 查询全部成员
        LambdaQueryWrapper<ChatGroupMember> allWrap = new LambdaQueryWrapper<>();
        allWrap.eq(ChatGroupMember::getGroupId, groupId);
        List<ChatGroupMember> memberList = groupMemberMapper.selectList(allWrap);
        // 组装VO
        List<GroupMemberVO> voList = new ArrayList<>();
        for (ChatGroupMember m : memberList) {
            User u = userService.getByUsername(m.getUsername());
            GroupMemberVO vo = new GroupMemberVO();
            vo.setUsername(m.getUsername());
            vo.setAvatar(u == null ? "" : u.getAvatar());
            vo.setOnline(u != null && u.getIsOnline() != null && u.getIsOnline() == 1);
            vo.setMemberType(m.getMemberType());
            vo.setIsMute(m.getIsMute());
            vo.setIsChatting(m.getIsChatting());
            vo.setMemberNick(m.getMemberNick() == null ? "" : m.getMemberNick());
            voList.add(vo);
        }
        return Result.success(voList);
    }

    /**
     * 普通用户退出群聊（群主禁止调用）
     */
    @PostMapping("/group/member/quit")
    public Result<?> quitGroup(@RequestParam Long groupId, @RequestParam String currentUser) {
        // 1.查询群信息
        ChatGroup group = groupMapper.selectById(groupId);
        if (group == null) {
            return Result.fail("群不存在");
        }
        // 2.群主不能直接退出，必须先转让群主
        if (currentUser.equals(group.getOwnerUsername())) {
            return Result.fail("群主无法直接退群，请先转让群主权限");
        }
        // 3.删除成员记录
        LambdaQueryWrapper<ChatGroupMember> wrap = new LambdaQueryWrapper<>();
        wrap.eq(ChatGroupMember::getGroupId, groupId)
                .eq(ChatGroupMember::getUsername, currentUser);
        groupMemberMapper.delete(wrap);
        return Result.success("已退出群聊");
    }

    /**
     * 获取群历史聊天消息
     */
    @GetMapping("/group/history")
    public Result<List<GroupMsgVO>> getGroupHistory(
            @RequestParam Long groupId,
            @RequestParam String currentUser
    ) {
        // 校验用户在群内
        LambdaQueryWrapper<ChatGroupMember> memberWrap = new LambdaQueryWrapper<>();
        memberWrap.eq(ChatGroupMember::getGroupId, groupId)
                .eq(ChatGroupMember::getUsername, currentUser);
        ChatGroupMember member = groupMemberMapper.selectOne(memberWrap);
        if (member == null) {
            return Result.fail("你不在该群，无法查看消息");
        }
        // 查询群消息，按时间升序
        LambdaQueryWrapper<ChatGroupMsg> msgWrap = new LambdaQueryWrapper<>();
        msgWrap.eq(ChatGroupMsg::getGroupId, groupId)
                .orderByAsc(ChatGroupMsg::getCreateTime);
        List<ChatGroupMsg> msgList = groupMsgMapper.selectList(msgWrap);
        // 组装VO，携带发送人头像
        List<GroupMsgVO> voList = msgList.stream().map(msg -> {
            GroupMsgVO vo = new GroupMsgVO();
            vo.setId(msg.getId());
            vo.setGroupId(msg.getGroupId());
            vo.setSendUsername(msg.getSendUsername());
            User sendUser = userService.getByUsername(msg.getSendUsername());
            vo.setSendAvatar(sendUser == null ? "" : sendUser.getAvatar());
            vo.setMsgContent(msg.getMsgContent());
            vo.setMsgType(msg.getMsgType());
            vo.setFileUrl(msg.getFileUrl());
            vo.setFileName(msg.getFileName());
            vo.setFileSize(msg.getFileSize());
            vo.setCreateTime(msg.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
        return Result.success(voList);
    }

    // ===================== 已删除重复冲突接口（全部移至ChatGroupController） =====================
    // 移除：updateGroupInfo / updateMemberNick / removeGroupMember / transferGroupOwner / deleteGroup
    // 移除：readSingleGroupMsg / readAllGroupMsg / getGroupUnreadCount / getMsgReadUserList
}