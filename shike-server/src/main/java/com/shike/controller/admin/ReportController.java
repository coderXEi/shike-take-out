package com.shike.controller.admin;


import com.shike.result.Result;
import com.shike.service.ReportService;
import com.shike.vo.OrderReportVO;
import com.shike.vo.SalesTop10ReportVO;
import com.shike.vo.TurnoverReportVO;
import com.shike.vo.UserReportVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDate;

@RestController
@Slf4j
@RequestMapping("/admin/report")
@Tag(name = "数据统计 相关接口")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @GetMapping("/turnoverStatistics")
    @Operation(summary = "营业额统计")
    public Result<TurnoverReportVO> turnoverStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end ) {
        log.info("营业额统计:开始时间{},结束时间{}",begin,end);

        return Result.success(reportService.getTurnOverStatistics(begin,end));
    }

    @GetMapping("/userStatistics")
    @Operation(summary = "用户数据统计")
    public Result<UserReportVO> userStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end ){
        log.info("用户数据统计:begin{},end:{}",begin,end);

        return Result.success(reportService.getUserStatistics(begin,end));
    }

    @GetMapping("/ordersStatistics")
    @Operation(summary = "订单数据统计")
    public Result<OrderReportVO> orderStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end
    ) {
        log.info("订单数据统计开始:{},结束:{}",begin,end);

        return Result.success(reportService.getOrderStatistics(begin,end));
    }

    @GetMapping("/top10")
    @Operation(summary = "销售top10")
    public Result<SalesTop10ReportVO> salesTop10ReportVOResult (
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end
    ) {
        log.info("销售额top10 数据统计开始:{},结束:{}",begin,end);

        return Result.success(reportService.getSalesTop10(begin,end));
    }



    @GetMapping("/export")
    @Operation(summary = "导出运营数据报表")
    public void export(HttpServletResponse response, OrderReportVO orderReportVO) {
            // 通过response 获得输出流
        reportService.exportData(response);
    }
}
