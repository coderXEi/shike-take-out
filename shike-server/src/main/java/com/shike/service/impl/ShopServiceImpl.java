package com.shike.service.impl;

import com.shike.entity.Shop;
import com.shike.mapper.ShopMapper;
import com.shike.service.ShopService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.SerializationException;
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
        Integer status = null;
        try {
            status = (Integer) redisTemplate.opsForValue().get(KEY);
        } catch (SerializationException e) {
            // 历史数据可能是 JDK 序列化格式，与当前 JSON 序列化器不兼容，
            // 直接删除脏数据并回源数据库，避免接口 500
            log.warn("店铺状态缓存反序列化失败，删除并回源数据库: {}", e.getMessage());
            redisTemplate.delete(KEY);
        }
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
