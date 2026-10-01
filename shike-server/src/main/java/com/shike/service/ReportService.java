package com.shike.service;

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
}
