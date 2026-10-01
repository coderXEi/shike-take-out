package com.shike.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.shike.constant.MessageConstant;
import com.shike.dto.OrdersPageQueryDTO;
import com.shike.dto.OrdersPaymentDTO;
import com.shike.dto.OrdersSubmitDTO;
import com.shike.entity.*;
import com.shike.exception.AddressBookBusinessException;
import com.shike.exception.OrderBusinessException;
import com.shike.exception.ShoppingCartBusinessException;
import com.shike.mapper.*;
import com.shike.result.PageResult;
import com.shike.service.OrderService;
import com.shike.utils.CurrentHolder;
import com.shike.utils.WeChatPayUtil;
import com.shike.vo.OrderPaymentVO;
import com.shike.vo.OrderStatisticsVO;
import com.shike.vo.OrderSubmitVO;
import com.shike.vo.OrderVO;
import com.shike.websocket.WebSocketServer;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


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
    @Autowired
    private WebSocketServer webSocketServer;


    @Override
    @Transactional
    public OrderSubmitVO submit(OrdersSubmitDTO dto) {



        // 处理业务异常  收货地，购物车数据为空
        AddressBook addressBook = addressBookMapper.getById(dto.getAddressBookId());
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        ShoppingCart shoppingCart = new ShoppingCart();

        Long userId = CurrentHolder.get();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);

        if (shoppingCartList == null || shoppingCartList.isEmpty()) {
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

        for (ShoppingCart cart : shoppingCartList) {
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

        // ==================== 原【真实调用微信支付SDK】代码，暂注释掉，勿删除 ====================
        // //调用微信支付接口，生成预支付交易单
        // JSONObject jsonObject = weChatPayUtil.pay(
        //         ordersPaymentDTO.getOrderNumber(), //商户订单号
        //         new BigDecimal(0.01), //支付金额，单位 元
        //         "苍穹外卖订单", //商品描述
        //         user.getOpenid() //微信用户的openid
        // );
        //
        // if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
        //     throw new OrderBusinessException("该订单已支付");
        // }
        //
        // OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        // vo.setPackageStr(jsonObject.getString("package"));
        //
        // return vo;
        // ==================== 原代码结束 ====================

        // ==================== 模拟支付：跳过微信SDK，直接执行支付成功逻辑（等价于 notify 回调） ====================
        // 直接调用支付成功业务处理，效果等同于微信支付成功回调 notify/paySuccess
        paySuccess(ordersPaymentDTO.getOrderNumber());

        // 构造伪支付参数返回给前端，保持前端支付流程不报错
        OrderPaymentVO vo = new OrderPaymentVO();
        vo.setNonceStr("mock_nonce_str");
        vo.setTimeStamp(String.valueOf(System.currentTimeMillis() / 1000));
        vo.setSignType("RSA");
        vo.setPackageStr("prepay_id=mock_prepay_id");
        vo.setPaySign("mock_pay_sign");

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

        Map<String, Object> map = new HashMap<>();
        map.put("type",1);
        map.put("orderId",111);
        map.put("content","订单来了");
        String jsonString = JSON.toJSONString(map);

        webSocketServer.sendToAllClient(jsonString);
    }

    @Override
    public PageResult pageQuery(Integer pageNum, Integer pageSize, Integer status) {
        PageHelper.startPage(pageNum, pageSize);

        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setUserId(CurrentHolder.get());
        ordersPageQueryDTO.setStatus(status);
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);
        List<OrderVO> list = new ArrayList<>();
        if (page != null && page.getTotal() > 0) {

            for (Orders order : page) {
                Long orderId = order.getId();
                // 再去查订单详细信息
                List<OrderDetail> orderDetail = orderDetailMapper.getByOrderId(orderId);

                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(order, orderVO);
                // 判空，加入结果集
                if (orderDetail != null) {
                    orderVO.setOrderDetailList(orderDetail);
                }
                list.add(orderVO);
            }
            // 查询结果不为空
        }
        // Orders实体类  + orderDetail 实体类

        // 最终返回的是PageResult total 和records  list
        return new PageResult(page.getTotal(), list);
    }

    @Override
    public OrderVO details(Long id) {

        // 通过当前用户id 获取到订单信息
        Orders orders = orderMapper.getById(id);
        // 获取菜品 订单明细
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);
        return orderVO;
    }

    @Override
    public void cancel(Long id) throws Exception {
        // 取消某个订单  待支付和待接单的可直接取消
        // 根据id 先获取订单状态
        Orders ordersDB =  orderMapper.getById(id);
        if(ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
        if(ordersDB.getStatus() > 2) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(id);

        // 执行退款
        if(ordersDB.getStatus() == Orders.TO_BE_CONFIRMED) {
            // ==================== 原【真实调用微信退款SDK】代码，暂注释掉，勿删除 ====================
            // // 微信接口退款
            // weChatPayUtil.refund(
            //         ordersDB.getNumber(),
            //         ordersDB.getNumber(),
            //         new BigDecimal(0.01),
            //         new BigDecimal(0.01)
            // );
            // ==================== 原代码结束 ====================

            // 模拟退款：跳过微信SDK，直接标记为已退款
            orders.setPayStatus(Orders.REFUND);
        }

        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用户取消");
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);

        // 商家已接单的 需要先电话

        // 派送的需要电话沟通商家

        // 若待接单状态下取消，需退款给用户

        // 取消订单需要将订单状态修改为已取消
//        orderMapper.cancelById(id);
    }

    @Override
    public void repetition(Long id) {
        // 相当于针对当前订单信息 再加入购物车
        // 获取用户id
        Long userId = CurrentHolder.get();

        // 根据订单id 获取详细
        List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(id);
        // 将订单详细对象转成 购物车对象
        List<ShoppingCart> shoppingCartList = orderDetails.stream().map(x -> {
            ShoppingCart shoppingCart = new ShoppingCart();
            BeanUtils.copyProperties(x, shoppingCart, "id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());

            return shoppingCart;
        }).collect(Collectors.toList());

        // 加入购物车
        shoppingCartMapper.insertBatch(shoppingCartList);



    }

    /**
     * 订单搜索。
     *
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult search(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        // 部分订单状态  需要额外返回订单菜品信息 将orders 转成orderVO
        List<OrderVO> list = getOrderVOList(page);

        return new PageResult(page.getTotal(),list);
    }

    @Override
    public OrderStatisticsVO statistic() {
         return  orderMapper.statistic();


    }

    @Override
    public void reminder(Long id) {
        Orders order = orderMapper.getById(id);
        // 订单是否存在
        if(order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        Map<String, Object> map = new HashMap<>();
        map.put("type",2);
        map.put("orderId",id);
        map.put("content","客户催单，订单号:" +order.getId());

        // ws 推送催单
        webSocketServer.sendToAllClient(JSON.toJSONString(map));
    }

    private List<OrderVO> getOrderVOList(Page<Orders> page) {
        // 需返回订单菜品信息  自定义ordervo响应
        List<OrderVO> orderVOList = new ArrayList<>();

        List<Orders> ordersList = page.getResult();
        if(!CollectionUtils.isEmpty(ordersList)) {
            for(Orders order : ordersList) {
                // 共同字段赋值到OrderVO
                OrderVO orderVO  = new OrderVO();
                BeanUtils.copyProperties(order, orderVO);
                String orderDishes = getOrderDishesStr(order);

                // 将订单菜品 信息封装到orderVO中  并添加到orderVOList
                orderVO.setOrderDishes(orderDishes);
                orderVOList.add(orderVO);
            }
        }
        return orderVOList;
    }

    private String getOrderDishesStr(Orders orders) {
        // 查询订单菜品详情信息（订单中的菜品和数量）
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

        // 将每一条订单菜品信息拼接为字符串（格式：宫保鸡丁*3；）
        List<String> orderDishList = orderDetailList.stream().map(x -> {
            String orderDish = x.getName() + "*" + x.getNumber() + ";";
            return orderDish;
        }).collect(Collectors.toList());

        // 将该订单对应的所有菜品信息拼接在一起
        return String.join("", orderDishList);
    }
}
