package com.shike.service.impl;

import com.shike.entity.Shop;
import com.shike.mapper.ShopMapper;
import com.shike.service.ShopService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
public class ShopServiceImpl implements ShopService {

    public static final String KEY = "SHOP_STATUS";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ShopMapper shopMapper;

    @Override
    public Integer getStatus() {
        // 1. 先查缓存
        Integer status = (Integer) redisTemplate.opsForValue().get(KEY);
        if (status != null) {
            return status;
        }

        // 2. 缓存没有，查数据库
        Shop shop = shopMapper.getShop();
        if (shop == null || shop.getStatus() == null) {
            log.warn("店铺信息不存在，无法获取营业状态");
            return null;
        }
        status = shop.getStatus();

        // 3. 回写缓存
        redisTemplate.opsForValue().set(KEY, status);
        return status;
    }

    @Override
    public void setStatus(Integer status) {
        // 1. 更新数据库
        Shop shop = shopMapper.getShop();
        if (shop != null) {
            shop.setStatus(status);
            shop.setUpdateTime(LocalDateTime.now());
            shopMapper.updateStatus(shop);
        }

        // 2. 同步缓存
        redisTemplate.opsForValue().set(KEY, status);
    }
}
