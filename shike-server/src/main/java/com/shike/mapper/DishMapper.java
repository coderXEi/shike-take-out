package com.shike.mapper;


import com.github.pagehelper.Page;
import com.shike.anno.AutoFill;
import com.shike.dto.DishPageQueryDTO;
import com.shike.entity.Dish;
import com.shike.enumeration.OperationType;
import com.shike.vo.DishVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DishMapper {


    /**
     * 插入菜品数据
     * @param dish
     */
    @AutoFill(value= OperationType.INSERT)
    void insert(Dish dish);

    /**
     * 分页查询菜品
     * @param dishPageQueryDTO
     * @return
     */
    Page<DishVO> pageQuery(DishPageQueryDTO dishPageQueryDTO);


    /**
     * 获取某个菜品
     * @param id
     */
    @Select("select * from dish where id =#{id}")
    Dish getById(Long id);

    /**
     * 统计指定菜品id中处于指定状态的数量
     * @param ids 菜品id集合
     * @param status 状态
     * @return 满足条件的数量
     */
    Long countByIdsAndStatus(@Param("ids") List<Long> ids, @Param("status") Integer status);

    /**
     * 根据id批量删除菜品
     * @param ids 菜品id集合
     * @return 删除的行数
     */
    int deleteByIds(@Param("ids") List<Long> ids);


    /**
     * 修改菜品数据
     * @param dish
     */
    @AutoFill(value= OperationType.UPDATE)
    void update(Dish dish);
}
