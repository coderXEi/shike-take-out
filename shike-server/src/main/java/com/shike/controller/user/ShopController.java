package com.shike.controller.user;


import com.shike.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.*;

@RestController("userShopController")
@Slf4j
@RequestMapping("/user/status")
@Api(tags = "店铺设置接口")
public class ShopController {
    // 类名相同 创建在IOC容器的bean 名称相同 是默认值 类名小写
    public static final String KEY = "SHOP_STATUS";

    @Autowired
    RedisTemplate<String,Object> redisTemplate;

    /**
     * 获取店铺状态
     */
    @GetMapping("/status")
    @ApiOperation("获取店铺营业状态")
    public Result<Integer> getStatus() {

        Integer shopStatus =(Integer) redisTemplate.opsForValue().get(KEY);
        log.info("获取店铺的营业状态{}",shopStatus ==1 ? "营业" :"打烊");
        return Result.success(shopStatus);
    }
}
