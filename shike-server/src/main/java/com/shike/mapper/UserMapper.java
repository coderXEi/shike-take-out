package com.shike.mapper;


import com.shike.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.time.LocalDate;
import java.util.Map;

@Mapper
public interface UserMapper {

    @Select("select * from user where openid = #{openid}")
    User getUserByOpenId(String openid);

    /**
     * 根据id查询用户
     * @param id
     * @return
     */
    @Select("select * from user where id = #{id}")
    User getById(Long id);


    /**
     * 新增用户
     * @return 影响行数
     */
    int insert(User user);

    /**
     * 统计用户
     * @param begin
     * @param end
     * @return
     */
    Integer countByMap(Map<String,Object> map);
}
