package com.example.mgis.websocket;

import jakarta.websocket.server.ServerEndpointConfig;

public class WsOriginConfig extends ServerEndpointConfig.Configurator {
    @Override
    public boolean checkOrigin(String originHeaderValue) {
        if (originHeaderValue == null) {
            return false;
        }
        // 放行本地、10.10 内网段，不管端口是什么
        return originHeaderValue.contains("localhost")
                || originHeaderValue.contains("127.0.0.1")
                || originHeaderValue.startsWith("http://10.10.");
    }
}