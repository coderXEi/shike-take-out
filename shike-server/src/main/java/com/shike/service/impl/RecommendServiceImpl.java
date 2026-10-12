package com.shike.service.impl;

import com.shike.constant.HotCategoryConstant;
import com.shike.constant.RecommendCategoryConstant;
import com.shike.constant.StatusConstant;
import com.shike.entity.Category;
import com.shike.mapper.CategoryMapper;
import com.shike.mapper.DishMapper;
import com.shike.service.RecommendService;
import com.shike.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 【为你推荐】个性化分类实现
 *
 * <p>沿用【大家都在买】的「虚拟分类 + 快照」模式：分类只是一条普通 category 记录，
 * 商品来源放在 Redis，绝不改写 {@code dish.category_id}，
 * 因此同一菜品可同时出现在原分类与【为你推荐】中。
 *
 * <p>不同点：推荐按用户隔离，key 为 {@code recommend_dish_{userId}}，
 * 采用懒加载（未命中即算即写）+ TTL，无需定时任务逐用户预计算。
 */
@Service
@Slf4j
public class RecommendServiceImpl implements RecommendService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public Long ensureRecommendCategory() {
        Category category = categoryMapper.selectByName(RecommendCategoryConstant.RECOMMEND_CATEGORY_NAME);
        if (category != null) {
            return category.getId();
        }

        log.info("[为你推荐] 分类不存在，自动创建: {}", RecommendCategoryConstant.RECOMMEND_CATEGORY_NAME);
        LocalDateTime now = LocalDateTime.now();
        Category newCategory = Category.builder()
                // 该分类下展示的是菜品（快照），因此归入菜品分类
                .type(1)
                .name(RecommendCategoryConstant.RECOMMEND_CATEGORY_NAME)
                .sort(RecommendCategoryConstant.RECOMMEND_CATEGORY_SORT)
                // 初始启用：推荐是按用户懒加载的，分类本身作为固定栏目常驻展示
                .status(StatusConstant.ENABLE)
                .createTime(now)
                .updateTime(now)
                .createUser(RecommendCategoryConstant.SYSTEM_USER_ID)
                .updateUser(RecommendCategoryConstant.SYSTEM_USER_ID)
                .build();
        categoryMapper.insert(newCategory);

        return newCategory.getId() != null
                ? newCategory.getId()
                : categoryMapper.selectIdByName(RecommendCategoryConstant.RECOMMEND_CATEGORY_NAME);
    }

    @Override
    public List<DishVO> getRecommendDish(Long userId) {
        // 1. 未登录：无法个性化，交由调用方决定隐藏该栏目
        if (userId == null) {
            log.debug("[为你推荐] 未登录用户，不返回个性化推荐");
            return null;
        }

        String key = RecommendCategoryConstant.recommendDishKey(userId);

        // 2. 命中缓存直接返回
        List<DishVO> cached = readCache(key);
        if (cached != null) {
            return cached;
        }

        // 3. 未命中：按历史订单实时计算
        List<DishVO> recommendList = dishMapper.getRecommendDishByUser(
                userId,
                RecommendCategoryConstant.RECOMMEND_DISH_DAYS,
                RecommendCategoryConstant.RECOMMEND_CATEGORY_TOP_N,
                RecommendCategoryConstant.RECOMMEND_DISH_TOP_K);

        // 4. 无历史订单 -> 回退到【大家都在买】热门快照，避免栏目为空
        if (recommendList == null || recommendList.isEmpty()) {
            log.info("[为你推荐] 用户{} 近{}天无有效历史订单，回退热门快照",
                    userId, RecommendCategoryConstant.RECOMMEND_DISH_DAYS);
            return fallbackToHot();
        }

        // 5. 写缓存（TTL，让偏好自然滚动刷新）
        try {
            redisTemplate.opsForValue().set(
                    key,
                    recommendList,
                    RecommendCategoryConstant.RECOMMEND_DISH_CACHE_TTL_MINUTES,
                    TimeUnit.MINUTES);
        } catch (Exception e) {
            // 缓存写入失败不影响本次返回
            log.warn("[为你推荐] 用户{} 推荐快照写入失败: {}", userId, e.getMessage());
        }

        log.info("[为你推荐] 用户{} 推荐计算完成，商品数={}", userId, recommendList.size());
        return recommendList;
    }

    /**
     * 读取用户推荐快照。
     *
     * <p>兜底：key 不存在、序列化异常等情况返回 null，交由调用方回源计算。
     */
    @SuppressWarnings("unchecked")
    private List<DishVO> readCache(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value instanceof List) {
                return (List<DishVO>) value;
            }
        } catch (Exception e) {
            log.warn("[为你推荐] 推荐快照读取失败，将回源计算: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 回退：读取【大家都在买】全局热门快照。
     *
     * <p>仍为空时返回 null（调用方据此隐藏或返回空列表）。
     */
    @SuppressWarnings("unchecked")
    private List<DishVO> fallbackToHot() {
        try {
            Object value = redisTemplate.opsForValue().get(HotCategoryConstant.HOT_DISH_CACHE_KEY);
            if (value instanceof List) {
                List<DishVO> hot = (List<DishVO>) value;
                // 只回退前 K 个，与推荐结果规模保持一致
                return hot.size() > RecommendCategoryConstant.RECOMMEND_DISH_TOP_K
                        ? new ArrayList<>(hot.subList(0, RecommendCategoryConstant.RECOMMEND_DISH_TOP_K))
                        : hot;
            }
        } catch (Exception e) {
            log.warn("[为你推荐] 回退读取热门快照失败: {}", e.getMessage());
        }
        return null;
    }
}
