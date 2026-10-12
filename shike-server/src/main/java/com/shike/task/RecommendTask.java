package com.shike.task;

import com.shike.service.RecommendService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 【为你推荐】分类维护任务
 *
 * <p>与【大家都在买】不同，【为你推荐】的商品是按用户懒加载计算的，
 * 无需（也无法）由定时任务为所有用户预计算。这里只负责保证「分类壳子」始终存在：
 * <ul>
 *     <li>应用启动时确保一次，避免部署后用户端分类列表看不到该栏目；</li>
 *     <li>每日凌晨再确保一次，兜底分类被误删/误禁用的场景。</li>
 * </ul>
 */
@Component
@Slf4j
public class RecommendTask implements ApplicationRunner {

    private final RecommendService recommendService;

    public RecommendTask(RecommendService recommendService) {
        this.recommendService = recommendService;
    }

    /**
     * 启动完成后确保【为你推荐】分类存在
     */
    @Override
    public void run(ApplicationArguments args) {
        ensureCategory();
    }

    /**
     * 每日 00:05:00 兜底确保分类存在
     * cron: 秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 5 0 * * ?")
    public void ensureRecommendCategory() {
        ensureCategory();
    }

    private void ensureCategory() {
        try {
            Long id = recommendService.ensureRecommendCategory();
            log.info("[为你推荐] 分类就绪, categoryId={}", id);
        } catch (Exception e) {
            // 维护任务异常不应影响启动/调度线程
            log.error("[为你推荐] 确保分类存在失败", e);
        }
    }
}
