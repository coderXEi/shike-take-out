package com.shike.config;


import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
@ConditionalOnProperty(prefix = "spring.rabbitmq.listener.simple.retry.enabled",havingValue = "true")
public class ErrorConfiguration {

    // 定义兜底的异常 消息处理方案
    /*
    * 当消费者 可靠性中采取 RepublishMessageRecoverer
    * 即 在多次失败重试后 将消息转给error 交换机 消息被接收后 直接告警 让人工介入
    *
    * */

    @Bean
    public DirectExchange errorExchange() {
        return new DirectExchange("error.direct");
    }

    @Bean
    public Queue errorQueue(){
        return new Queue("error.queue");
    }

    @Bean
    public Binding errorBinding(Queue errorQueue,DirectExchange errorExchange){

        return BindingBuilder.bind(errorQueue).to(errorExchange).with("error");
    }

    // 消息转换器
    @Bean
    public MessageRecoverer messageRecoverer(RabbitTemplate rabbitTemplate){

        log.info("加载错误处理交换机");
        return new RepublishMessageRecoverer(rabbitTemplate,"error.direct","error");
    }
}
