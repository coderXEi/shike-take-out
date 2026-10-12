package com.shike.controller.user;

import com.github.pagehelper.Page;
import com.shike.dto.OrdersPaymentDTO;
import com.shike.dto.OrdersSubmitDTO;
import com.shike.entity.OrderDetail;
import com.shike.mapper.OrderDetailMapper;
import com.shike.result.PageResult;
import com.shike.result.Result;
import com.shike.service.OrderService;
import com.shike.utils.CurrentHolder;
import com.shike.vo.OrderPaymentVO;
import com.shike.vo.OrderSubmitVO;
import com.shike.vo.OrderVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("userOrderController")
@Slf4j
@Tag(name = "用户端订单类接口")
@RequestMapping("/user/order")
public class OrderController {

    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderDetailMapper orderDetailMapper;

    /**
     * 历史订单查询
     */
    @GetMapping("/historyOrders")
    @Operation(summary = "C端-历史订单查询")
    public Result<PageResult> getHistoryOrders(@Param("page") Integer page,@Param("pageSize") Integer pageSize,@Param("status") Integer status) {
        log.info("历史订单查询:{},{},{}", pageSize, page, status);
        PageResult pageResult =  orderService.pageQuery(page,pageSize,status);
        return Result.success(pageResult);
    }


    /**
     * 用户下单api
     */
    @PostMapping("/submit")
    @Operation(summary = "用户下单接口")
    public Result<OrderSubmitVO> submit(@RequestBody OrdersSubmitDTO dto){
        log.info("用户下单传参:{}",dto);
        OrderSubmitVO orderSubmitVO =  orderService.submit(dto);

        return Result.success(orderSubmitVO);
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    @PutMapping("/payment")
    @Operation(summary = "订单支付")
    public Result<OrderPaymentVO> payment(@RequestBody OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        log.info("订单支付：{}", ordersPaymentDTO);
        OrderPaymentVO orderPaymentVO = orderService.payment(ordersPaymentDTO);
        log.info("生成预支付交易单：{}", orderPaymentVO);
        return Result.success(orderPaymentVO);
    }

    /**
     * 查询订单详情 根据id
     */
    @GetMapping("/orderDetail/{id}")
    @Operation(summary = "根据订单id查订单详情")
    public Result<OrderVO> getOrderDetail(@PathVariable("id") Long id){
        log.info("根据id 查询订单id:{}",id);
         OrderVO orderVO =  orderService.details(id);
        return Result.success(orderVO);
    }

    /**
     * 取消订单api
     */
    @PutMapping("/cancel/{id}")
    @Operation(summary = "取消订单接口")
    public Result cancelOrder(@PathVariable("id") Long id) throws Exception {
        log.info("取消订单，id:{}",id);

        orderService.cancel(id);
        return Result.success();
    }

    /**
     * 再来一单
     */
    @PostMapping("/repetition/{id}")
    @Operation(summary = "再来一单")
    public Result repetition(@PathVariable("id") Long id) {
        log.info("再来一单");
        // - 再来一单就是将原订单中的商品重新加入到购物车中
        orderService.repetition(id);
        return Result.success();
    }

    @GetMapping("/reminder/{id}")
    @Operation(summary = "客户催单")
    public Result reminder(Long id) {
        log.info("催单了:{}",id);
        orderService.reminder(id);
        return Result.success();
    }


    private void getRecommandShopsByHistoryOrders() throws Exception {
        Long userId = CurrentHolder.get();
        if (userId != null) {
            throw new Exception("未登录异常");
        }

        //  获取用户历史订单  no  -> 返回null   客户端隐藏 为你推荐  => 大家都在买

        // yes ->  根据历史订单 的商品信息 拿到商品集合

        // 将历史订单的最新的几个 拼接  然后进行embedding   去查库 获取TOP K




    }

    private void getHotestShops() {

        //  单纯查表 无需rag
    }


    private void FAQQuery() {
        // 用户在线提问， LLM 考虑 决定是否 查表，

        // 将用户文本 进行embedding

        // 查表 返回TOP K
    }
}
