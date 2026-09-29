package com.shike.service;

import com.shike.dto.ShoppingCartDTO;
import com.shike.entity.ShoppingCart;

import java.util.List;

public interface ShoppingCartService {

    /**
     * 添加购物车方法
     * @param shoppingCartDTO
     */
    void addShoppingCart(ShoppingCartDTO shoppingCartDTO);


    /**\
     * 查看购物车
     * @return
     */
    List<ShoppingCart> showShoppingCart();

    void clean();

    /**
     * 减少购物车商品数（数量为 1 时删除该记录）
     * @param shoppingCartDTO
     */
    void subShoppingCart(ShoppingCartDTO shoppingCartDTO);
}
