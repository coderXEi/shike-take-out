package com.shike.ai.service;


import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class AiCreator {

    @Resource
    private ChatModel model;

    /**
     * 默认对话记忆：由 {@link com.shike.ai.config.AiConfiguration} 暴露的
     * {@link ChatMemoryProvider} 按固定 id 派生。
     *
     * <p>{@link AssistantService#chat(String)} 未使用 {@code @MemoryId}，
     * 属于单会话签名，因此这里取一个固定 id 的记忆实例。</p>
     */
    @Autowired
    private ChatMemoryProvider chatMemoryProvider;

    private static final Object DEFAULT_MEMORY_ID = "default";

    /**
     * RAG 检索器为可选依赖：{@code shike.rag.enabled=false} 时容器中不存在该 Bean，
     * 用 {@link ObjectProvider} 延迟解析，避免启动阶段因缺少 Bean 直接失败。
     */
    @Resource
    private ObjectProvider<ContentRetriever> contentRetrieverProvider;

    @Bean
    public AssistantService assistantService() {

        // 构建者模式 来构造ai service
        // 创建 一个ai服务
//        return AiServices.create(AssistantService.class, model);

        AiServices<AssistantService> builder = AiServices.builder(AssistantService.class)
                .chatModel(model)
                .chatMemory(chatMemoryProvider.get(DEFAULT_MEMORY_ID));

        // RAG 开启时（RagConfiguration 生效）才挂载检索器，关闭时退化为普通对话
        ContentRetriever contentRetriever = contentRetrieverProvider.getIfAvailable();
        if (contentRetriever != null) {
            builder.contentRetriever(contentRetriever);  //RAG检索
            log.info("AssistantService 已启用 RAG 检索增强");
        } else {
            log.warn("未找到 ContentRetriever（shike.rag.enabled=false?），AssistantService 以纯对话模式运行");
        }

        return builder.build();
    }
}
