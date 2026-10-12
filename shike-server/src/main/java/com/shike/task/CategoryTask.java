package com.shike.task;


import com.shike.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 【大家都在买】热门分类定时任务
 *
 * <p>每日凌晨执行一次，刷新近 14 天热销 TOP-15 商品到【大家都在买】分类。
 * 具体业务逻辑在 {@link CategoryService#refreshHotCategory()}，
 * 以便管理端「手动触发刷新」接口复用（首次上线 / 任务漏跑时可立即补数据）。
 *
 * <p>兜底：统计无数据时清空快照并把分类置为禁用（0），用户端不展示该分类。
 */
@Component
@Slf4j
public class CategoryTask {

    private final CategoryService categoryService;

    public CategoryTask(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * 更新【大家都在买】分类
     * cron: 秒 分 时 日 月 周 —— 每日 00:00:00 执行
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void refreshHotCategory() {
        try {
            categoryService.refreshHotCategory();
        } catch (Exception e) {
            // 定时任务异常不应影响调度线程，记录日志后等待下一次触发
            log.error("[大家都在买] 定时刷新热门分类失败", e);
        }
    }
}
