package com.shike;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.DigestUtils;
@SpringBootTest
public class LoginTest {
    @Test
    public void testLogin() {
        String s = DigestUtils.md5DigestAsHex("123456".getBytes());

        System.out.println(s);
    }
}
