package com.example.mgis.websocket;

import com.alibaba.fastjson2.JSONObject;
import com.example.mgis.controller.mapper.chart.ChatMsgMapper;
import com.example.mgis.entity.chart.ChatMsg;
import jakarta.annotation.Resource;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ServerEndpoint(value = "/ws/chat", configurator = WsOriginConfig.class)
public class ChatServer {
    //全局在线用户
    public static final ConcurrentHashMap<String, List<Session>> ONLINE = new ConcurrentHashMap<>();
    private static ChatMsgMapper chatMsgMapperStatic;

    private Session session;
    private String currentUsername;

    //Spring注入静态Mapper
    @Resource
    public void setChatMsgMapper(ChatMsgMapper mapper){
        ChatServer.chatMsgMapperStatic = mapper;
    }

    @OnOpen
    public void onOpen(Session session) throws IOException {
        this.session = session;
        String query = session.getQueryString();
        String userId = null;
        if(query != null && query.startsWith("userId=")){
            userId = query.substring("userId=".length());
        }
        if(userId == null || userId.isBlank()){
            session.close();
            return;
        }
        this.currentUsername = userId;
        ONLINE.computeIfAbsent(userId,k->new ArrayList<>()).add(session);
    }

    @OnMessage
    public void onMessage(String jsonStr){
        JSONObject json = JSONObject.parseObject(jsonStr);
        String targetUser = json.getString("to");
        String content = json.getString("msg");

        //消息入库
        ChatMsg chatMsg = new ChatMsg();
        chatMsg.setSendUsername(currentUsername);
        chatMsg.setReceiveUsername(targetUser);
        chatMsg.setMsgContent(content);
        chatMsg.setMsgType(1);
        chatMsg.setIsRead(0);
        chatMsgMapperStatic.insert(chatMsg);

        String sendText = "["+currentUsername+"]："+content;
        if("all".equals(targetUser)){
            sendAllMsg(sendText);
        }else{
            sendSingleUserMsg(targetUser,sendText);
        }
    }

    @OnClose
    public void onClose(){
        List<Session> list = ONLINE.get(currentUsername);
        if(list == null) return;
        list.remove(this.session);
        if(list.isEmpty()){
            ONLINE.remove(currentUsername);
        }
    }

    @OnError
    public void onError(Session session,Throwable e){
        //不再重复关闭
    }

    //私聊单发
    public void sendSingleUserMsg(String username,String text){
        List<Session> list = ONLINE.get(username);
        if(list == null || list.isEmpty()) return;
        for(Session s : list){
            sendMsg(s,text);
        }
    }

    //全员群发
    public void sendAllMsg(String text){
        for(List<Session> arr : ONLINE.values()){
            for(Session s : arr){
                sendMsg(s,text);
            }
        }
    }

    //发送前判断会话状态，规避关闭报错
    private void sendMsg(Session session,String content){
        if(session == null || !session.isOpen()) return;
        try{
            session.getBasicRemote().sendText(content);
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}