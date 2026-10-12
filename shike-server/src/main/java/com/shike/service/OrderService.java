package com.shike.service;

import com.shike.dto.OrdersPageQueryDTO;
import com.shike.dto.OrdersPaymentDTO;
import com.shike.dto.OrdersSubmitDTO;
import com.shike.entity.Orders;
import com.shike.result.PageResult;
import com.shike.vo.OrderPaymentVO;
import com.shike.vo.OrderStatisticsVO;
import com.shike.vo.OrderSubmitVO;
import com.shike.vo.OrderVO;

public interface OrderService {

    /**
     * 用户下单接口
     * @param dto
     * @return
     */
    OrderSubmitVO submit(OrdersSubmitDTO dto);

    /**
     * 订单支付
     * @param ordersPaymentDTO
     * @return
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * 支付成功，修改订单状态
     * @param outTradeNo
     */
    void paySuccess(String outTradeNo);


    /**
     * 历史订单 分页查询
     * @param page
     * @param pageSize
     * @param status
     * @return
     */
    PageResult pageQuery(Integer page, Integer pageSize, Integer status);


    /**
     * 根据订单id 获取订单详细信息
     * @param id
     * @return
     */
    OrderVO details(Long id);


    /**
     * 根据id取消订单
     * @param id
     */
    void cancel(Long id) throws Exception;


    /**
     * 再来一单
     * @param id
     */
    void repetition(Long id);

    /**
     * 分页多条件订单查询
     * @param ordersPageQueryDTO
     * @return
     */
    PageResult search(OrdersPageQueryDTO ordersPageQueryDTO);


    /**
     * 各个状态订单数量统计
     * @return
     */
    OrderStatisticsVO statistic();

    /**
     * 客户催单
     * @param id
     */
    void reminder(Long id);


    /**
     * 根据订单号查询订单（不做用户归属校验，供定时任务 / MQ 消费者等内部流程使用）
     * @param orderNumber 订单号（订单唯一业务标识）
     * @return 订单实体，不存在时返回 null
     */
    Orders getByNumber(String orderNumber);


    /**
     * 关闭超时未支付的订单（订单超时延时消息的最终处理）。
     *
     * <p>仅当订单仍处于「待付款」时才关闭，否则视为已支付/已处理，直接返回（幂等）。
     * 关闭动作：状态置为已取消、取消原因置为「订单超时，自动取消」、记录取消时间。
     *
     * @param orderNumber 订单号（订单唯一业务标识，来自超时延时消息体）
     * @return true 表示本次调用确实关闭了订单；false 表示订单不存在或已无需关闭
     */
    boolean closeTimeOutOrder(String orderNumber);
}
