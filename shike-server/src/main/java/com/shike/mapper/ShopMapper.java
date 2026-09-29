package com.shike.mapper;

import com.shike.entity.Shop;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ShopMapper {

    /**
     * 查询店铺信息（项目默认单店铺，取第一条）
     */
    @Select("select * from shop order by id limit 1")
    Shop getShop();

    /**
     * 更新营业状态
     */
    @Update("update shop set status = #{status}, update_time = #{updateTime} where id = #{id}")
    void updateStatus(Shop shop);
}
