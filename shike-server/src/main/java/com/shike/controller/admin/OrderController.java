package com.shike.controller.admin;


import com.shike.dto.OrdersPageQueryDTO;
import com.shike.result.PageResult;
import com.shike.result.Result;
import com.shike.service.OrderService;
import com.shike.vo.OrderStatisticsVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("adminOrderController")
@Slf4j
@Tag(name = "管理端订单管理接口")
@RequestMapping("/admin/order")
public class OrderController {

    @Autowired
    OrderService orderService;

    @GetMapping("/conditionSearch")
    @Operation(summary = "多条件分页订单搜索")
    public Result<PageResult> search(OrdersPageQueryDTO ordersPageQueryDTO){
        log.info("搜索订单的条件:{}",ordersPageQueryDTO);

        PageResult pageResult =  orderService.search(ordersPageQueryDTO);

        return Result.success(pageResult);
    }

    @GetMapping("/statistics")
    @Operation(summary = "各个状态订单数量统计")
    public Result<OrderStatisticsVO> statistics() {
        log.info("分类订单数量统计");
        OrderStatisticsVO orderStatisticsVO =  orderService.statistic();
        return Result.success(orderStatisticsVO);
    }
}
