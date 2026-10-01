package com.shike.service;

import com.shike.dto.OrdersPageQueryDTO;
import com.shike.dto.OrdersPaymentDTO;
import com.shike.dto.OrdersSubmitDTO;
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
}
