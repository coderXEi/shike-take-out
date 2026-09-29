package com.shike.service;

public interface ShopService {

    /**
     * 获取店铺营业状态：先查缓存，缓存没有则查数据库并回写缓存
     *
     * @return 营业状态 0:打烊 1:营业
     */
    Integer getStatus();

    /**
     * 设置店铺营业状态：更新数据库并同步缓存
     *
     * @param status 营业状态 0:打烊 1:营业
     */
    void setStatus(Integer status);
}
