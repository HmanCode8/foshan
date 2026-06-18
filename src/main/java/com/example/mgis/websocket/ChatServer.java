package com.example.mgis.websocket;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.mgis.controller.mapper.chart.ChatGroupMemberMapper;
import com.example.mgis.controller.mapper.chart.ChatGroupMsgMapper;
import com.example.mgis.controller.mapper.chart.ChatGroupMsgReadMapper;
import com.example.mgis.controller.mapper.chart.ChatMsgMapper;
import com.example.mgis.entity.chart.ChatGroupMember;
import com.example.mgis.entity.chart.ChatGroupMsg;
import com.example.mgis.entity.chart.ChatGroupMsgRead;
import com.example.mgis.entity.chart.ChatMsg;
import com.example.mgis.service.chart.UserFriendService;
import jakarta.annotation.Resource;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Component
@ServerEndpoint(value = "/ws/chat", configurator = WsOriginConfig.class)
public class ChatServer {
    //全局在线用户 key:用户名 value:多端session列表
    public static final ConcurrentHashMap<String, List<Session>> ONLINE = new ConcurrentHashMap<>();
    // 【新增】异步线程池，解决并发发送消息状态冲突报错 TEXT_FULL_WRITING
    private static final ExecutorService WS_EXECUTOR = Executors.newFixedThreadPool(30);

    // 单聊消息Mapper
    private static ChatMsgMapper chatMsgMapperStatic;
    // 群成员Mapper
    private static ChatGroupMemberMapper groupMemberMapperStatic;
    // 群消息Mapper
    private static ChatGroupMsgMapper groupMsgMapperStatic;
    // 【新增】群消息已读记录Mapper
    private static ChatGroupMsgReadMapper groupMsgReadMapperStatic;
    // 好友服务（推送上下线状态）
    private static UserFriendService userFriendServiceStatic;

    // 消息类型常量
    public static final int MSG_TYPE_TEXT = 1;
    public static final int MSG_TYPE_FILE = 2;
    public static final String ONLINE_TIP = "【状态变更】%s 已上线";
    public static final String OFFLINE_TIP = "【状态变更】%s 已下线";

    private Session session;
    private String currentUsername;

    // ===================== 静态Mapper注入 =====================
    @Resource
    public void setChatMsgMapper(ChatMsgMapper mapper) {
        ChatServer.chatMsgMapperStatic = mapper;
    }

    @Resource
    public void setGroupMemberMapper(ChatGroupMemberMapper mapper) {
        ChatServer.groupMemberMapperStatic = mapper;
    }

    @Resource
    public void setGroupMsgMapper(ChatGroupMsgMapper mapper) {
        ChatServer.groupMsgMapperStatic = mapper;
    }

    // 【新增】注入群消息已读Mapper
    @Resource
    public void setGroupMsgReadMapper(ChatGroupMsgReadMapper mapper) {
        ChatServer.groupMsgReadMapperStatic = mapper;
    }

    @Resource
    public void setUserFriendService(UserFriendService userFriendService) {
        ChatServer.userFriendServiceStatic = userFriendService;
    }

    @OnOpen
    public void onOpen(Session session) throws IOException {
        this.session = session;
        String query = session.getQueryString();
        String userId = null;
        if (query != null && query.startsWith("userId=")) {
            userId = query.substring("userId=".length());
            userId = URLDecoder.decode(userId, StandardCharsets.UTF_8.name());
        }
        if (userId == null || userId.isBlank()) {
            session.close();
            return;
        }
        this.currentUsername = userId;
        ONLINE.computeIfAbsent(userId, k -> new ArrayList<>()).add(session);
        // 推送上线状态给所有好友
        broadcastStatus(currentUsername, true);
    }

