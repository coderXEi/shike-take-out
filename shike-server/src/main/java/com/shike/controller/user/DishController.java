package com.shike.controller.user;

import com.shike.constant.DishCacheConstant;
import com.shike.constant.HotCategoryConstant;
import com.shike.constant.StatusConstant;
import com.shike.entity.Dish;
import com.shike.mapper.CategoryMapper;
import com.shike.result.Result;
import com.shike.service.DishService;
import com.shike.service.RecommendService;
import com.shike.utils.CurrentHolder;
import com.shike.vo.DishVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.ArrayList;
import java.util.List;

@RestController("userDishController")
@RequestMapping("/user/dish")
@Slf4j
@Tag(name = "C端-菜品浏览接口")
public class DishController {
    @Autowired
    private DishService dishService;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private RecommendService recommendService;

    @Autowired
    RedisTemplate redisTemplate;

    /**
     * 根据分类id查询菜品
     *
     * <p>【大家都在买】是动态分类：其商品来自定时任务写入 Redis 的热销排行快照，
     * 而不是 {@code dish.category_id}。因此这里先判断是否为热门分类，
     * 是则直接返回快照（分类被禁用 / 快照为空时返回空列表，客户端不展示）。
     *
     * <p>【为你推荐】同样是动态分类，但按当前登录用户维护。命中该分类时从
     * {@code recommend_dish_{userId}} 读取（未命中则懒计算），用户未登录/无偏好时
     * 由 Service 兜底，最终仍为空则返回空列表。
     *
     * @param categoryId 分类id
     * @return 该分类下在售的菜品列表
     */
    @GetMapping("/list")
    @Operation(summary = "根据分类id查询菜品")
    public Result<List<DishVO>> list(Long categoryId) {
        // 热门分类：走快照，不回源 dish 表（dish 表里没有归属该分类的菜品）
        if (isHotCategory(categoryId)) {
            return Result.success(getHotDishFromCache());
        }

        // 个性化分类：按当前用户维度读取/计算推荐快照
        if (isRecommendCategory(categoryId)) {
            List<DishVO> recommendList = recommendService.getRecommendDish(CurrentHolder.get());
            return Result.success(recommendList != null ? recommendList : new ArrayList<>());
        }

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

    /**
     * 判断请求的分类是否为【大家都在买】（按名称精确匹配，与定时任务一致）
     */
    private boolean isHotCategory(Long categoryId) {
        if (categoryId == null) {
            return false;
        }
        Long hotCategoryId = categoryMapper.selectIdByName(HotCategoryConstant.HOT_CATEGORY_NAME);
        return categoryId.equals(hotCategoryId);
    }

    /**
     * 判断请求的分类是否为【为你推荐】（按名称精确匹配，与 RecommendService 一致）。
     *
     * <p>顺带确保该分类记录存在（首次访问时自动创建），
     * 避免部署后未跑任何任务时用户端看不到这个栏目。
     */
    private boolean isRecommendCategory(Long categoryId) {
        if (categoryId == null) {
            return false;
        }
        Long recommendCategoryId = recommendService.ensureRecommendCategory();
        return categoryId.equals(recommendCategoryId);
    }

    /**
     * 读取热销排行快照。
     *
     * <p>兜底：分类被禁用、key 过期、缓存反序列化异常等情况统一返回空列表，
     * 保证接口不报错、用户端不显示空分类。
     */
    @SuppressWarnings("unchecked")
    private List<DishVO> getHotDishFromCache() {
        try {
            Object value = redisTemplate.opsForValue().get(HotCategoryConstant.HOT_DISH_CACHE_KEY);
            if (value instanceof List) {
                return (List<DishVO>) value;
            }
        } catch (Exception e) {
            log.warn("[大家都在买] 热门商品快照读取失败，按空列表处理: {}", e.getMessage());
        }
        return new ArrayList<>();
    }

}
