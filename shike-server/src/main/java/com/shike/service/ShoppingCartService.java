package com.shike.service;

import com.shike.dto.ShoppingCartDTO;

public interface ShoppingCartService {

    /**
     * 添加购物车方法
     * @param shoppingCartDTO
     */
    void addShoppingCart(ShoppingCartDTO shoppingCartDTO);
}
