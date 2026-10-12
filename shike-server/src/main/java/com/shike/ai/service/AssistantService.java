package com.shike.ai.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;

import java.util.List;

/**
 * 商家端智能客服服务。
 *
 * <p>通过 LangChain4j Spring Boot Starter 的 {@link AiService} 声明式装配：
 * 由 starter 自动扫描并生成实现，无需手写 {@code AiServices.builder()} 的 {@code @Bean}。</p>
 *
 * <p>装配默认使用「自动」模式（{@code wiringMode=AUTOMATIC}）：按类型从 Spring 容器中
 * 解析 {@code ChatModel} 与 {@code ContentRetriever}。若容器中存在多个同类型 Bean，
 * 需显式指定 {@code chatModel="beanName"} / {@code contentRetriever="beanName"}。</p>
 */
public interface AssistantService {
    // 直接指定本地文件 包含提示词
    @SystemMessage(fromResource = "system-prompt.txt")
    String chat(String userMessage);


    @SystemMessage(fromResource = "system-prompt.txt")
    Report chatForReport(String userMessage);


    @SystemMessage("你是志愿填报的老师，你只负责志愿填报的任务，用户的其他任务不予回答")
    String chatWithFlux(String userMessage);
    // record 关键字， 以写函数的形式 来创建一个类
    record Report(String name, List<String> suggestionList){};
}
