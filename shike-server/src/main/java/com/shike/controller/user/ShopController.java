package com.shike.controller.user;


import com.shike.result.Result;
import com.shike.service.ShopService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController("userShopController")
@Slf4j
@RequestMapping("/user/shop")
@Tag(name = "店铺设置接口")
public class ShopController {

    @Autowired
    private ShopService shopService;

    /**
     * 获取店铺状态
     */
    @GetMapping("/status")
    @Operation(summary = "获取店铺营业状态")
    public Result<Integer> getStatus() {

        Integer shopStatus = shopService.getStatus();
        log.info("获取店铺的营业状态{}", shopStatus == null ? "未知" : (shopStatus == 1 ? "营业" : "打烊"));
        return Result.success(shopStatus);
    }
}
