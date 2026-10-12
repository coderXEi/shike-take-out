package com.shike.constant;

/**
 * 【大家都在买】热门分类相关常量
 *
 * <p>设计说明：
 * <ul>
 *     <li>该分类是一条普通的 {@code category} 记录（type = 1 菜品分类），
 *         名称固定为 {@link #HOT_CATEGORY_NAME}，由定时任务负责创建/维护。</li>
 *     <li>热门商品<b>不会</b>回写 {@code dish.category_id}——
 *         分类与菜品是一对多的归属关系，改写会破坏商品原有归类，
 *         也会影响套餐关联、管理端分页查询等。因此热点排行以「快照」形式存放在 Redis，
 *         由 /user/dish/list 在命中该分类时读取。</li>
 *     <li>分类是否对用户可见，只由 {@code category.status} 控制：
 *         快照为空则置 0（禁用，用户端 category/list 不返回），否则置 1。</li>
 * </ul>
 */
public class HotCategoryConstant {

    /**
     * 热门分类名称（category.idx_name 唯一，靠名称定位这条记录）
     */
    public static final String HOT_CATEGORY_NAME = "大家都在买";

    /**
     * 热门分类的展示排序，值越小越靠前（排在普通分类之前）
     */
    public static final Integer HOT_CATEGORY_SORT = 0;

    /**
     * 热门商品排行快照的 Redis key，全体用户共用
     */
    public static final String HOT_DISH_CACHE_KEY = "dish_hot_everyone_buy";

    /**
     * 统计热度的时间窗口：最近 N 天
     */
    public static final Integer HOT_DISH_DAYS = 14;

    /**
     * 排行取前 K 个商品
     */
    public static final Integer HOT_DISH_TOP_K = 15;

    /**
     * 快照缓存过期时间（小时）。定时任务每日刷新一次，
     * 设置略大于 24 小时，即使某天任务异常也能靠过期兜底，不会永久返回脏数据。
     */
    public static final long HOT_DISH_CACHE_TTL_HOURS = 25L;

    /**
     * 热门分类的创建人/修改人（系统操作，无具体员工）
     */
    public static final Long SYSTEM_USER_ID = 0L;
}
