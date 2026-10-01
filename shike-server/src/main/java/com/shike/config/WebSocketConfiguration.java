package com.shike.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import javax.websocket.server.ServerContainer;

/**
 * WebSocket配置类，用于注册WebSocket的Bean
 */
@Configuration
@ConditionalOnClass(ServerContainer.class)
public class WebSocketConfiguration {

    /**
     * 仅在真实的内嵌 Servlet 容器（如内置 Tomcat）环境下才注册
     * ServerEndpointExporter。在 SpringBootTest（MOCK 环境）下不存在
     * javax.websocket.server.ServerContainer，注册该 Bean 会导致启动失败。
     */
    @Bean
    @Conditional(OnServerContainerAvailableCondition.class)
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }

}
