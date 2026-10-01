package com.shike.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.ServletContext;
import javax.websocket.server.ServerContainer;

/**
 * 判断当前运行环境是否存在真实的 {@link ServerContainer}（即运行在内嵌
 * Servlet 容器中）。在 SpringBootTest 默认的 MOCK 环境下不存在该属性，
 * 此时条件不成立，从而跳过 ServerEndpointExporter 的注册。
 */
public class OnServerContainerAvailableCondition implements Condition {

    private static final String SERVER_CONTAINER_ATTRIBUTE =
            "javax.websocket.server.ServerContainer";

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        if (!(context.getResourceLoader() instanceof WebApplicationContext)) {
            return false;
        }
        WebApplicationContext webApplicationContext =
                (WebApplicationContext) context.getResourceLoader();
        ServletContext servletContext = webApplicationContext.getServletContext();
        if (servletContext == null) {
            return false;
        }
        return servletContext.getAttribute(SERVER_CONTAINER_ATTRIBUTE) != null;
    }
}
