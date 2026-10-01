package com.shike.service.impl;

import com.shike.entity.Orders;
import com.shike.mapper.OrderMapper;
import com.shike.mapper.UserMapper;
import com.shike.service.OrderService;
import com.shike.service.ReportService;
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
}
