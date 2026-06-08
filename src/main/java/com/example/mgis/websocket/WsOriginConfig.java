package com.example.mgis.websocket;

import jakarta.websocket.server.ServerEndpointConfig;

public class WsOriginConfig extends ServerEndpointConfig.Configurator {
    @Override
    public boolean checkOrigin(String originHeaderValue) {
        if(originHeaderValue == null) return false;
        //同时放行 localhost:1609、127.0.0.1:1609 两种来源
        return originHeaderValue.equals("http://localhost:1609")
                || originHeaderValue.equals("http://127.0.0.1:1609");
    }
}