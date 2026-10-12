package com.shike.mq;

import lombok.Getter;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * 延时消息处理器。
 *
 * <p>基于 RabbitMQ 的 <b>队列 TTL + 死信交换机（DLX）</b> 模式实现延时投递：
 * 生产者把消息投递到一个设置了 {@code x-message-ttl} 的延时队列（或直接由本处理器
 * 在单条消息上设置 {@code expiration}），消息在队列中存活到期后成为死信，
 * 由死信交换机转发到真正的业务队列，消费者此时才收到消息。
 *
 * <p>使用方式（一行搞定，发延时消息时直接把它作为最后一个参数传入）：
 * <pre>{@code
 * // 1. 单段延时
 * rabbitTemplate.convertAndSend(
 *         "order.delay.exchange",
 *         "order.delay",
 *         orderId,
 *         DelayMessageProcessor.ofMinutes(15));
 *
 * // 2. 阶梯式延时：30s - 30s - 1min - 3min - 5min - 5min，累计 15 分钟
 * DelayMessageProcessor p = DelayMessageProcessor.ofLadder();
 * if (p.hasNext()) {
 *     rabbitTemplate.convertAndSend("order.delay.exchange", "order.delay", orderId, p.next());
 * }
 * }</pre>
 *
 * <p><b>阶梯式（ladder）说明：</b>内部用 {@code List<Long>}（单位毫秒）保存各段时间，
 * 每调用一次 {@link #next()} 就 {@code delay.remove(0)} 弹出队首时间并据此设置 TTL；
 * {@link #hasNext()} 判断是否还有下一段时间。
 * 这样可以把「15 分钟一次性长延时」拆成若干段短延时逐段续发，
 * 一旦用户中途支付，后续消息就不会再产生，避免无用消息在队列里积压。
 * 消费端在 {@code hasNext()} 为 false 时说明已到阶梯末尾，可直接取消订单。
 *
 * <p><b>注意：</b>本处理器只负责在单条消息上写入 TTL（{@code expiration}）等属性，
 * 并不创建交换机 / 队列。使用前需保证延时交换机、延时队列（带 {@code x-dead-letter-exchange}
 * 与可选 {@code x-message-ttl}）、业务交换机、业务队列及绑定关系已经声明。
 *
 * @see #of(long)
 * @see #ofLadder()
 * @see #next()
 * @see #hasNext()
 */
public class DelayMessageProcessor implements MessagePostProcessor {

    /**
     * 单条消息的 TTL 属性名（RabbitMQ 约定：{@code expiration}，字符串形式的毫秒数）。
     */
    public static final String EXPIRATION_HEADER = "expiration";

    /**
     * 默认延时时间：15 分钟（与订单超时取消时长保持一致）。
     */
    public static final long DEFAULT_DELAY_MILLIS = Duration.ofMinutes(15).toMillis();

    /**
     * 默认阶梯式延时梯度（单位毫秒）：30s → 30s → 1min → 3min → 5min → 5min。
     * <p>累计 30s + 30s + 60s + 180s + 300s + 300s = 900s = 15 分钟，
     * 与 {@link #DEFAULT_DELAY_MILLIS} 对齐。
     */
    public static final List<Long> DEFAULT_LADDER = Arrays.asList(
            Duration.ofSeconds(30).toMillis(),   // 30s
            Duration.ofSeconds(30).toMillis(),   // 30s
            Duration.ofMinutes(1).toMillis(),    // 1min
            Duration.ofMinutes(3).toMillis(),    // 3min
            Duration.ofMinutes(5).toMillis(),    // 5min
            Duration.ofMinutes(5).toMillis()     // 5min（补足至 15min）
    );

    /**
     * 当前用于写入消息 TTL 的延时时间，单位毫秒。
     * <p>单段模式下即构造时传入的值；阶梯模式下为最近一次 {@link #next()} 弹出的值。
     */
    @Getter
    private long delayMillis;

    /**
     * 阶梯式延时时间序列，单位毫秒；{@link #next()} 会从队首弹出。
     * <p>非阶梯模式（单段）下为 {@code null}。
     */
    private List<Long> delay;

    /**
     * 是否持久化消息，默认持久化，避免 broker 重启导致延时消息丢失。
     */
    private boolean persistent = true;

    /**
     * 待写入的自定义消息头。
     */
    private final Map<String, Object> headers = new HashMap<>();

    private DelayMessageProcessor(long delayMillis) {
        setDelayMillis(delayMillis);
    }

    /**
     * 内部构造：阶梯模式。
     *
     * @param ladder 阶梯延时序列（单位毫秒），会被复制为可变列表
     */
    private DelayMessageProcessor(List<Long> ladder) {
        setLadder(ladder);
    }


    /**
     * 创建秒级延时处理器。
     *
     * @param seconds 延时秒数，必须 &gt;= 0
     * @return 处理器实例
     */
    public static DelayMessageProcessor ofSeconds(long seconds) {
        return new DelayMessageProcessor(Duration.ofSeconds(seconds).toMillis());
    }

    /**
     * 创建分钟级延时处理器。
     *
     * @param minutes 延时分钟数，必须 &gt;= 0
     * @return 处理器实例
     */
    public static DelayMessageProcessor ofMinutes(long minutes) {
        return new DelayMessageProcessor(Duration.ofMinutes(minutes).toMillis());
    }

    /**
     * 创建<b>阶梯式</b>延时处理器，使用默认梯度 {@link #DEFAULT_LADDER}。
     *
     * @return 处理器实例
     */
    public static DelayMessageProcessor ofLadder() {
        return new DelayMessageProcessor(DEFAULT_LADDER);
    }

    /**
     * 创建<b>阶梯式</b>延时处理器。
     *
     * @param delays 各段延时（单位毫秒），按先后顺序传入；不能为 null / 空，元素不能为负
     * @return 处理器实例
     */
    public static DelayMessageProcessor ofLadder(Long... delays) {
        Objects.requireNonNull(delays, "delays 不能为 null");
        return new DelayMessageProcessor(Arrays.asList(delays));
    }

    /**
     * 创建<b>阶梯式</b>延时处理器。
     *
     * @param delays 各段延时（单位毫秒）列表，不能为 null / 空
     * @return 处理器实例
     */
    public static DelayMessageProcessor ofLadder(List<Long> delays) {
        return new DelayMessageProcessor(delays);
    }

    /**
     * 取出并<b>弹出</b>下一段延时（{@code delay.remove(0)}），同时用它作为本次消息的 TTL。
     * <p>返回的正是当前实例，因此可以直接写：
     * {@code rabbitTemplate.convertAndSend(ex, rk, body, processor.next())}。
     *
     * @return 当前处理器实例，已按弹出值设置 {@link #delayMillis}
     * @throws NoSuchElementException 阶梯已耗尽
     */
    public DelayMessageProcessor next() {
        if (!hasNext()) {
            throw new NoSuchElementException("阶梯延时已耗尽，无下一段时间");
        }
        // 弹出队首时间作为本段延时
        this.delayMillis = this.delay.remove(0);
        return this;
    }

    /**
     * 判断是否还有下一段延时时间。
     * <p>典型用法：下单时构造阶梯处理器，{@code hasNext()} 为 true 才发下一条延时消息；
     * 消费端每收到一次消息都重新 {@link #next()}，若 {@code hasNext()} 已为 false，
     * 说明已到阶梯末尾，此时<b>直接取消订单</b>，不再续发。
     *
     * @return true 表示还有下一段时间；false 表示阶梯耗尽（单段模式恒为 false）
     */
    public boolean hasNext() {
        return this.delay != null && !this.delay.isEmpty();
    }

    /**
     * 返回剩余阶梯的只读快照（单位毫秒），便于日志与测试。
     *
     * @return 剩余时间列表；单段模式返回空列表
     */
    public List<Long> remaining() {
        return this.delay == null ? new ArrayList<>() : new ArrayList<>(this.delay);
    }

    /**
     * 在消息进入队列前写入 TTL 与投递模式等属性。
     *
     * @param message 待发送消息
     * @return 处理后的消息
     * @throws AmqpException 处理失败时抛出
     */
    @Override
    public Message postProcessMessage(Message message) throws AmqpException {
        MessageProperties properties = message.getMessageProperties();
        // 单条消息 TTL，RabbitMQ 要求为字符串形式的毫秒数
        properties.setExpiration(String.valueOf(delayMillis));
        properties.setDeliveryMode(persistent
                ? MessageDeliveryMode.PERSISTENT
                : MessageDeliveryMode.NON_PERSISTENT);
        // 写入调用方追加的自定义头
        headers.forEach(properties::setHeader);
        return message;
    }

    /**
     * 设置延时时间（链式，单段模式）。
     *
     * @param delayMillis 延时时间，单位毫秒，必须 &gt;= 0
     * @return 当前实例
     */
    public DelayMessageProcessor delay(long delayMillis) {
        setDelayMillis(delayMillis);
        return this;
    }

    /**
     * 设置延时时间（链式，单段模式）。
     *
     * @param duration 延时时间，不能为 null 且不能为负
     * @return 当前实例
     */
    public DelayMessageProcessor delay(Duration duration) {
        Objects.requireNonNull(duration, "duration 不能为 null");
        return delay(duration.toMillis());
    }

    /**
     * 设置是否持久化消息（链式）。
     *
     * @param persistent true 持久化（默认），false 非持久化
     * @return 当前实例
     */
    public DelayMessageProcessor persistent(boolean persistent) {
        this.persistent = persistent;
        return this;
    }

    /**
     * 追加自定义消息头（链式），便于消费端区分业务类型或做幂等。
     *
     * @param key   头名称
     * @param value 头值，不能为 null
     * @return 当前实例
     */
    public DelayMessageProcessor withHeader(String key, Object value) {
        Objects.requireNonNull(key, "header key 不能为 null");
        Objects.requireNonNull(value, "header value 不能为 null");
        // 延迟到 postProcessMessage 中统一写入，避免此处丢弃已设置的属性
        this.headers.put(key, value);
        return this;
    }

    /**
     * 校验并设置延时时间（单段模式）。
     *
     * @param delayMillis 延时时间，单位毫秒
     */
    private void setDelayMillis(long delayMillis) {
        if (delayMillis < 0) {
            throw new IllegalArgumentException("延时时间不能为负数：" + delayMillis);
        }
        this.delayMillis = delayMillis;
    }

    /**
     * 校验并设置阶梯（复制为可变列表，避免外部修改影响内部状态）。
     *
     * @param ladder 阶梯延时序列（单位毫秒）
     */
    private void setLadder(List<Long> ladder) {
        Objects.requireNonNull(ladder, "阶梯延时列表不能为 null");
        if (ladder.isEmpty()) {
            throw new IllegalArgumentException("阶梯延时列表不能为空");
        }
        for (Long each : ladder) {
            if (each == null || each < 0) {
                throw new IllegalArgumentException("阶梯延时的每一段都必须为非负的毫秒值：" + each);
            }
        }
        this.delay = new ArrayList<>(ladder);
        // 用队首作为初始 TTL，保证即使调用方忘了先 next() 也不会写入 0
        this.delayMillis = this.delay.get(0);
    }
}
