package com.shike.mq.consumer;


import com.shike.constant.MqOrderConstant;
import com.shike.entity.Orders;
import com.shike.mq.publisher.OrderTimeoutMqSender;
import com.shike.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 订单超时延时消息消费者。
 *
 * <p>监听死信队列 {@code order.dlx.queue}：延时消息在 {@code order.delay.queue} 中等到 TTL 到期后
 * 成为死信，由 DLX 转发到这里。每收到一条消息，说明「又过完了阶梯中的一段」，处理逻辑：
 * <ol>
 *   <li><b>幂等：</b>按订单号查订单，若不存在或已非「待付款」，说明用户已支付/订单已处理，直接返回（ack），不再续发；</li>
 *   <li><b>续发：</b>若还有下一段阶梯，调用 {@link OrderTimeoutMqSender#sendDelayMessage(String, int)}
 *       发送下一段延时消息（{@code step = 当前段号 + 1}）；</li>
 *   <li><b>收尾：</b>若已到阶梯末尾（累计满 15 分钟仍未支付），调用
 *       {@link OrderService#closeTimeOutOrder(String)} 关闭订单。</li>
 * </ol>
 *
 * <p><b>消息格式（与 {@link OrderTimeoutMqSender} 约定一致）：</b>
 * <pre>
 * │ 消息体（String）   │ 订单号 orders.number（订单唯一业务标识，非自增 id）│
 * │ 消息头 bizType     │ "ORDER_TIMEOUT"                              │
 * │ 消息头 timeoutStep │ 当前段号，从 1 开始                          │
 * │ 消息头 expiration  │ 本段时长（毫秒），由 DelayMessageProcessor 写入 │
 * </pre>
 *
 * <p><b>异常处理：</b>方法抛出异常时由 Spring AMQP 的重试机制接管，
 * 重试耗尽后由 {@code RepublishMessageRecoverer} 转入 error 交换机（见 {@code ErrorConfiguration}）。
 */
@Component
@Slf4j
public class OrderTimeoutConsumer {

    private final OrderService orderService;

    private final OrderTimeoutMqSender orderTimeoutMqSender;

    /**
     * 构造器注入（项目新代码风格）。
     *
     * @param orderService         订单服务
     * @param orderTimeoutMqSender 订单超时消息发送者
     */
    public OrderTimeoutConsumer(OrderService orderService, OrderTimeoutMqSender orderTimeoutMqSender) {
        this.orderService = orderService;
        this.orderTimeoutMqSender = orderTimeoutMqSender;
    }

    /**
     * 消费一段订单超时延时消息。
     *
     * @param message 原始 AMQP 消息，消息体为订单号（orders.number），消息头含 timeoutStep
     */
    @RabbitListener(queues = MqOrderConstant.ORDER_DLX_QUEUE)
    public void consume(Message message) {

        // 1. 解析消息体与消息头
        String orderNumber = new String(message.getBody(), StandardCharsets.UTF_8).trim();
        MessageProperties props = message.getMessageProperties();
        Map<String, Object> headers = props.getHeaders();

        Integer currentStep = resolveStep(headers);

        log.info("收到订单超时延迟消息 orderNumber:{}, currentStep:{}", orderNumber, currentStep);
        if (orderNumber.isEmpty()) {
            // 非法消息（订单号为空）无需重试，直接丢弃并告警
            log.error("订单超时消息体为空（订单号缺失），丢弃。headers={}", headers);
            return;
        }
        if (currentStep == null) {
            // 非法消息（缺少段号）无需重试，直接丢弃并告警
            log.error("订单超时消息缺少 timeoutStep 头，丢弃。orderNumber={}, headers={}", orderNumber, headers);
            return;
        }

        // 2. 幂等：按订单号查订单，只有仍处于「待付款」才需要继续
        Orders orders = orderService.getByNumber(orderNumber);
        if (orders == null) {
            log.warn("订单不存在，丢弃延时消息。orderNumber={}", orderNumber);
            return;
        }
        if (!Orders.PENDING_PAYMENT.equals(orders.getStatus())) {
            log.info("订单已支付或已处理，不再续发延时消息。orderNumber={}, status={}, payStatus={}",
                    orderNumber, orders.getStatus(), orders.getPayStatus());
            return;
        }

        // 3. 判断是否还有下一段阶梯
        int nextStep = currentStep + 1;
        int totalStep = MqOrderConstant.ORDER_TIMEOUT_LADDER.size();
        if (nextStep > totalStep) {
            // 已到阶梯末尾（累计满 15 分钟仍未支付）——关闭订单
            log.info("订单超时阶梯已走完，关闭订单。orderNumber={}, totalStep={}", orderNumber, totalStep);
            orderService.closeTimeOutOrder(orderNumber);
            return;
        }

        // 4. 尚未超时：续发下一段延时消息，继续等待
        log.info("订单尚未支付，续发下一段延时消息。orderNumber={}, nextStep={}", orderNumber, nextStep);
        orderTimeoutMqSender.sendDelayMessage(orderNumber, nextStep);
    }

    /**
     * 从消息头解析当前段号。
     *
     * <p>发送时 {@code timeoutStep} 写入的是 {@code Integer}，但消息经 RabbitMQ 传输后
     * 数值类型可能被还原为 {@code Long}，故这里统一按 {@link Number} 处理，避免
     * {@code ClassCastException}。
     *
     * @param headers 消息头
     * @return 段号；不存在或非法时返回 null
     */
    private Integer resolveStep(Map<String, Object> headers) {
        Object raw = headers.get(MqOrderConstant.ORDER_TIMEOUT_STEP_HEADER);
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number) {
            return ((Number) raw).intValue();
        }
        try {
            return Integer.valueOf(raw.toString());
        } catch (NumberFormatException e) {
            log.error("timeoutStep 头格式非法：{}", raw);
            return null;
        }
    }
}
