package com.shike.result;

import lombok.Data;

import java.io.Serializable;

/**
 * 两地距离计算结果。
 * 由 {@link com.shike.utils.BaiduMapUtil} 返回，用于通用地承载「距离 + 是否可配送」信息。
 */
@Data
public class DeliveryDistanceResult implements Serializable {

    /** 是否成功计算出距离 */
    private Boolean success;

    /** 起点地址 */
    private String originAddress;

    /** 终点地址 */
    private String destinationAddress;

    /** 驾车距离，单位：米；解析失败为 null */
    private Double distance;

    /** 是否可配送；解析失败为 null */
    private Boolean canDeliver;

}
