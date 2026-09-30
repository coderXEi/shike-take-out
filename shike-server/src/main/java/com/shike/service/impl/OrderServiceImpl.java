package com.shike.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.shike.constant.MessageConstant;
import com.shike.dto.OrdersPaymentDTO;
import com.shike.dto.OrdersSubmitDTO;
import com.shike.entity.*;
import com.shike.exception.AddressBookBusinessException;
import com.shike.exception.OrderBusinessException;
import com.shike.exception.ShoppingCartBusinessException;
import com.shike.mapper.*;
import com.shike.service.OrderService;
import com.shike.utils.CurrentHolder;
import com.shike.utils.WeChatPayUtil;
import com.shike.vo.OrderPaymentVO;
import com.shike.vo.OrderSubmitVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private AddressBookMapper addressBookMapper;

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WeChatPayUtil weChatPayUtil;


    @Override
    @Transactional
    public OrderSubmitVO submit(OrdersSubmitDTO dto) {

        // 处理业务异常  收货地，购物车数据为空
        AddressBook addressBook = addressBookMapper.getById(dto.getAddressBookId());
        if(addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        ShoppingCart shoppingCart = new ShoppingCart();

        Long userId = CurrentHolder.get();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);

        if(shoppingCartList == null || shoppingCartList.isEmpty()) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }
        // 向订单表插入1条数据

        Orders orders = new Orders();
        BeanUtils.copyProperties(dto, orders);
        // 下单时间
        orders.setOrderTime(LocalDateTime.now());
        // 未付款
        orders.setPayStatus(Orders.UN_PAID);
        // 待付款
        orders.setStatus(Orders.PENDING_PAYMENT);
        // 订单号
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        // 收货人
        orders.setConsignee(addressBook.getConsignee());
        // 手机号
        orders.setPhone(addressBook.getPhone());
        // 用户id
        orders.setUserId(addressBook.getUserId());

        orderMapper.insert(orders);

        List<OrderDetail> orderDetailList = new ArrayList<>();

        for(ShoppingCart cart:shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId()); // 设置当前订单明细关联的订单id
            orderDetailList.add(orderDetail);
        }
        // 向订单明细表插入n条数据
        orderDetailMapper.insertBatch(orderDetailList);

        // 下单成功后 清空用户购物车数据
        shoppingCartMapper.deleteById(userId);

        // 封装vo对象
        return OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(orders.getOrderTime())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .build();
    }


    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = CurrentHolder.get();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }
}
