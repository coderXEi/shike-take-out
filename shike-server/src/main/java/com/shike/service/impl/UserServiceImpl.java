package com.shike.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.shike.constant.MessageConstant;
import com.shike.dto.UserLoginDTO;
import com.shike.entity.User;
import com.shike.exception.LoginFailedException;
import com.shike.mapper.UserMapper;
import com.shike.properties.WeChatProperties;
import com.shike.service.UserService;
import com.shike.utils.HttpClientUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service

public class UserServiceImpl implements UserService {

    public static final String wx_login_url = "https://api.weixin.qq.com/sns/jscode2session";

    @Autowired
    WeChatProperties weChatProperties;
    @Autowired
    private UserMapper userMapper;

    @Override
    public User wxlogin(UserLoginDTO userLoginDTO) {
        // 调用微信服务器接口 获取openid
        String s = getOpenId(userLoginDTO);
        // 解析json数据
        JSONObject jsonObject = JSON.parseObject(s);
        String openid = jsonObject.getString("openid");

        if(openid == null) {
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }
        // 判断是否为新用户
        User user = userMapper.getUserByOpenId(openid);

        if(user == null) {
            // 不存在 是新用户 则新增该用户
            user = User.builder().openid(openid).createTime(LocalDateTime.now()).build();

            userMapper.insert(user);
        }

        // code 是否为空，空则异常 失败
        // 判断当前code是否在用户表 若不再 则为新用户 新增新用户

        return null;
    }

    private String getOpenId(UserLoginDTO userLoginDTO) {
        Map<String, String> map = new HashMap<String, String>();
        map.put("appid",weChatProperties.getAppid());
        map.put("secret",weChatProperties.getSecret());
        map.put("js_code", userLoginDTO.getCode());
        map.put("grant_type","authorization_code");
        String s = HttpClientUtil.doGet(wx_login_url, map);
        return s;
    }
}
