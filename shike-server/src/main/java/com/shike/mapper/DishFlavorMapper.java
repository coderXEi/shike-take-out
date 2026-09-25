package com.shike.mapper;

import com.shike.entity.DishFlavor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DishFlavorMapper {


    /**
     * 批量插入口味数据
     */
    void insertBatch(@Param("flavors") List<DishFlavor> flavors);

    /**
     * 根据菜品id批量删除口味数据
     * @param ids 菜品id集合
     * @return 删除的行数
     */
    int deleteByDishIds(@Param("ids") List<Long> ids);

    /**
     * 根据菜品id 查询对应的口味
     * @param id
     * @return
     */
    @Select("select * from dish_flavor where dish_id = #{dishId}")
    List<DishFlavor> getByDishId(Long id);
}
