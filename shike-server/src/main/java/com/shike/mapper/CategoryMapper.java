package com.shike.mapper;

import com.github.pagehelper.Page;
import com.shike.anno.AutoFill;
import com.shike.dto.CategoryDTO;
import com.shike.dto.CategoryPageQueryDTO;
import com.shike.entity.Category;
import com.shike.enumeration.OperationType;
import com.shike.vo.CategoryVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

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
}
