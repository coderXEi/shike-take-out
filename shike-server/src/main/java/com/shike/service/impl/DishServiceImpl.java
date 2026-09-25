package com.shike.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.shike.constant.MessageConstant;
import com.shike.constant.StatusConstant;
import com.shike.dto.DishDTO;
import com.shike.dto.DishPageQueryDTO;
import com.shike.entity.Dish;
import com.shike.entity.DishFlavor;
import com.shike.exception.DeletionNotAllowedException;
import com.shike.mapper.DishFlavorMapper;
import com.shike.mapper.DishMapper;
import com.shike.mapper.SetMealDishMapper;
import com.shike.result.PageResult;
import com.shike.service.DishService;
import com.shike.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Autowired
    SetMealDishMapper setMealDishMapper;

    @Override
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {
        // 事务 菜品表插入一条数据、口味表插入n条数据

        Dish dish = new Dish();

        BeanUtils.copyProperties(dishDTO, dish);

        dishMapper.insert(dish);
        // insert生成的dish 主键值
        Long dishId = dish.getId();

        List<DishFlavor> flavors = dishDTO.getFlavors();

        if (flavors != null && !flavors.isEmpty()) {
            // 若是批量插入的 则 迭代分别赋值DishId
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishId);
            });
            // 插入口味表
            dishFlavorMapper.insertBatch(flavors);

        }

    }

    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {


        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);

        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 删除菜品
     * 规则：
     * 1. 可以一次删除一个，也可以批量删除
     * 2. 起售中的菜品不能删除（dish.status = 1）
     * 3. 被任意套餐关联的菜品不能删除（setmeal_dish）
     * 4. 删除菜品后，关联的口味数据一并删除（dish_flavor）
     * 5. 批量删除为原子操作，任一菜品校验不通过则整体回滚
     *
     * @param ids 要删除的菜品id集合
     */
    @Override
    @Transactional
    public void delete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        // 1. 起售中的菜品不能删除
        Long onSaleCount = dishMapper.countByIdsAndStatus(ids, StatusConstant.ENABLE);
        if (onSaleCount != null && onSaleCount > 0) {
            throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
        }

        // 2. 被任意套餐关联的菜品不能删除
        List<Long> relatedSetmealIds = setMealDishMapper.getSetMealIdsByDishIds(ids);
        if (relatedSetmealIds != null && !relatedSetmealIds.isEmpty()) {
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

        // 3. 先删子表口味数据，再删主表菜品数据
        dishFlavorMapper.deleteByDishIds(ids);
        dishMapper.deleteByIds(ids);
    }


    /**
     * 根据id查询菜品和对应口味数据
     * @param id
     * @return
     */
    @Override
    public DishVO getByIdWithFlavor(Long id) {

        // 根据id查询菜品数据
        Dish dish = dishMapper.getById(id);
        // 根据菜品id 查询口味数据
        List<DishFlavor> dishFlavors=  dishFlavorMapper.getByDishId(dish.getId());
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish, dishVO);

        dishVO.setFlavors(dishFlavors);
        return dishVO;
    }

    /**
     * 修改菜品及其口味数据
     * 规则：
     * 1. 修改菜品基本信息（dish）
     * 2. 先删除该菜品原有的全部口味数据（dish_flavor）
     * 3. 再重新批量插入新的口味数据
     * 4. 以上操作需保证原子性，任一环节失败整体回滚
     *
     * @param dishDTO 菜品及口味数据
     */
    @Override
    @Transactional
    public void updateWithFlavor(DishDTO dishDTO) {
        Long dishId = dishDTO.getId();

        // 1. 修改菜品基本信息
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        dishMapper.update(dish);

        // 2. 删除原有口味数据
        dishFlavorMapper.deleteByDishIds(Collections.singletonList(dishId));

        // 3. 重新插入新的口味数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && !flavors.isEmpty()) {
            flavors.forEach(flavor -> flavor.setDishId(dishId));
            dishFlavorMapper.insertBatch(flavors);
        }
    }
}
