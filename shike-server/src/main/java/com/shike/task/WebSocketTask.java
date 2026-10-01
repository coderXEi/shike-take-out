package com.shike.task;

import com.alibaba.fastjson.JSON;
import com.shike.websocket.WebSocketServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Component
public class WebSocketTask {
    @Autowired
    private WebSocketServer webSocketServer;

    /**
     * 通过WebSocket每隔5秒向客户端发送消息
     */
//    @Scheduled(cron = "0/5 * * * * ?")
    public void sendMessageToClient() {

        Map<String, Object> map = new HashMap<>();
        map.put("type",1);
        map.put("orderId",111);
        map.put("content","订单来了");
        String jsonString = JSON.toJSONString(map);

        webSocketServer.sendToAllClient(jsonString);
    }
}
