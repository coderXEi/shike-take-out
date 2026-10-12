package com.shike.constant;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * rabbitmq  订单延时队列相关常量
 */
@Slf4j
public class MqOrderConstant {

    // 订单延时消息交换机、队列 命名
    public static final String ORDER_DELAY_EXCHANGE = "order.delay.exchange";
    public static final String ORDER_DELAY_QUEUE = "order.delay.queue";
    public static final String ORDER_DELAY_ROUTINGKEY = "order.delay.routingkey";

    // 死信队列命名（延时队列到点后，消息成为死信转发到这里）
    public static final String ORDER_DLX_EXCHANGE = "order.dlx.exchange";
    public static final String ORDER_DLX_QUEUE = "order.dlx.queue";
    public static final String ORDER_DLX_ROUTINGKEY = "order.dlx.routingkey";

    // 延时消息的业务类型头，消费者据此区分消息用途
    public static final String ORDER_TIMEOUT_BIZ_TYPE = "ORDER_TIMEOUT";

    // 订单超时取消原因
    public static final String ORDER_TIMEOUT_CANCEL_REASON = "订单超时，自动取消";

    // 订单超时消息头：当前已走到阶梯的第几段（从 1 开始）
    public static final String ORDER_TIMEOUT_STEP_HEADER = "timeoutStep";

    // 订单超时延时消息体约定：携带的是业务订单号（orders.number），而非订单表自增主键 id。
    // id 是数据库自增实现细节（换库/迁移/分库分表后可能变化），订单号才是订单的唯一业务标识。

    /**
     * 订单超时阶梯式延时梯度（单位毫秒）：30s → 30s → 1min → 3min → 5min → 5min。
     * <p>累计 30 + 30 + 60 + 180 + 300 + 300 = 900s = 15 分钟，与订单支付超时时长对齐。
     * <p>发送端与消费端<b>共用同一份定义</b>，保证阶梯顺序一致。
     */
    public static final List<Long> ORDER_TIMEOUT_LADDER = Collections.unmodifiableList(Arrays.asList(
            Duration.ofSeconds(30).toMillis(),   // 第 1 段：30s
            Duration.ofSeconds(30).toMillis(),   // 第 2 段：30s
            Duration.ofMinutes(1).toMillis(),    // 第 3 段：1min
            Duration.ofMinutes(3).toMillis(),    // 第 4 段：3min
            Duration.ofMinutes(5).toMillis(),    // 第 5 段：5min
            Duration.ofMinutes(5).toMillis()     // 第 6 段：5min（补足至 15min）
    ));

    /**
     * 截取从第 {@code fromStep} 段（含）开始的剩余阶梯。
     * <p>消费端可以据此从消息头的 {@code timeoutStep} 续发下一段，
     * 而无需把整份阶梯序列塞进消息体里。
     *
     * @param fromStep 起始段号，从 1 开始；超过总段数时返回空列表
     * @return 剩余阶梯（单位毫秒），不可变
     */
    public static List<Long> ladderFrom(int fromStep) {
        if (fromStep < 1 || fromStep > ORDER_TIMEOUT_LADDER.size()) {
            log.warn("请求的阶梯起始段号非法，fromStep={}, 总段数={}", fromStep, ORDER_TIMEOUT_LADDER.size());
            return Collections.emptyList();
        }
        return ORDER_TIMEOUT_LADDER.subList(fromStep - 1, ORDER_TIMEOUT_LADDER.size());
    }
}
