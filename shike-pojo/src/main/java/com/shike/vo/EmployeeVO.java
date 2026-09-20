package com.shike.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import java.io.Serializable;
import java.time.LocalDateTime;
@Data
public class EmployeeVO implements Serializable {

    private Long id;
    private String username;
    private String name;
    private String phone;
    private String sex;
    private String idNumber;
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    private Long createUser;
    private Long updateUser;
}