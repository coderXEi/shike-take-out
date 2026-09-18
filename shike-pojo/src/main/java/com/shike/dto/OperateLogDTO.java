package com.shike.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OperateLogDTO {
    private Integer id; //ID
    private Long operateEmpId; //操作人ID
    private LocalDateTime operateTime; //操作时间
    private String className; //操作类名
    private String methodName; //操作方法名
    private String methodParams; //操作方法参数
    private String returnValue; //操作方法返回值
    private Long costTime; //操作耗时
}