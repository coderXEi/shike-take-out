package com.shike.mq.publisher;

import com.shike.constant.MqOrderConstant;
import com.shike.dto.event.OrderCreatedEvent;
import com.shike.mq.DelayMessageProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 订单超时延时消息发送者。
 *
 * <p>监听 {@link OrderCreatedEvent}，在<b>下单事务提交之后</b>发送第一段延时消息。
 * 使用 {@link TransactionalEventListener} 且 {@code phase = AFTER_COMMIT}，保证：
 * <ul>
 *   <li>订单数据已真正落库、对其他数据库连接可见，消费者一定查得到；</li>
 *   <li>下单事务若回滚，消息不会被发出，不会产生「订单不存在」的孤儿消息。</li>
 * </ul>
 *
 * <p><b>注意：</b>AFTER_COMMIT 阶段事务已提交，此处抛出的异常<b>不会</b>导致订单回滚。
 * 因此这里对发送异常做兜底 catch + 日志，避免影响主流程；
 * 发送失败的消息由 {@code OrderTask} 定时扫描做最终兜底。
 */
@Component
@Slf4j
public class OrderTimeoutMqSender {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 构造器注入（项目新代码风格）。
     *
     * @param rabbitTemplate RabbitMQ 模板
     */
    public OrderTimeoutMqSender(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * 下单事务提交后，发送订单超时的第一段延时消息。
     * <p>后续各段由消费者在每段到期时续发（阶梯式，见 {@link DelayMessageProcessor}），
     * 用户中途支付后不再续发，避免无用消息积压。
     *
     * @param event 订单创建事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCreated(OrderCreatedEvent event) {
        if (event == null || event.getOrderNumber() == null || event.getOrderNumber().isEmpty()) {
            log.warn("[订单超时消息] 收到无效的订单创建事件，忽略：{}", event);
            return;
        }
        // 下单后从阶梯第 1 段开始
        sendDelayMessage(event.getOrderNumber(), 1);
    }

    /**
     * 发送订单超时阶梯中<b>第 {@code step} 段</b>的延时消息。
     *
     * <p>下单（{@code step = 1}）与消费者续发（{@code step = 上一段 + 1}）共用此方法，
     * 保证两处发送逻辑（交换机、routingKey、TTL、消息头）完全一致。
     *
     * <p><b>传参约定：</b>消息体携带的是订单业务编号 {@code orders.number}（而非自增主键 id）；
     * {@code step} 为「接下来要等待的段号」，从 1 开始，最大为
     * {@code MqOrderConstant.ORDER_TIMEOUT_LADDER.size()}。消费者收到第 N 段到期的消息后，
     * 调用 {@code sendDelayMessage(orderNumber, N + 1)} 即可续上下一段。
     * 若 {@code step} 超出阶梯范围，说明已到阶梯末尾，<b>不再发送</b>并返回 {@code false}，
     * 由调用方决定是否取消订单。
     *
     * @param orderNumber 订单号（消息体，订单唯一业务标识）
     * @param step        要发送的段号，从 1 开始
     * @return true 表示该段延时消息已成功投递；false 表示已到阶梯末尾或发送失败
     */
    public boolean sendDelayMessage(String orderNumber, int step) {
        if (step < 1 || step > MqOrderConstant.ORDER_TIMEOUT_LADDER.size()) {
            log.warn("[订单超时消息] 段号超出阶梯范围，不发送。orderNumber={}, step={}, 总段数={}",
                    orderNumber, step, MqOrderConstant.ORDER_TIMEOUT_LADDER.size());
            return false;
        }

        // 从共享阶梯定义中截取「第 step 段起」的剩余部分，next() 弹出队首即为本段 TTL
        DelayMessageProcessor processor = DelayMessageProcessor
                .ofLadder(MqOrderConstant.ladderFrom(step))
                .withHeader("bizType", MqOrderConstant.ORDER_TIMEOUT_BIZ_TYPE)
                .withHeader(MqOrderConstant.ORDER_TIMEOUT_STEP_HEADER, step);

        try {
            long delay = processor.next().getDelayMillis();
            rabbitTemplate.convertAndSend(
                    MqOrderConstant.ORDER_DELAY_EXCHANGE,
                    MqOrderConstant.ORDER_DELAY_ROUTINGKEY,
                    orderNumber,
                    processor);
            log.info("[订单超时消息] 第 {} 段延时消息已发送，orderNumber={}, delay={}ms", step, orderNumber, delay);
            return true;
        } catch (Exception e) {
            // 事务已提交，异常无法回滚，此处仅记录，交由定时任务兜底
            log.error("[订单超时消息] 第 {} 段延时消息发送失败，orderNumber={}", step, orderNumber, e);
            return false;
        }
    }

}
