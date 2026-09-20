package com.shike.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.shike.dto.CategoryDTO;
import com.shike.dto.CategoryPageQueryDTO;
import com.shike.entity.Category;
import com.shike.mapper.CategoryMapper;
import com.shike.result.PageResult;
import com.shike.service.CategoryService;
import com.shike.vo.CategoryVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@Slf4j
public class CategoryServiceImpl implements CategoryService {
    @Autowired
    CategoryMapper categoryMapper;

    @Override
    public void update(CategoryDTO category) {
        categoryMapper.update(category);
    }

    @Override
    public PageResult page(CategoryPageQueryDTO categoryPageQueryDTO) {
        PageHelper.startPage(categoryPageQueryDTO.getPage(),categoryPageQueryDTO.getPageSize());
        Page<CategoryVO> page = categoryMapper.page(categoryPageQueryDTO);
        long total = page.getTotal();
        return new PageResult(total,page.getResult());
    }

    @Override
    public void startOrStop(Long id, Integer status) {
        Category category = Category.builder()
                .id(id)
                .status(status)
                .build();
        categoryMapper.startOrStop(category);
    }

    @Override
    public void addCategory(CategoryDTO categoryDTO) {

        categoryMapper.add(categoryDTO);
    }

    @Override
    public void deleteCategory(Integer id) {
        categoryMapper.delete(id);
    }

    @Override
    public List<CategoryVO> getCategory(Integer type) {

        return categoryMapper.get(type);
    }
}
