package com.shike.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.shike.constant.HotCategoryConstant;
import com.shike.constant.StatusConstant;
import com.shike.dto.CategoryDTO;
import com.shike.dto.CategoryPageQueryDTO;
import com.shike.entity.Category;
import com.shike.mapper.CategoryMapper;
import com.shike.mapper.DishMapper;
import com.shike.result.PageResult;
import com.shike.service.CategoryService;
import com.shike.vo.CategoryVO;
import com.shike.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;


@Service
@Slf4j
public class CategoryServiceImpl implements CategoryService {
    @Autowired
    CategoryMapper categoryMapper;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

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

    @Override
    public List<Category> list(Integer type) {
        List<Category> list = categoryMapper.list(type);
        return list != null ? list : java.util.Collections.emptyList();
    }

    /**
     * 刷新【大家都在买】热门分类
     *
     * <p>设计说明：热门商品<b>不回写</b> {@code dish.category_id}，
     * 而是以 Redis 快照承载（{@link HotCategoryConstant#HOT_DISH_CACHE_KEY}），
     * 由 /user/dish/list 在请求该分类时读取，避免破坏商品原有归属。
     */
    @Override
    public int refreshHotCategory() {
        long start = System.currentTimeMillis();
        log.info("[大家都在买] 开始刷新热门分类, days={}, topK={}",
                HotCategoryConstant.HOT_DISH_DAYS, HotCategoryConstant.HOT_DISH_TOP_K);

        // 1. 统计热销菜品排行（已完成订单 + 在售菜品）
        List<DishVO> hotList = dishMapper.getHotDish(
                HotCategoryConstant.HOT_DISH_DAYS, HotCategoryConstant.HOT_DISH_TOP_K);

        // 2. 确保分类记录存在（首次部署时库中可能没有这条数据）
        Category hotCategory = ensureHotCategory();
        if (hotCategory == null) {
            log.error("[大家都在买] 热门分类不存在且创建失败，本次刷新终止");
            return 0;
        }

        // 3. 兜底：无数据 -> 清快照 + 禁用分类（用户端 category/list 与 dish/list 均不展示）
        if (hotList == null || hotList.isEmpty()) {
            redisTemplate.delete(HotCategoryConstant.HOT_DISH_CACHE_KEY);
            setCategoryStatus(hotCategory, StatusConstant.DISABLE);
            log.info("[大家都在买] 近{}天无热销数据，已隐藏该分类, categoryId={}",
                    HotCategoryConstant.HOT_DISH_DAYS, hotCategory.getId());
            return 0;
        }

        // 4. 有数据 -> 写快照 + 启用分类
        //    TTL 略大于 24h：任务异常时靠过期自动兜底，不会长期返回过期排行
        redisTemplate.opsForValue().set(
                HotCategoryConstant.HOT_DISH_CACHE_KEY,
                hotList,
                HotCategoryConstant.HOT_DISH_CACHE_TTL_HOURS,
                TimeUnit.HOURS);
        setCategoryStatus(hotCategory, StatusConstant.ENABLE);

        log.info("[大家都在买] 热门分类刷新完成, categoryId={}, 商品数={}, 耗时={}ms",
                hotCategory.getId(), hotList.size(), System.currentTimeMillis() - start);
        return hotList.size();
    }

    /**
     * 保证【大家都在买】分类存在；不存在则创建。
     *
     * @return 分类记录（含 id），创建/查询失败返回 null
     */
    private Category ensureHotCategory() {
        Category category = categoryMapper.selectByName(HotCategoryConstant.HOT_CATEGORY_NAME);
        if (category != null) {
            return category;
        }

        log.info("[大家都在买] 分类不存在，自动创建: {}", HotCategoryConstant.HOT_CATEGORY_NAME);
        LocalDateTime now = LocalDateTime.now();
        Category newCategory = Category.builder()
                // 该分类下展示的是菜品（快照），因此归入菜品分类
                .type(1)
                .name(HotCategoryConstant.HOT_CATEGORY_NAME)
                .sort(HotCategoryConstant.HOT_CATEGORY_SORT)
                // 初始禁用，待下面按实际数据量决定是否启用，避免空分类短暂可见
                .status(StatusConstant.DISABLE)
                .createTime(now)
                .updateTime(now)
                .createUser(HotCategoryConstant.SYSTEM_USER_ID)
                .updateUser(HotCategoryConstant.SYSTEM_USER_ID)
                .build();
        categoryMapper.insert(newCategory);

        return newCategory.getId() == null
                ? categoryMapper.selectByName(HotCategoryConstant.HOT_CATEGORY_NAME)
                : newCategory;
    }

    /**
     * 更新分类状态，仅在状态发生变化时执行 update，减少无意义的写库
     */
    private void setCategoryStatus(Category category, Integer status) {
        if (status.equals(category.getStatus())) {
            log.debug("[大家都在买] 分类状态无需变更, categoryId={}, status={}", category.getId(), status);
            return;
        }
        categoryMapper.updateStatusById(category.getId(), status);
        category.setStatus(status);
    }
}
