package com.shike.test;

import com.shike.ai.service.AssistantService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class LLM {

    @Resource
    AssistantService assistantService;
    @Test
    public void testLangchain4j() {
        String chat = assistantService.chat("你好  当前外卖平台 有什么商品");
        System.out.println(chat);
    }

    @Test
    public void chatForReport() {
        AssistantService.Report report = assistantService.chatForReport("你好 你们店有什么招牌吗，请给出一些示例");

        System.out.println(report);
    }

    @Test
    public void chatForReport2() {
        String s = assistantService.chatWithFlux("信息安全专业有哪些推荐的学习");

        System.out.println(s);
    }
}
