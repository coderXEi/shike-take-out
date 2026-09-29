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

import java.time.LocalDateTime;
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
            cart.setCreateTime(LocalDateTime.now());
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
                shoppingCart.setCreateTime(LocalDateTime.now());
            }

            shoppingCartMapper.insert(shoppingCart);
        }

        // 不存在 则插入购物车数据

    }

    @Override
    public List<ShoppingCart> showShoppingCart() {
        Long l = CurrentHolder.get();
        ShoppingCart shoppingCart = ShoppingCart.builder().userId(l).build();
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        return list != null ? list : java.util.Collections.emptyList();
    }

    @Override
    public void clean() {
        Long l = CurrentHolder.get();
        shoppingCartMapper.cleanById(l);
    }

    @Override
    public void subShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        // 构造查询条件：当前用户 + 菜品/套餐 + 口味
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        shoppingCart.setUserId(CurrentHolder.get());

        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        if (list == null || list.isEmpty()) {
            // 购物车中没有该商品，无需减少
            return;
        }

        ShoppingCart cart = list.get(0);
        if (cart.getNumber() == null || cart.getNumber() <= 1) {
            // 数量为 1，再减则删除该记录，避免出现 number = 0 的幽灵行
            shoppingCartMapper.deleteById(cart.getId());
        } else {
            cart.setNumber(cart.getNumber() - 1);
            shoppingCartMapper.updateNumber(cart);
        }
    }
}
