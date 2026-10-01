package com.shike.controller.admin;


import com.shike.result.Result;
import com.shike.service.ShopService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController("adminShopController")
@Slf4j
@RequestMapping("/admin/shop")
@Api(tags = "店铺设置接口")
public class ShopController {

    @Autowired
    private ShopService shopService;


    /**
     * 设置店铺营业状态
     *
     * @param status
     * @return
     */
    @PutMapping("/{status}")
    @ApiOperation("设置店铺营业状态")
    public Result setStatus(@PathVariable Integer status) {
        log.info("设置店铺营业状态:{}", status == 1 ? "营业" : "打烊 ");

        shopService.setStatus(status);
        return Result.success();
    }

    /**
     * 获取店铺状态
     */
    @GetMapping("/status")
    @ApiOperation("获取店铺营业状态")
    public Result getStatus() {

        Integer shopStatus = shopService.getStatus();
        log.info("获取店铺的营业状态{}", shopStatus == null ? "未知" : (shopStatus == 1 ? "营业" : "打烊"));
        return Result.success(shopStatus);
    }
}
