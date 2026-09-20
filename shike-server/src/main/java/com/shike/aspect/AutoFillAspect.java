package com.shike.aspect;

import com.shike.anno.AutoFill;
import com.shike.enumeration.OperationType;
import com.shike.utils.CurrentHolder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static com.shike.constant.AutoFillConstant.*;

/**
 * 自定义切面，实现公共字段自动填充处理
 */
@Aspect
@Component
@Slf4j
public class AutoFillAspect {

    /**
     * 切入点  mapper下的
     */
    @Pointcut("execution(* com.shike.mapper.*.*(..)) && @annotation(com.shike.anno.AutoFill)")
    public void autoFillPointCut() {
    }

    @Before("autoFillPointCut()")
    public void autoFill(JoinPoint joinPoint) {
        log.info("开始进行公共字段自动填充...");

        // 获取到当前被拦截方法的数据库操作类型
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        AutoFill autoFill =  signature.getMethod().getAnnotation(AutoFill.class);  // 获取方法上的注解对象
        OperationType operationType =  autoFill.value();  //获取数据库操作类型


        // 获取被拦截方法的参数 实体对象
        Object[] args = joinPoint.getArgs();
        if(args == null || args.length == 0) {
            return;
        }
        Object entity = args[0];
        // 准备赋值的数据  time and userId
        LocalDateTime now = LocalDateTime.now();
        Long currentId = CurrentHolder.get();
        // 根据不同的操作 为不同字段赋值，需要拿到字段对应Class实例 通过反射 修改对应的值
        if(operationType == OperationType.INSERT) {
            //为4个公共字段赋值
            try {
               Method setCreateTime =  entity.getClass().getDeclaredMethod(SET_CREATE_TIME,LocalDateTime.class);
               Method setCreateUser =  entity.getClass().getDeclaredMethod(SET_CREATE_USER,Long.class);
               Method setUpdateTime =  entity.getClass().getDeclaredMethod(SET_UPDATE_TIME,LocalDateTime.class);
               Method setUpdateUser =  entity.getClass().getDeclaredMethod(SET_UPDATE_USER,Long.class);


               setCreateTime.invoke(entity,now);
               setCreateUser.invoke(entity,currentId);
               setUpdateTime.invoke(entity,now);
               setUpdateUser.invoke(entity,currentId);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        if(operationType == OperationType.UPDATE) {
            // 为2个公共字段赋值

            try {
                Method setUpdateTime =  entity.getClass().getDeclaredMethod(SET_UPDATE_TIME,LocalDateTime.class);
                Method setUpdateUser =  entity.getClass().getDeclaredMethod(SET_UPDATE_USER,Long.class);
                setUpdateTime.invoke(entity,now);
                setUpdateUser.invoke(entity,currentId);

            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }

        }


        // acording that operation type
    }
}
