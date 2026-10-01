package com.shike.controller.user;

import com.github.pagehelper.Page;
import com.shike.dto.OrdersPaymentDTO;
import com.shike.dto.OrdersSubmitDTO;
import com.shike.entity.OrderDetail;
import com.shike.mapper.OrderDetailMapper;
import com.shike.result.PageResult;
import com.shike.result.Result;
import com.shike.service.OrderService;
import com.shike.vo.OrderPaymentVO;
import com.shike.vo.OrderSubmitVO;
import com.shike.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("userOrderController")
@Slf4j
@Api(tags = "用户端订单类接口")
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
    @ApiOperation("C端-历史订单查询")
    public Result<PageResult> getHistoryOrders(@Param("page") Integer page,@Param("pageSize") Integer pageSize,@Param("status") Integer status) {
        log.info("历史订单查询:{},{},{}", pageSize, page, status);
        PageResult pageResult =  orderService.pageQuery(page,pageSize,status);
        return Result.success(pageResult);
    }


    /**
     * 用户下单api
     */
    @PostMapping("/submit")
    @ApiOperation("用户下单接口")
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
    @ApiOperation("订单支付")
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
    @ApiOperation("根据订单id查订单详情")
    public Result<OrderVO> getOrderDetail(@PathVariable("id") Long id){
        log.info("根据id 查询订单id:{}",id);
         OrderVO orderVO =  orderService.details(id);
        return Result.success(orderVO);
    }

    /**
     * 取消订单api
     */
    @PutMapping("/cancel/{id}")
    @ApiOperation("取消订单接口")
    public Result cancelOrder(@PathVariable("id") Long id) throws Exception {
        log.info("取消订单，id:{}",id);

        orderService.cancel(id);
        return Result.success();
    }

    /**
     * 再来一单
     */
    @PostMapping("/repetition/{id}")
    @ApiOperation("再来一单")
    public Result repetition(@PathVariable("id") Long id) {
        log.info("再来一单");
        // - 再来一单就是将原订单中的商品重新加入到购物车中
        orderService.repetition(id);
        return Result.success();
    }

    @GetMapping("/reminder/{id}")
    @ApiOperation("客户催单")
    public Result reminder(Long id) {
        log.info("催单了:{}",id);
        orderService.reminder(id);
        return Result.success();
    }


}
