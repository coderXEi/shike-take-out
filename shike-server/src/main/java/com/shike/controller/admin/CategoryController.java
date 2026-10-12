package com.shike.controller.admin;


import com.shike.dto.CategoryDTO;
import com.shike.dto.CategoryPageQueryDTO;
import com.shike.result.PageResult;
import com.shike.result.Result;
import com.shike.service.CategoryService;
import com.shike.vo.CategoryVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/category")
@Slf4j
@Tag(name = "分类相关接口")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @PutMapping
    @Operation(summary = "更新分类信息")
    public Result updateCategory(@RequestBody CategoryDTO category) {
        log.info("更新分类参数:{}", category);

        categoryService.update(category);
        return Result.success();
    }


    @GetMapping("/page")
    @Operation(summary = "分类分页查询方法")
    public Result<PageResult> page(CategoryPageQueryDTO categoryPageQueryDTO) {
        // 返回total 和records list total应该是这个records的长度
        log.info("分类分页查询:{}", categoryPageQueryDTO);

        PageResult categoryVO = categoryService.page(categoryPageQueryDTO);
        //
        return Result.success(categoryVO);
    }

    @PostMapping("/{status}")
    @Operation(summary = "启用或禁用分类")
    public Result startOrStop(Long id, @PathVariable Integer status) {
        log.info("启用或禁用分类id:{}，状态变更为:{}", id, status);
        categoryService.startOrStop(id, status);
        return Result.success();
    }

    @PostMapping
    @Operation(summary = "新增分类")
    public Result addCategory(CategoryDTO categoryDTO) {
        log.info("新增分类:{}", categoryDTO);
        categoryService.addCategory(categoryDTO);
        return Result.success();
    }

    @DeleteMapping
    @Operation(summary = "根据id删除分类")
    public Result deleteCategory(Integer id) {
        log.info("要删除分类的id:{}", id);
        categoryService.deleteCategory(id);
        return Result.success();
    }

    @GetMapping("/list")
    @Operation(summary = "查询分类")
    public Result<List<CategoryVO>> getCategory(Integer type) {
        log.info("查询分类，可能存在的类型参数:{}", type);
        List<CategoryVO> categoryVOList = categoryService.getCategory(type);
        return Result.success(categoryVOList);
    }

    @PostMapping("/hot/refresh")
    @Operation(summary = "手动刷新【大家都在买】热门分类")
    public Result<Integer> refreshHotCategory() {
        log.info("手动触发【大家都在买】热门分类刷新");
        int count = categoryService.refreshHotCategory();
        return Result.success(count);
    }

}
