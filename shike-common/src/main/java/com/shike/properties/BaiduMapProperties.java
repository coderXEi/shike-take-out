package com.shike.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 百度地图开放平台配置。
 * ak/sk 用于签名，shopAddress 为商家（配送起点）地址，deliveryScope 为最大配送距离（单位：米）。
 */
@Component
@ConfigurationProperties(prefix = "shike.baidumap")
@Data
public class BaiduMapProperties {

    private String ak; // 百度地图开放平台 AK

    private String sk; // 百度地图开放平台 SK

    private String shopAddress; // 商家地址（配送起点）

    private Double deliveryScope; // 最大可配送距离，单位：米

}
