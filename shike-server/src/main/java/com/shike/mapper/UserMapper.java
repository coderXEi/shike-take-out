package com.shike.mapper;


import com.shike.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

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
}
