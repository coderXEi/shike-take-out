package com.shike.mapper;

import com.github.pagehelper.Page;
import com.shike.dto.GoodsSalesDTO;
import com.shike.dto.OrdersPageQueryDTO;
import com.shike.entity.Orders;
import com.shike.vo.OrderStatisticsVO;
import com.shike.vo.OrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {
    /**
     * 插入订单数据
     * @param order
     */
    void insert(Orders order);

    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);


    /**
     * 历史订单分页查询
     * @param ordersPageQueryDTO
     * @return
     */
    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);


    /**
     * 根据用户id  获取订单详情
     * @param id
     * @return
     */
    @Select("select * from orders where id = #{id}")
    Orders getById(Long id);

    /**
     * 多条件分页订单查询
     * @param ordersPageQueryDTO
     * @return
     */
    Page<OrderVO> search(OrdersPageQueryDTO ordersPageQueryDTO);


    /**
     * 不同类型状态订单量统计
     * @return
     */
    OrderStatisticsVO statistic();

    /**
     * 定时处理订单状态
     * @param status
     * @param time
     * @return
     */
    @Select("select * from orders where status = #{status} and order_time < #{time}")
    List<Orders> getByStatusAndOrderTime(Integer status, LocalDateTime time);
}
