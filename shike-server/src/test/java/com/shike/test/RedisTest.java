package com.shike.test;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Set;

public class RedisTest {

    @Autowired
    private RedisTemplate<String,Object> redisTemplate;

    @Test
    public void testRedisTemplate() {
        System.out.println(redisTemplate);
        ValueOperations vo = redisTemplate.opsForValue();
    }

    /**
     * 操作字符串类型数据
     */
    @Test
    public void testStringRedisTemplate() {
            // set  get setex  setnx

//        redisTemplate.opsForValue().set("city","北京");

        ValueOperations valueOperations = redisTemplate.opsForValue();
        valueOperations.set("city", "北京 ");
        System.out.println(valueOperations.get("city"));
    }

    @Test
    public void testHash(){
        // hset hget hdel hkeys hvals

        HashOperations hashOperations = redisTemplate.opsForHash();

        hashOperations.put("100","name","tom");
        hashOperations.put("100","age","50");

        Set keys = hashOperations.keys("100");

        Object name = hashOperations.get("100", "name");

        System.out.println((String)name);
    }

    @Test
    public void testList(){
    //  列表 LPUSH   LRANGE  RPOP   LLEN

        ListOperations listOperations = redisTemplate.opsForList();
        listOperations.leftPushAll("myList","a","b","c");

        listOperations.size("myList");
    }
    @Test
    public void testSet(){
        // sadd   smembers  scard
    }


    @Test
    public void testCommon() {
        // keys exists  type del
        Set<String> keys = redisTemplate.keys("*");

        redisTemplate.hasKey("*");
    }



}
