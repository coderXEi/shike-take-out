package com.shike.ai.config;


import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/*
* chatMemoryProvider
* 多个ChatModel
* 共享工具Bean
* */
@Configuration
public class AiConfiguration {

    // 此处建立 多个 ChatModel bean 若多应用使用不同模型

    @Bean
    public ChatMemoryProvider chatMemoryProvider() {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(20)
                .build();
    }
}
