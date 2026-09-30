package com.shike.task;


import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@Slf4j
public class MyTask {


    /**
     * 定时任务
     */
//    @Scheduled(cron = "0/5 * * * * ?")
    public void execute_task() {
        // 任务逻辑

        log.info("定时任务执行:{}",new Date());
    }
}
