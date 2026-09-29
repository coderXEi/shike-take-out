package com.shike.controller.user;

import com.shike.dto.ShoppingCartDTO;
import com.shike.entity.ShoppingCart;
import com.shike.result.Result;
import com.shike.service.ShoppingCartService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.message.ReusableMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/shoppingCart")
@Api(tags = "用户端 购物车接口")
@Slf4j
public class ShoppingCartController {
    @Autowired
    private ShoppingCartService shoppingCartService;

    @PostMapping("/add")
    @ApiOperation("添加购物车功能")
    public Result add(@RequestBody ShoppingCartDTO shoppingCartDTO){
        log.info("添加购物车 {}",shoppingCartDTO);
        shoppingCartService.addShoppingCart(shoppingCartDTO);
        return Result.success();
    }

    @GetMapping("/list")
    @ApiOperation("查看购物车接口")
    public Result<List<ShoppingCart>> list() {
        List<ShoppingCart> shoppingCarts = shoppingCartService.showShoppingCart();
        return Result.success(shoppingCarts);
    }


    /**
     * 减少购物车数量
     * @return
     */
    @PostMapping("/sub")
    @ApiOperation("减少购物车商品数")
    public Result sub(@RequestBody ShoppingCartDTO shoppingCartDTO){
        log.info("减少购物车 {}",shoppingCartDTO);
        shoppingCartService.subShoppingCart(shoppingCartDTO);
        return Result.success();
    }


    /**
     * 清空购物车
     * @return
     */
    @DeleteMapping("/clean")
    @ApiOperation("清空购物车")
    public Result clean(){

        shoppingCartService.clean();
        return Result.success();
    }
}
