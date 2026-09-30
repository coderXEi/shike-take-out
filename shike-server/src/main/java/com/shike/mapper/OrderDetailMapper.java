package com.shike.mapper;


import com.shike.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderDetailMapper {

    /**
     * 批量添加订单明细
     * @param orderDetailList
     */
    void insertBatch(List<OrderDetail> orderDetailList);


    /**
     * 订单详细信息查询 根据订单id
     * @param orderId
     * @return
     */
    @Select("select * from shike_take_out.order_detail where order_id = #{orderId}")
    List<OrderDetail> getByOrderId(Long orderId);
}
