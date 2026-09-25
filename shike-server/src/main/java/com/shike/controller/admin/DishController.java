package com.shike.controller.admin;


import com.shike.dto.DishDTO;
import com.shike.dto.DishPageQueryDTO;
import com.shike.result.PageResult;
import com.shike.result.Result;
import com.shike.service.DishService;
import com.shike.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/dish")
@Api(tags = "菜品相关接口")
@Slf4j
public class DishController {

    @Autowired
    DishService dishService;

    @PostMapping
    @ApiOperation("新增菜品")
    public Result save(@RequestBody DishDTO dishDTO) {
        log.info("新增菜品:{}",dishDTO);

        return Result.success();
    }

    @GetMapping("/page")
    @ApiOperation("菜品分页查询")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO){

        log.info("菜品分页查询:{}",dishPageQueryDTO);
        PageResult pr = dishService.pageQuery(dishPageQueryDTO);
        return Result.success(pr);
    }


    // @RequestParam 将接受到的1,2,3 字符串解析成能被list接收
    @DeleteMapping
    @ApiOperation("删除菜品接口")
    public Result delete( @RequestParam List<Long> ids){

        log.info("要删除的菜品id :{}",ids);
        dishService.delete(ids);
        return Result.success();
    }

    @GetMapping("/{id}")
    @ApiOperation("根据id查询菜品")
    public Result<DishVO> findById(@PathVariable Long id){
        log.info("根据id查询菜品");
        DishVO dishVO =  dishService.getByIdWithFlavor(id);
        return Result.success(dishVO);
    }


    @PutMapping
    @ApiOperation("修改菜品信息")
    public Result update(@RequestBody DishDTO dishDTO){
        log.info(" 接收到菜品信息:{}",dishDTO);

        dishService.updateWithFlavor(dishDTO);
        return Result.success();
    }

}
