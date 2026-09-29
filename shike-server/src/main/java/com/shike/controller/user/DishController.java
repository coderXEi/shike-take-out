package com.shike.controller.user;

import com.shike.constant.DishCacheConstant;
import com.shike.constant.StatusConstant;
import com.shike.entity.Dish;
import com.shike.result.Result;
import com.shike.service.DishService;
import com.shike.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController("userDishController")
@RequestMapping("/user/dish")
@Slf4j
@Api(tags = "C端-菜品浏览接口")
public class DishController {
    @Autowired
    private DishService dishService;

    @Autowired
    RedisTemplate redisTemplate;

    /**
     * 根据分类id查询菜品
     *
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<DishVO>> list(Long categoryId) {
        // 构造key
        String key = DishCacheConstant.categoryListKey(categoryId);
        // 查redis缓存是否存在
        List<DishVO> list = (List<DishVO>) redisTemplate.opsForValue().get(key);
        // 存在则直接返回
        if(list != null) {
            return Result.success(list);
        }


        // 不返回 再查数据库
        Dish dish = new Dish();
        dish.setCategoryId(categoryId);
        dish.setStatus(StatusConstant.ENABLE);//查询起售中的菜品

         list = dishService.listWithFlavor(dish);
         redisTemplate.opsForValue().set(key, list);

         return Result.success(list);
    }

}
