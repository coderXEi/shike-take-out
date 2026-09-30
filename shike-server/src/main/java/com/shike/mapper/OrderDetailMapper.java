package com.shike.mapper;


import com.shike.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface OrderDetailMapper {

    /**
     * 批量添加订单明细
     * @param orderDetailList
     */
    void insertBatch(List<OrderDetail> orderDetailList);
}
