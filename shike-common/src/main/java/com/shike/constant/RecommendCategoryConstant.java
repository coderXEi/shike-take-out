package com.shike.constant;

/**
 * 【为你推荐】个性化分类相关常量
 *
 * <p>设计说明（与【大家都在买】同一套「虚拟分类 + 快照」思路）：
 * <ul>
 *     <li>该分类是一条普通的 {@code category} 记录（type = 1 菜品分类），
 *         名称固定为 {@link #RECOMMEND_CATEGORY_NAME}，仅作为「栏目壳子」，
 *         让用户端分类列表能展示它。</li>
 *     <li>推荐商品<b>不会</b>回写 {@code dish.category_id}——
 *         商品始终保留原有的唯一归属，同一菜品可同时出现在原分类和【为你推荐】中。</li>
 *     <li>与【大家都在买】全体用户共用一份快照不同，【为你推荐】是<b>按用户</b>维护的：
 *         Redis key 为 {@code recommend_dish_{userId}}，读写都在当前登录用户维度。</li>
 *     <li>采用<b>懒加载</b>：用户请求该分类时若缓存未命中，实时按历史订单计算一次并写缓存，
 *         设置 TTL 让偏好自然滚动作废，无需定时任务逐个用户预计算。</li>
 * </ul>
 */
public class RecommendCategoryConstant {

    /**
     * 个性化分类名称（category.idx_name 唯一，靠名称定位这条记录）
     */
    public static final String RECOMMEND_CATEGORY_NAME = "为你推荐";

    /**
     * 分类的展示排序，值越小越靠前（排在【大家都在买】之后、普通分类之前）
     */
    public static final Integer RECOMMEND_CATEGORY_SORT = 1;

    /**
     * 单个用户推荐快照的 Redis key 前缀，实际 key 为 {@code recommend_dish_{userId}}
     */
    public static final String RECOMMEND_DISH_CACHE_PREFIX = "recommend_dish_";

    /**
     * 统计用户偏好的时间窗口：最近 N 天历史订单
     */
    public static final Integer RECOMMEND_DISH_DAYS = 30;

    /**
     * 每个用户推荐取前 K 个商品
     */
    public static final Integer RECOMMEND_DISH_TOP_K = 10;

    /**
     * 取用户购买量最高的前 N 个分类作为偏好分类
     */
    public static final Integer RECOMMEND_CATEGORY_TOP_N = 5;

    /**
     * 单个用户推荐快照的缓存过期时间（分钟）。
     * 用户行为变化后最长等待该时长即可自然刷新，避免长期返回陈旧推荐。
     */
    public static final long RECOMMEND_DISH_CACHE_TTL_MINUTES = 120L;

    /**
     * 分类的创建人/修改人（系统操作，无具体员工）
     */
    public static final Long SYSTEM_USER_ID = 0L;

    /**
     * 计算某个用户的推荐快照 Redis key
     *
     * @param userId 用户id
     * @return 形如 {@code recommend_dish_1}
     */
    public static String recommendDishKey(Long userId) {
        return RECOMMEND_DISH_CACHE_PREFIX + userId;
    }
}
