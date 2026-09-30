package com.shike.service;

import com.shike.dto.OrdersPaymentDTO;
import com.shike.dto.OrdersSubmitDTO;
import com.shike.vo.OrderPaymentVO;
import com.shike.vo.OrderSubmitVO;

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
}
