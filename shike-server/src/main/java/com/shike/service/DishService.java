package com.shike.service;


import com.shike.dto.DishDTO;
import com.shike.dto.DishPageQueryDTO;
import com.shike.result.PageResult;
import com.shike.vo.DishVO;

import java.util.List;

public interface DishService {

    public void saveWithFlavor(DishDTO dishDTO);


    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO);


    /**
     * 删除指定菜品
     * @param ids
     */
    void delete(List<Long> ids);


    /**
     * 根据id 返回菜品和口味
     * @param id
     * @return
     */
    DishVO getByIdWithFlavor(Long id);


    /**
     * 根据id修改菜品信息和对应口味信息
     * @param dishDTO
     */
    void updateWithFlavor(DishDTO dishDTO);
}
