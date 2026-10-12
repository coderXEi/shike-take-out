package com.shike.controller.user;


import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/chat")
public class ChatController {

    @Autowired
    private ChatModel model;

    @GetMapping
    public String chat(@RequestParam String msg) {
        log.info("智能客服提问: {}", msg);
        List<ChatMessage> messages = List.of(
                SystemMessage.systemMessage("你是外卖平台客服，基于已有信息来回答，若有不知道的消息请回答不知道 而不是编造"),
                UserMessage.userMessage(msg)
        );
        ChatResponse chatResponse = model.chat(messages);

        // 只返回模型回答文本，避免把 langchain4j 的 Java toString() 结构直接暴露给前端
        AiMessage aiMessage = chatResponse.aiMessage();
        String answer = aiMessage == null ? null : aiMessage.text();
        log.info("智能客服回答: {}", answer);
        return answer == null ? "" : answer;
    }

}
