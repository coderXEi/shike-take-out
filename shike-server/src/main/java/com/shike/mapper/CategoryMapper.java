package com.shike.mapper;

import com.github.pagehelper.Page;
import com.shike.anno.AutoFill;
import com.shike.dto.CategoryDTO;
import com.shike.dto.CategoryPageQueryDTO;
import com.shike.entity.Category;
import com.shike.enumeration.OperationType;
import com.shike.vo.CategoryVO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CategoryMapper {


    @Update("update category set name = #{name},sort = #{sort},type = #{type} where id= #{id}")
    @AutoFill(OperationType.UPDATE)
    void update(CategoryDTO dto);


    Page<CategoryVO> page(CategoryPageQueryDTO categoryPageQueryDTO);

    @Update("update category set status = #{status} where id = #{id}")
    @AutoFill(OperationType.UPDATE)
    void startOrStop(Category category);


    @Insert("Insert into category (name,sort,type) values(#{name},#{sort},#{type})")
    @AutoFill(OperationType.INSERT)
    void add(CategoryDTO categoryDTO);


    @Delete("delete from category where id =#{id}")
    void delete(Integer id);


    List<CategoryVO> get(Integer type);

    /**
     * 根据类型查询启用的分类列表
     * @param type 1:菜品分类 2:套餐分类，为null时查询全部
     * @return
     */
    List<Category> list(Integer type);


    /**
     * 根据名称查询分类id
     * @param name 分类名称
     * @return 分类id，不存在返回 null
     */
    Long selectIdByName(String name);


    /**
     * 更新分类状态
     * @param categoryId
     * @param status
     */
    void updateStatusById(@Param("id") Long categoryId,@Param("status") Integer status);


    /**
     * 按名称查询分类（含状态），用于定时任务判断热门分类是否已存在
     * @param name 分类名称
     * @return 分类，不存在返回 null
     */
    Category selectByName(String name);


    /**
     * 新增【大家都在买】分类（不存在时由定时任务自动创建）
     * @param category 分类
     */
    void insert(Category category);
}
