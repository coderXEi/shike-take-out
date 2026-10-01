package com.shike.controller.admin;


import com.shike.result.Result;
import com.shike.service.ReportService;
import com.shike.vo.TurnoverReportVO;
import com.shike.vo.UserReportVO;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@Slf4j
@RequestMapping("/admin/report")
@Api(tags = "数据统计 相关接口")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @GetMapping("/turnoverStatistics")
    public Result<TurnoverReportVO> turnoverStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end ) {
        log.info("营业额统计:开始时间{},结束时间{}",begin,end);

        return Result.success(reportService.getTurnOverStatistics(begin,end));
    }

    @GetMapping("/userStatistics")
    public Result<UserReportVO> userStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end ){
        log.info("用户数据统计:begin{},end:{}",begin,end);

        return Result.success(reportService.getUserStatistics(begin,end));
    }
}
