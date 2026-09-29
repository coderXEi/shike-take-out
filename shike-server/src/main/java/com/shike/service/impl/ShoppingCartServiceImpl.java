package com.shike.service.impl;

import com.shike.dto.ShoppingCartDTO;
import com.shike.entity.Dish;
import com.shike.entity.Setmeal;
import com.shike.entity.ShoppingCart;
import com.shike.mapper.DishMapper;
import com.shike.mapper.SetmealMapper;
import com.shike.mapper.ShoppingCartMapper;
import com.shike.service.ShoppingCartService;
import com.shike.utils.CurrentHolder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    ShoppingCartMapper shoppingCartMapper;

    @Autowired
    DishMapper dishMapper;
    @Autowired
    SetmealMapper setmealMapper;

    @Override
    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        // 判断加入购物车的商品已经存在
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);

        Long userId = CurrentHolder.get();
        shoppingCart.setUserId(userId);

        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);

        // 存在则数量+1
        if(list != null && !list.isEmpty()) {
            //
            ShoppingCart cart = list.get(0);
            cart.setNumber(cart.getNumber() + 1);
            // 更新number
            shoppingCartMapper.updateNumber(cart);

        } else {
            // 不存在  这次添加的是菜品还是套餐
            Long dishId = shoppingCartDTO.getDishId();
            if(dishId != null) {
                // 添加的是菜品 否则是套餐
                Dish dish = dishMapper.getById(dishId);
                shoppingCart.setName(dish.getName());
                shoppingCart.setImage(dish.getImage());
                shoppingCart.setAmount(dish.getPrice());

                shoppingCart.setNumber(1);


            } else {
                // 是套餐
                Long setmealId = shoppingCartDTO.getSetmealId();
                Setmeal setmeal = setmealMapper.getById(setmealId);
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setImage(setmeal.getImage());
                shoppingCart.setAmount(setmeal.getPrice());
                shoppingCart.setNumber(1);



            }

            shoppingCartMapper.insert(shoppingCart);

        }

        // 不存在 则插入购物车数据

    }
}
