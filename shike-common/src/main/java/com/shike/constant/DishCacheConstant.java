package com.shike.constant;

/**
 * 菜品缓存 key 常量
 */
public class DishCacheConstant {

    /**
     * 菜品缓存统一前缀，清理时使用 dish_* 可匹配所有菜品缓存
     */
    public static final String DISH_CACHE_PREFIX = "dish_";

    /**
     * 菜品详情缓存前缀
     */
    public static final String DISH_INFO_PREFIX = "dish_info_";

    /**
     * 分类下菜品列表缓存 key
     *
     * @param categoryId 分类id
     * @return 缓存 key
     */
    public static String categoryListKey(Long categoryId) {
        return DISH_CACHE_PREFIX + categoryId;
    }

    /**
     * 菜品详情缓存 key
     *
     * @param id 菜品id
     * @return 缓存 key
     */
    public static String dishInfoKey(Long id) {
        return DISH_INFO_PREFIX + id;
    }
}
