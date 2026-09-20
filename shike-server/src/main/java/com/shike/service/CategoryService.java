package com.shike.service;

import com.shike.dto.CategoryDTO;
import com.shike.dto.CategoryPageQueryDTO;
import com.shike.result.PageResult;
import com.shike.vo.CategoryVO;

import java.util.List;

public interface CategoryService {

    void update(CategoryDTO category);


    /**
     * 分类的分页查询逻辑
     * @param categoryPageQueryDTO
     * @return
     */
    PageResult page(CategoryPageQueryDTO categoryPageQueryDTO);


    /**
     * 启用或禁用分类
     * @param id
     * @param status
     */
    void startOrStop(Long id, Integer status);

    /**
     * 新增分类
     * @param categoryDTO
     */
    void addCategory(CategoryDTO categoryDTO);

    /**
     * 删除分类
     * @param id
     */
    void deleteCategory(Integer id);

    /**
     * 获取分类数据
     * @param type
     * @return
     */
    List<CategoryVO> getCategory(Integer type);
}