    @OnMessage
    public void onMessage(String jsonStr) {
        JSONObject json = JSONObject.parseObject(jsonStr);
        // 区分群聊/单聊：存在toGroupId代表群消息，to代表单聊
        Long toGroupId = json.getLong("toGroupId");
        String targetUser = json.getString("to");
        String content = json.getString("msg");
        Integer msgType = json.getInteger("msgType");
        String fileUrl = json.getString("fileUrl");
        String fileName = json.getString("fileName");
        String fileSize = json.getString("fileSize");

        // 默认文本消息
        if (msgType == null) {
            msgType = MSG_TYPE_TEXT;
        }

        // ===================== 分支1：群聊消息 =====================
        if (toGroupId != null) {
            // 入库群消息
            ChatGroupMsg groupMsg = new ChatGroupMsg();
            groupMsg.setGroupId(toGroupId);
            groupMsg.setSendUsername(currentUsername);
            groupMsg.setMsgContent(content);
            groupMsg.setMsgType(msgType);
            groupMsg.setFileUrl(fileUrl);
            groupMsg.setFileName(fileName);
            groupMsg.setFileSize(fileSize);
            groupMsgMapperStatic.insert(groupMsg);

            // ===================== 新增：批量生成群消息未读记录 =====================
            if (groupMsgReadMapperStatic != null && groupMemberMapperStatic != null) {
                // 查询当前群所有成员账号
                List<String> allGroupUserList = groupMemberMapperStatic.selectUsernameByGroupId(toGroupId);
                // 过滤掉发送消息的本人，仅给其他成员生成未读记录
                List<String> unreadUserList = allGroupUserList.stream()
                        .filter(username -> !currentUsername.equals(username))
                        .collect(Collectors.toList());

                if (!unreadUserList.isEmpty()) {
                    List<ChatGroupMsgRead> readRecordList = new ArrayList<>();
                    for (String username : unreadUserList) {
                        ChatGroupMsgRead readRecord = new ChatGroupMsgRead();
                        readRecord.setGroupMsgId(groupMsg.getId());
                        readRecord.setGroupId(toGroupId);
                        readRecord.setUsername(username);
                        readRecord.setIsRead(0); // 0=未读
                        readRecordList.add(readRecord);
                    }
                    // 批量插入未读记录
                    groupMsgReadMapperStatic.insertBatch(readRecordList);
                }
            }

            // 推送给群内所有在线成员
            sendGroupMsg(toGroupId, jsonStr);
            return;
        }

        // ===================== 分支2：原有单聊逻辑（完全保留） =====================
        ChatMsg chatMsg = new ChatMsg();
        chatMsg.setSendUsername(currentUsername);
        chatMsg.setReceiveUsername(targetUser);
        chatMsg.setMsgContent(content);
        chatMsg.setMsgType(msgType);
        chatMsg.setIsRead(0);
        chatMsg.setFileUrl(fileUrl);
        chatMsg.setFileName(fileName);
        chatMsg.setFileSize(fileSize);
        chatMsgMapperStatic.insert(chatMsg);

        if ("all".equals(targetUser)) {
            sendAllMsg(jsonStr);
        } else {
            sendSingleUserMsg(targetUser, jsonStr);
        }
    }

    @OnClose
    public void onClose() {
        List<Session> list = ONLINE.get(currentUsername);
        if (list == null) return;
        list.remove(this.session);
        // 全部终端离线后，推送下线通知
        if (list.isEmpty()) {
            ONLINE.remove(currentUsername);
            broadcastStatus(currentUsername, false);
        }
    }

    @OnError
    public void onError(Session session, Throwable e) {
        e.printStackTrace();
    }

    // ===================== 原有发送方法不变 =====================
    // 私聊单发
    public void sendSingleUserMsg(String username, String text) {
        List<Session> list = ONLINE.get(username);
        if (list == null || list.isEmpty()) return;
        for (Session s : list) {
            sendMsg(s, text);
        }
    }

    // 全员群发
    public void sendAllMsg(String text) {
        for (List<Session> arr : ONLINE.values()) {
            for (Session s : arr) {
                sendMsg(s, text);
            }
        }
    }

    // 【改造】基础发送工具：提交到异步线程池执行，解决并发写流异常
    private void sendMsg(Session session, String content) {
        WS_EXECUTOR.submit(() -> {
            if (session == null || !session.isOpen()) {
                return;
            }
            try {
                session.getBasicRemote().sendText(content);
            } catch (IOException e) {
                // 会话关闭/发送失败仅打印日志，不阻塞主线程
                e.printStackTrace();
            }
        });
    }

    // 推送上下线状态给好友
    public void broadcastStatus(String username, boolean isOnline) {
        if (userFriendServiceStatic == null) return;
        List<String> friendList = userFriendServiceStatic.getPassFriend(username);
        String tip = String.format(isOnline ? ONLINE_TIP : OFFLINE_TIP, username);
        for (String friend : friendList) {
            sendSingleUserMsg(friend, tip);
        }
    }

    // ===================== 新增：群消息批量推送 =====================
    /**
     * 向指定群所有在线成员推送群消息
     * @param groupId 群ID
     * @param jsonMsg 完整消息JSON
     */
    public void sendGroupMsg(Long groupId, String jsonMsg) {
        if (groupMemberMapperStatic == null) return;
        // 查询该群全部成员
        LambdaQueryWrapper<ChatGroupMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatGroupMember::getGroupId, groupId);
        List<ChatGroupMember> memberList = groupMemberMapperStatic.selectList(wrapper);
        // 提取群内所有用户名
        List<String> memberUsernames = memberList.stream()
                .map(ChatGroupMember::getUsername)
                .collect(Collectors.toList());
        // 遍历群成员，在线则推送
        for (String user : memberUsernames) {
            sendSingleUserMsg(user, jsonMsg);
        }
    }
}