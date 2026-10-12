package com.shike.mapper;


import com.github.pagehelper.Page;
import com.shike.anno.AutoFill;
import com.shike.dto.DishPageQueryDTO;
import com.shike.entity.Dish;
import com.shike.enumeration.OperationType;
import com.shike.vo.DishVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface DishMapper {


    /**
     * 插入菜品数据
     * @param dish
     */
    @AutoFill(value= OperationType.INSERT)
    void insert(Dish dish);

    /**
     * 分页查询菜品
     * @param dishPageQueryDTO
     * @return
     */
    Page<DishVO> pageQuery(DishPageQueryDTO dishPageQueryDTO);


    /**
     * 获取某个菜品
     * @param id
     */
    @Select("select * from dish where id =#{id}")
    Dish getById(Long id);

    /**
     * 统计指定菜品id中处于指定状态的数量
     * @param ids 菜品id集合
     * @param status 状态
     * @return 满足条件的数量
     */
    Long countByIdsAndStatus(@Param("ids") List<Long> ids, @Param("status") Integer status);

    /**
     * 根据id批量删除菜品
     * @param ids 菜品id集合
     * @return 删除的行数
     */
    int deleteByIds(@Param("ids") List<Long> ids);


    /**
     * 修改菜品数据
     * @param dish
     */
    @AutoFill(value= OperationType.UPDATE)
    void update(Dish dish);


    /**
     * 动态条件查询菜品
     * @param dish
     * @return
     */
    List<Dish> list(Dish dish);


    /**
     * 根据套餐id查询菜品
     * @param setmealId
     * @return
     */
    @Select("select a.* from dish a left join setmeal_dish b on a.id = b.dish_id where b.setmeal_id = #{setmealId}")
    List<Dish> getBySetmealId(Long setmealId);

    /**
     * 根据条件统计菜品数量
     * @param map
     * @return
     */
    Integer countByMap(Map map);


    /**
     * 统计最近 days 天内已完成订单中，销量最高的前 topNum 个在售菜品
     *
     * @param days   统计天数（如 14）
     * @param topNum 取前多少个（如 15）
     * @return 按销量倒序的菜品列表，可能为空
     */
    List<DishVO> getHotDish(@Param("days") Integer days, @Param("topNum") Integer topNum);


    /**
     * 【为你推荐】按用户历史订单计算个性化推荐菜品
     *
     * <p>思路：先从用户近 days 天已完成订单中挑出购买量最高的前 categoryTopN 个分类，
     * 再在这些分类的在售菜品里，按全站销量倒序取前 topNum 个。
     * 这样既能命中用户口味偏好，又不会只重复他已买过的菜，具备一定“推荐”新鲜度。
     *
     * @param userId        当前登录用户id
     * @param days          历史订单时间窗口（天）
     * @param categoryTopN  偏好分类数量上限
     * @param topNum        返回商品数量上限
     * @return 推荐菜品列表，无历史数据时返回空列表
     */
    List<DishVO> getRecommendDishByUser(@Param("userId") Long userId,
                                        @Param("days") Integer days,
                                        @Param("categoryTopN") Integer categoryTopN,
                                        @Param("topNum") Integer topNum);
}
