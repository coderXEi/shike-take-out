package com.shike.mq.config;

import com.shike.constant.MqOrderConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 订单超时取消 —— RabbitMQ 拓扑声明。
 *
 * <p>采用 <b>消息级 TTL + 死信交换机（DLX）</b> 实现延时投递，拓扑如下：
 * <pre>
 *   生产者
 *     └─(routingKey)─► order.delay.exchange ──► order.delay.queue
 *                                                     │ 消息在队列中等待 TTL 到期
 *                                                     │ （TTL 由 DelayMessageProcessor 逐条设置）
 *                                                     ▼ 成为死信，按队列参数转发
 *                                           order.dlx.exchange ──► order.dlx.queue
 *                                                                        │
 *                                                                 消费者监听这里
 * </pre>
 *
 * <p><b>为什么不在延时队列上设 {@code x-message-ttl}：</b>
 * 阶梯式延时每一段的时长都不同（30s / 1min / 3min / 5min…），
 * 必须逐条设置 TTL（由 {@code DelayMessageProcessor} 写入 {@code expiration}），
 * 因此这里只配置死信参数，不配置队列级 TTL。
 *
 * <p><b>注意（队首阻塞）：</b>RabbitMQ 只保证检查队首消息是否过期。
 * 订单超时使用独立的延时队列，避免与其他延时业务互相阻塞。
 */
@Configuration
@Slf4j
public class OrderMqConfiguration {

    /**
     * 延时交换机：生产者把延时消息投递到这里。
     *
     * @return 直连交换机
     */
    @Bean
    public DirectExchange orderDelayExchange() {
        return new DirectExchange(MqOrderConstant.ORDER_DELAY_EXCHANGE, true, false);
    }

    /**
     * 延时队列：消息在此等待 TTL 到期，到期后成为死信被转发到 {@code order.dlx.exchange}。
     *
     * @return 持久化延时队列
     */
    @Bean
    public Queue orderDelayQueue() {
        return QueueBuilder.durable(MqOrderConstant.ORDER_DELAY_QUEUE)
                // 死信交换机：TTL 到期的消息转发到这里
                .deadLetterExchange(MqOrderConstant.ORDER_DLX_EXCHANGE)
                // 死信路由键：转发时使用的 routingKey，需与 DLX 绑定一致
                .deadLetterRoutingKey(MqOrderConstant.ORDER_DLX_ROUTINGKEY)
                .build();
    }

    /**
     * 延时交换机与延时队列的绑定。
     *
     * @param orderDelayQueue    延时队列
     * @param orderDelayExchange 延时交换机
     * @return 绑定关系
     */
    @Bean
    public Binding orderDelayBinding(@Qualifier("orderDelayQueue") Queue orderDelayQueue,
                                     @Qualifier("orderDelayExchange") DirectExchange orderDelayExchange) {
        return BindingBuilder.bind(orderDelayQueue)
                .to(orderDelayExchange)
                .with(MqOrderConstant.ORDER_DELAY_ROUTINGKEY);
    }

    /**
     * 死信交换机：接收从延时队列过期转发的消息。
     *
     * @return 直连交换机
     */
    @Bean
    public DirectExchange orderDlxExchange() {
        return new DirectExchange(MqOrderConstant.ORDER_DLX_EXCHANGE, true, false);
    }

    /**
     * 死信队列：消费者监听此队列，执行「续发下一段」或「取消订单」逻辑。
     *
     * @return 持久化队列
     */
    @Bean
    public Queue orderDlxQueue() {
        return QueueBuilder.durable(MqOrderConstant.ORDER_DLX_QUEUE).build();
    }

    /**
     * 死信交换机与死信队列的绑定。
     *
     * @param orderDlxQueue    死信队列
     * @param orderDlxExchange 死信交换机
     * @return 绑定关系
     */
    @Bean
    public Binding orderDlxBinding(@Qualifier("orderDlxQueue") Queue orderDlxQueue,
                                   @Qualifier("orderDlxExchange") DirectExchange orderDlxExchange) {
        return BindingBuilder.bind(orderDlxQueue)
                .to(orderDlxExchange)
                .with(MqOrderConstant.ORDER_DLX_ROUTINGKEY);
    }

    // 练习创建 交换机和队列 以及绑定关系


}
