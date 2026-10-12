package com.shike.service;

import com.shike.vo.DishVO;

import java.util.List;

/**
 * 【为你推荐】个性化分类业务接口
 *
 * <p>与【大家都在买】（{@link CategoryService#refreshHotCategory()}）共用
 * 「虚拟分类 + Redis 快照」模式，区别在于推荐结果是按用户维度维护的。
 */
public interface RecommendService {

    /**
     * 获取【为你推荐】分类的 id；分类不存在时自动创建后返回。
     *
     * @return 分类id，创建/查询失败返回 null
     */
    Long ensureRecommendCategory();

    /**
     * 获取当前登录用户的推荐菜品列表。
     *
     * <p>读取顺序：
     * <ol>
     *     <li>未登录（userId 为空）-> 返回 null，由调用方决定是否隐藏该栏目；</li>
     *     <li>命中 Redis 快照 -> 直接返回；</li>
     *     <li>未命中 -> 按历史订单实时计算并写缓存；</li>
     *     <li>无历史订单 -> 回退到【大家都在买】热门快照（仍为空则返回 null）。</li>
     * </ol>
     *
     * @param userId 当前登录用户id，可为 null
     * @return 推荐菜品列表；未登录且无兜底数据时返回 null
     */
    List<DishVO> getRecommendDish(Long userId);
}
