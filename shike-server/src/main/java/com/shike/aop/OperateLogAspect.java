package com.shike.aop;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.shike.dto.OperateLogDTO;
import com.shike.mapper.OperateLogMapper;
import com.shike.utils.CurrentHolder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;

@Slf4j
@Component
@Aspect
public class OperateLogAspect {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private OperateLogMapper operateLogMapper;

    @Around("@annotation(com.shike.anno.Log)")
    public Object recordLog(ProceedingJoinPoint joinPoint) throws Throwable {

        // 记录方法开始时间
        Long startTime = System.currentTimeMillis();
        // 执行请求处理
        Object result = joinPoint.proceed();

        Long endTime = System.currentTimeMillis();

        OperateLogDTO operateLog = new OperateLogDTO();

        Long costTime = endTime - startTime;

        operateLog.setOperateEmpId(getCurrentUserId());
        operateLog.setOperateTime(LocalDateTime.now());
        operateLog.setCostTime(costTime);

        operateLog.setClassName(joinPoint.getTarget().getClass().getName());
        operateLog.setMethodName(joinPoint.getSignature().getName());
        operateLog.setMethodParams(Arrays.toString(joinPoint.getArgs()));
        try {
            operateLog.setReturnValue(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            log.warn("返回值序列化失败: {}", e.getMessage());
            operateLog.setReturnValue(result.toString());
        }

        log.info("操作日志记录 - 类:{} 方法:{} 耗时:{}ms", operateLog.getClassName(), operateLog.getMethodName(), costTime);

        operateLogMapper.insert(operateLog);

        return result;

    }

    public int getCurrentUserId() {
        return CurrentHolder.get();
    }


}
