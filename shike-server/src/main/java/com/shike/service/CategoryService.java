package com.shike.service;

import com.shike.dto.CategoryDTO;
import com.shike.dto.CategoryPageQueryDTO;
import com.shike.entity.Category;
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


    /**
     * 根据类型获取分类
     * @param type
     * @return
     */
    List<Category> list(Integer type);


    /**
     * 刷新【大家都在买】热门分类。
     *
     * <p>重新统计近 N 天热销菜品 TOP-K，写入 Redis 快照，
     * 并按有/无数据同步分类的启用状态。
     * 定时任务与「手动触发刷新」接口共用该逻辑。
     *
     * @return 本次刷新的热门商品数量，无数据返回 0
     */
    int refreshHotCategory();
}
