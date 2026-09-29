package com.shike.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 店铺
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Shop implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //店铺名称
    private String name;

    //店铺logo
    private String logo;

    //联系电话
    private String phone;

    //店铺地址
    private String address;

    //经度
    private BigDecimal longitude;

    //纬度
    private BigDecimal latitude;

    //店铺简介
    private String description;

    //公告
    private String notice;

    //开始营业时间
    private LocalTime openTime;

    //结束营业时间
    private LocalTime closeTime;

    //营业状态 0:打烊 1:营业
    private Integer status;

    //起送金额
    private BigDecimal minAmount;

    //配送费
    private BigDecimal deliveryFee;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createUser;

    private Long updateUser;
}
