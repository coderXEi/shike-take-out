package com.shike.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;


public class JwtUtil {

    /**
     * 由字符串秘钥生成 HMAC-SHA 签名密钥。
     *
     * <p>jjwt 0.12.x 起不再接受 {@code byte[]} 作为签名密钥，需显式构造 {@link SecretKey}。
     * 使用 {@link Keys#hmacShaKeyFor(byte[])} 时会校验长度（HS256 要求 >= 256 bit）。</p>
     */
    private static SecretKey signKey(String secretKey) {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成jwt
     * 使用Hs256算法, 私匙使用固定秘钥
     *
     * @param secretKey jwt秘钥
     * @param ttlMillis jwt过期时间(毫秒)
     * @param claims    设置的信息
     * @return
     */
    public static String createJWT(String secretKey, long ttlMillis, Map<String, Object> claims) {
        // 生成JWT的时间
        long expMillis = System.currentTimeMillis() + ttlMillis;
        Date exp = new Date(expMillis);

        // 设置jwt的body
        JwtBuilder builder = Jwts.builder()
                // 如果有私有声明，一定要先设置这个自己创建的私有的声明
                .claims(claims)
                // 设置签名使用的签名算法和签名使用的秘钥
                .signWith(signKey(secretKey))
                // 设置过期时间
                .expiration(exp);

        return builder.compact();
    }

    /**
     * Token解密
     *
     * @param secretKey jwt秘钥 此秘钥一定要保留好在服务端, 不能暴露出去, 否则sign就可以被伪造, 如果对接多个客户端建议改造成多个
     * @param token     加密后的token
     * @return
     */
    public static Claims parseJWT(String secretKey, String token) {
        // 得到 JwtParser，校验签名并解析
        return Jwts.parser()
                // 设置签名的秘钥
                .verifyWith(signKey(secretKey))
                .build()
                // 设置需要解析的jwt
                .parseSignedClaims(token)
                .getPayload();
    }

}
