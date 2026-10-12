package com.shike.config;


import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;


/**
 * 发送者可靠性 - 确认机制
 */
@Configuration
@Slf4j
public class MqConfirmConfig implements ApplicationContextAware {
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        RabbitTemplate rabbitTemplate = applicationContext.getBean(RabbitTemplate.class);

        // 配置回调 confirm回调 确认消息是否抵达交换机
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
                // correlationData  可能为null  发消息但没传data
            if(correlationData ==null) {
                log.warn("confirm 回调，correlationData为null");
            }
            String msgId = null;
            if (correlationData != null) {
                msgId = correlationData.getId();
            }
            if(ack) {
                // ack 为true 消息到达交换机  同时确保没有return 代表无异常
                log.info("confirm消息成功 到达交换机msgId ={}",msgId);
                // 还要判断 没有return回调触发 这表示 投递队列成功
            } else {
                log.error("[confirm] 消息投递交换机失败 msgId ={},原因={}",msgId,cause);
                // 数据库更新消息状态  投递失败 status = 2 留给定时任务重试
            }
        });
        // 消息到达交换机，但路由不到队列 消息退回
        rabbitTemplate.setReturnsCallback(returnedMessage -> {
            // 获取消息id
            String correlationId = returnedMessage.getMessage().getMessageProperties().getCorrelationId();
            log.error("[return回调] 消息路由失败,msgId ={},exchange={},routingKey={}，回复文本={}",
                    correlationId,returnedMessage.getExchange(),returnedMessage.getRoutingKey(),returnedMessage);
        });

    }
}
