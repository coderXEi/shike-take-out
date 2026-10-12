package com.shike.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.shike.constant.MessageConstant;
import com.shike.constant.MqOrderConstant;
import com.shike.dto.OrdersPageQueryDTO;
import com.shike.dto.OrdersPaymentDTO;
import com.shike.dto.OrdersSubmitDTO;
import com.shike.dto.event.OrderCreatedEvent;
import com.shike.entity.*;
import com.shike.exception.AddressBookBusinessException;
import com.shike.exception.OrderBusinessException;
import com.shike.exception.ShoppingCartBusinessException;
import com.shike.mapper.*;
import com.shike.result.DeliveryDistanceResult;
import com.shike.result.PageResult;
import com.shike.service.OrderService;
import com.shike.utils.BaiduMapUtil;
import com.shike.utils.CurrentHolder;
import com.shike.utils.WeChatPayUtil;
import com.shike.vo.OrderPaymentVO;
import com.shike.vo.OrderStatisticsVO;
import com.shike.vo.OrderSubmitVO;
import com.shike.vo.OrderVO;
import com.shike.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Service
@Slf4j
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
    @Autowired
    private BaiduMapUtil baiduMapUtil;

    @Autowired
    private ApplicationEventPublisher eventPublisher;


    @Override
    @Transactional
    public OrderSubmitVO submit(OrdersSubmitDTO dto) {
        Long userId = CurrentHolder.get();

        // 1. 数据校验：收货地址存在、在配送范围内、购物车非空
        AddressBook addressBook = validateAddress(dto.getAddressBookId());
        List<ShoppingCart> shoppingCartList = getUserShoppingCart(userId);

        // 2. 组装并持久化订单（订单表 1 条）
        Orders orders = buildOrder(dto, addressBook);
        saveOrder(orders);

        // 3. 持久化订单明细（订单明细表 n 条）
        saveOrderDetails(orders.getId(), shoppingCartList);

        // 4. 清空用户购物车
        clearShoppingCart(userId);

        // 5. 发布订单创建事件：由 OrderTimeoutMqSender 在【事务提交后】发送第一段延时消息，
        //    保证消费者拿到消息时订单已落库可见，且事务回滚时不会发出孤儿消息。
        //    消息体使用订单号（订单唯一业务标识），而非自增 id。
        eventPublisher.publishEvent(OrderCreatedEvent.builder()
                .orderId(orders.getId())
                .orderNumber(orders.getNumber())
                .build());

        // 6. 封装返回对象
        return OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(orders.getOrderTime())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .build();
    }

    /**
     * 校验收货地址：存在性 + 是否在商家配送范围内。
     *
     * @param addressBookId 地址簿 id
     * @return 收货地址
     * @throws AddressBookBusinessException 地址不存在
     * @throws OrderBusinessException       地址无法解析或超出配送范围
     */
    private AddressBook validateAddress(Long addressBookId) {
        AddressBook addressBook = addressBookMapper.getById(addressBookId);
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        // 校验收货地址是否在商家配送范围内（商家地址、配送范围取自配置文件）
        checkDeliveryRange(addressBook);
        return addressBook;
    }

    /**
     * 校验并获取当前用户的购物车数据。
     *
     * @param userId 用户 id
     * @return 购物车列表（非空）
     * @throws ShoppingCartBusinessException 购物车为空
     */
    private List<ShoppingCart> getUserShoppingCart(Long userId) {
        ShoppingCart query = ShoppingCart.builder().userId(userId).build();
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(query);
        if (shoppingCartList == null || shoppingCartList.isEmpty()) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }
        return shoppingCartList;
    }

    /**
     * 根据提交参数与收货地址组装订单实体（不落库）。
     *
     * @param dto         下单参数
     * @param addressBook 收货地址
     * @return 待持久化的订单
     */
    private Orders buildOrder(OrdersSubmitDTO dto, AddressBook addressBook) {
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
        return orders;
    }

    /**
     * 持久化订单，插入后回填自增主键。
     *
     * @param orders 订单实体
     */
    private void saveOrder(Orders orders) {
        orderMapper.insert(orders);
    }

    /**
     * 将由购物车组成的订单明细批量持久化。
     *
     * @param orderId          订单 id
     * @param shoppingCartList 购物车数据
     */
    private void saveOrderDetails(Long orderId, List<ShoppingCart> shoppingCartList) {
        List<OrderDetail> orderDetailList = new ArrayList<>(shoppingCartList.size());
        for (ShoppingCart cart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            // 关联当前订单 id
            orderDetail.setOrderId(orderId);
            orderDetailList.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetailList);
    }

    /**
     * 下单成功后清空该用户的购物车数据。
     *
     * @param userId 用户 id
     */
    private void clearShoppingCart(Long userId) {
        shoppingCartMapper.cleanById(userId);
    }

    /**
     * 校验收货地址是否在商家配送范围内。
     * 商家地址（shike.baidumap.shop-address）与最大配送距离（shike.baidumap.delivery-scope，单位：米）均来自配置文件。
     * 若地址解析失败或超出配送范围，抛出 {@link OrderBusinessException}，由全局异常处理器返回提示。
     *
     * @param addressBook 收货地址簿
     */
    private void checkDeliveryRange(AddressBook addressBook) {
        // 拼接完整收货地址，提升地理编码成功率
        String userAddress = String.join("",
                nullToEmpty(addressBook.getProvinceName()),
                nullToEmpty(addressBook.getCityName()),
                nullToEmpty(addressBook.getDistrictName()),
                nullToEmpty(addressBook.getDetail()));

        DeliveryDistanceResult distanceResult = baiduMapUtil.getDistanceFromShop(userAddress);
        if (!Boolean.TRUE.equals(distanceResult.getSuccess())) {
            log.error("收货地址解析失败，addressBookId={}, address={}", addressBook.getId(), userAddress);
            throw new OrderBusinessException(MessageConstant.ADDRESS_GEOCODE_FAILED);
        }

        log.info("下单距离校验：addressBookId={}, address={}, 距离={}米, 是否可配送={}",
                addressBook.getId(), userAddress, distanceResult.getDistance(), distanceResult.getCanDeliver());
        if (!Boolean.TRUE.equals(distanceResult.getCanDeliver())) {
            throw new OrderBusinessException(MessageConstant.OUT_OF_DELIVERY_RANGE);
        }
    }

    /** null 安全的字符串转换 */
    private String nullToEmpty(String value) {
        return value == null ? "" : value;
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

    @Override
    public Orders getByNumber(String orderNumber) {
        if (orderNumber == null || orderNumber.isEmpty()) {
            return null;
        }
        // 供 MQ 消费者 / 定时任务使用的内部查询，不做用户归属校验
        return orderMapper.getByNumber(orderNumber);
    }

    @Override
    @Transactional
    public boolean closeTimeOutOrder(String orderNumber) {
        if (orderNumber == null || orderNumber.isEmpty()) {
            log.warn("[订单超时关闭] orderNumber 为空，忽略");
            return false;
        }
        Orders ordersDB = orderMapper.getByNumber(orderNumber);
        if (ordersDB == null) {
            log.warn("[订单超时关闭] 订单不存在，orderNumber={}", orderNumber);
            return false;
        }
        // 幂等：只有仍处于「待付款」的订单才需要关闭；
        // 用户中途支付后会变为待接单，此处直接返回，避免误取消已支付订单
        if (!Orders.PENDING_PAYMENT.equals(ordersDB.getStatus())) {
            log.info("[订单超时关闭] 订单已支付或已处理，无需关闭，orderNumber={}, status={}, payStatus={}",
                    orderNumber, ordersDB.getStatus(), ordersDB.getPayStatus());
            return false;
        }

        Orders orders = new Orders();
        // update 需要自增主键定位记录，主键取自查到的订单实体（业务上仍以 orderNumber 为入口）
        orders.setId(ordersDB.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason(MqOrderConstant.ORDER_TIMEOUT_CANCEL_REASON);
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);

        log.info("[订单超时关闭] 订单已超时自动取消，orderNumber={}, orderId={}",
                orderNumber, ordersDB.getId());
        return true;
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
