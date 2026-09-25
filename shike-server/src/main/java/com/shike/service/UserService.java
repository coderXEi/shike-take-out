package com.shike.service;


import com.shike.dto.UserLoginDTO;
import com.shike.entity.User;

public interface UserService {

    /**
     * 用户小程序段微信登录
     * @param userLoginDTO
     */
    User wxlogin(UserLoginDTO userLoginDTO);
}
