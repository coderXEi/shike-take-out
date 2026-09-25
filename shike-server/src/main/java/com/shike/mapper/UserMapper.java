package com.shike.mapper;


import com.shike.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {

    @Select("select * from user where openid = #{openid}")
    User getUserByOpenId(String openid);


    /**
     * 新增用户
     * @return
     */
    User insert(User user);
}
