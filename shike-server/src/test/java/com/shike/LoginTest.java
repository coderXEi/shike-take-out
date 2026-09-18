package com.shike;


import com.shike.constant.JwtClaimsConstant;
import com.shike.properties.JwtProperties;
import com.shike.utils.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.DigestUtils;

import java.util.HashMap;
import java.util.Map;

@SpringBootTest
public class LoginTest {

    @Autowired
    JwtProperties jwtProperties;
    @Test
    public void testLogin() {


        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.EMP_ID, 1);

        String token = JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                claims);

        System.out.println(token);
    }
}
