package com.shike.mapper;


import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SetMealDishMapper {

    /**
     * 根据菜品id集合  查询对应的套餐id（去重）
     * @param ids 菜品id集合
     * @return 关联到的套餐id集合，无关联时为空集合
     */
    List<Long> getSetMealIdsByDishIds(@Param("ids") List<Long> ids);
}
