package com.shike.service;

import com.shike.vo.OrderReportVO;
import com.shike.vo.SalesTop10ReportVO;
import com.shike.vo.TurnoverReportVO;
import com.shike.vo.UserReportVO;

import java.time.LocalDate;

public interface ReportService {

    /**
     * 营业额统计
     * @param begin
     * @param end
     * @return
     */
    TurnoverReportVO getTurnOverStatistics(LocalDate begin, LocalDate end);


    /**
     * 指定时间内 用户数据统计 新增的  总共用户
     * @param begin
     * @param end
     * @return
     */
    UserReportVO getUserStatistics(LocalDate begin, LocalDate end);


    /**
     * 订单数据统计
     *
     * @param begin 开始日期（含）
     * @param end   结束日期（含）
     * @return 每日订单总数、有效订单数及汇总完成率
     */
    OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end);

    /**
     * 销量排名 Top10
     *
     * @param begin 开始日期（含）
     * @param end   结束日期（含）
     * @return 商品名称与销量，逗号分隔
     */
    SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end);
}
