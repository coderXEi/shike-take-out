package com.shike.controller.user;


import com.shike.constant.JwtClaimsConstant;
import com.shike.dto.UserLoginDTO;
import com.shike.entity.User;
import com.shike.properties.JwtProperties;
import com.shike.result.Result;
import com.shike.service.UserService;
import com.shike.utils.JwtUtil;
import com.shike.vo.UserLoginVO;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/user/user")
@Api(tags = "C端用户接口")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    JwtProperties jwtProperties;

    public Result<UserLoginVO> login(@RequestBody UserLoginDTO userLoginDTO) {
        log.info("微信登录接口:{}",userLoginDTO);
        User user = userService.wxlogin(userLoginDTO);
        Map<String,Object> claims = new HashMap<>();
        String token = JwtUtil.createJWT(jwtProperties.getUserSecretKey(),jwtProperties.getUserTtl(),claims);
        // 为微信用户生成jwt令牌

        claims.put(JwtClaimsConstant.USER_ID,user.getId());

        UserLoginVO userLoginVO = UserLoginVO.builder()
                .id(user.getId())
                .openid(user.getOpenid())
                .token(token)
                .build();


        return Result.success(userLoginVO);
    }
}
