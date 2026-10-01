package com.shike.service.impl;

import com.shike.dto.GoodsSalesDTO;
import com.shike.entity.Orders;
import com.shike.mapper.OrderMapper;
import com.shike.mapper.UserMapper;
import com.shike.service.OrderService;
import com.shike.service.ReportService;
import com.shike.vo.OrderReportVO;
import com.shike.vo.SalesTop10ReportVO;
import com.shike.vo.TurnoverReportVO;
import com.shike.vo.UserReportVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service

public class ReportServiceImpl implements ReportService {

    /** 销量排行榜取前 N 名 */
    private static final int TOP_N = 10;

    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;


    @Override
    public TurnoverReportVO getTurnOverStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> list = new ArrayList<>();
        // 从begin 到end 的每一天（含首尾）都添加到list中，然后拼成字符串给前端
        LocalDate current = begin;
        while (!current.isAfter(end)) {
            list.add(current);
            current = current.plusDays(1);
        }
        // 从LocalDate 转到LocalDateTime，逐天统计营业额
        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : list) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map<String, Object> map = new HashMap<>();
            map.put("beginTime", beginTime);
            map.put("endTime", endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.sumByMap(map);
            turnover = turnover == null ? 0.0 : turnover;
            turnoverList.add(turnover);
        }

        return TurnoverReportVO.builder()
                .dateList(StringUtils.join(list, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();
    }

    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {

        List<LocalDate> list = new ArrayList<>();
        list.add(begin);

        while(!begin.isAfter(end)) {
            begin = begin.plusDays(1);
            list.add(begin);
        }

        // 总的用户数量，新增用户数
        List<Integer> newUserList = new ArrayList<>();
        List<Integer> totalUserList = new ArrayList<>();
        // 查数据 - list  - 转string

        for (LocalDate date : list) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map<String, Object> map = new HashMap<>();

            map.put("endTime", endTime);
            Integer totalUser =  userMapper.countByMap(map);
            map.put("beginTime", beginTime);
            Integer newUser = userMapper.countByMap(map);
            totalUserList.add(totalUser);
            newUserList.add(newUser);
        }
       return UserReportVO.builder().dateList(StringUtils.join(list, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .build();
    }

    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = buildDateList(begin, end);

        // 每日订单总数、每日有效订单数
        List<Integer> orderCountList = new ArrayList<>(dateList.size());
        List<Integer> validOrderCountList = new ArrayList<>(dateList.size());

        // 区间汇总：订单总数、有效订单数
        int totalOrderCount = 0;
        int validOrderCount = 0;

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            int dayTotal = getOrderCount(beginTime, endTime, null);
            int dayValid = getOrderCount(beginTime, endTime, Orders.COMPLETED);

            orderCountList.add(dayTotal);
            validOrderCountList.add(dayValid);

            totalOrderCount += dayTotal;
            validOrderCount += dayValid;
        }

        // 订单完成率 = 有效订单数 / 订单总数，避免除零
        double orderCompletionRate = totalOrderCount == 0
                ? 0.0
                : validOrderCount * 1.0 / totalOrderCount;

        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    @Override
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        // 用「次日零点」作为开区间上界，避免 LocalTime.MAX 纳秒精度带来的边界丢失
        LocalDateTime endTime = LocalDateTime.of(end.plusDays(1), LocalTime.MIN);

        List<GoodsSalesDTO> salesList =
                orderMapper.getSalesTop10(beginTime, endTime, Orders.COMPLETED, TOP_N);

        List<String> nameList = new ArrayList<>(salesList.size());
        List<Integer> numberList = new ArrayList<>(salesList.size());
        for (GoodsSalesDTO goodsSales : salesList) {
            nameList.add(goodsSales.getName());
            numberList.add(goodsSales.getNumber());
        }

        return SalesTop10ReportVO.builder()
                .nameList(StringUtils.join(nameList, ","))
                .numberList(StringUtils.join(numberList, ","))
                .build();
    }

    /**
     * 构建 [begin, end] 闭区间内的每一天（含首尾）。
     * 若 end 早于 begin，则返回空列表，避免死循环。
     */
    private List<LocalDate> buildDateList(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        for (LocalDate date = begin; !date.isAfter(end); date = date.plusDays(1)) {
            dateList.add(date);
        }
        return dateList;
    }

    private Integer getOrderCount(LocalDateTime beginTime, LocalDateTime endTime, Integer status) {
        Map<String, Object> map = new HashMap<>();
        map.put("beginTime", beginTime);
        map.put("endTime", endTime);
        map.put("status", status);

        Integer count = orderMapper.countByMap(map);
        return count == null ? 0 : count;
    }
}
