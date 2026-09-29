package com.shike.service;

import com.shike.dto.SetmealDTO;
import com.shike.dto.SetmealPageQueryDTO;
import com.shike.entity.Setmeal;
import com.shike.result.PageResult;
import com.shike.vo.DishItemVO;
import com.shike.vo.SetmealVO;
import java.util.List;

public interface SetmealService {

    /**
     * 条件查询
     * @param setmeal
     * @return
     */
    List<Setmeal> list(Setmeal setmeal);

    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    List<DishItemVO> getDishItemById(Long id);


    /**
     * 新增套餐，保存套餐和菜品的关联
     * @param setmealDTO
     */
    void saveWithDish(SetmealDTO setmealDTO);


    /**
     * 分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);


    /**
     * 批量删除套餐
     * @param ids
     */
    void deleteBatch(List<Long> ids);


    /**
     * 根据id查询套餐和关联的菜品数据
     * @param id
     * @return
     */
    SetmealVO getByIdWithDish(Long id);

    /**
     * 修改套餐
     * @param setmealDTO
     */
    void update(SetmealDTO setmealDTO);


    /**
     * 套餐起售、停售
     * @param status
     * @param id
     */
    void startOrStop(Integer status, Long id);
}
