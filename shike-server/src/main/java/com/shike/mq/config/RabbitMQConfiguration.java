package com.shike.mq.config;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置。
 * <p>
 * 声明 {@link RabbitAdmin}，使 {@code @RabbitListener} 上的
 * {@code @Queue}/{@code @Exchange}/{@code @QueueBinding} 定义
 * 能在应用启动时自动创建到 broker（队列、交换机、绑定）。
 * 不提供该 bean 时，注解里的声明不会生效，监听容器做被动声明会报 404。
 */
@Configuration
public class RabbitMQConfiguration {

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        // 声明失败时抛出异常，便于尽早发现问题
        rabbitAdmin.setIgnoreDeclarationExceptions(false);
        return rabbitAdmin;
    }

    /** 替代默认的 JDK 序列化器，使消息体为可读 JSON  同时可以配置唯一id 消费者可靠性 幂等处理*/
    @Bean
    public MessageConverter messageConverter() {
    // 配置自动创建消息ID 用于识别不同消息，也可在业务中基于ID判断是否是重复消息
        Jackson2JsonMessageConverter messageConverter = new Jackson2JsonMessageConverter();
        messageConverter.setCreateMessageIds(true);

        return messageConverter;
    }


}
