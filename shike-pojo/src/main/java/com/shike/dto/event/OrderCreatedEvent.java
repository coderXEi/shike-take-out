package com.shike.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 订单创建完成事件。
 *
 * <p>由 {@code OrderServiceImpl#submit} 在下单事务内发布，
 * 由 {@code OrderTimeoutMqSender} 通过 {@code @TransactionalEventListener}
 * 在<b>事务提交后</b>消费，用于发送订单超时延时消息。
 *
 * <p>这样做的原因：延时时长需要从「订单已真正落库、对其他连接可见」的时刻开始计时。
 * 若在事务提交前发送，消费者可能在数据可见前就收到消息，
 * 导致「查不到订单」而白白浪费一次阶梯延时。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订单 id（订单表自增主键）。
     * <p>仅作内部标识使用，<b>不作为超时延时消息体</b>。
     */
    private Long orderId;

    /**
     * 订单号（业务编号，订单唯一标识）。
     * <p>超时延时消息体携带此值，而不是自增 id。
     */
    private String orderNumber;
}
