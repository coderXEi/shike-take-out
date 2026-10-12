package com.shike.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.shike.json.JacksonObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@Slf4j
public class RedisConfiguration {

    @Bean
    public RedisTemplate redisTemplate(RedisConnectionFactory connectionFactory) {
        log.info("创建 redis模板对象");
        RedisTemplate redisTemplate =  new RedisTemplate();
        // 设置redis 连接工厂对象
        redisTemplate.setConnectionFactory(connectionFactory);

        // 基于 JacksonObjectMapper 构造 value 序列化器：
        // 1) 复用项目统一的 LocalDateTime/LocalDate/LocalTime 格式（否则缓存 DishVO 时会因
        //    LocalDateTime 无法序列化而抛出 "Type id handling not implemented for type java.lang.Object"）
        // 2) 开启默认类型信息，反序列化时才能还原成具体类型（List<DishVO> -> ArrayList<DishVO>）
        GenericJackson2JsonRedisSerializer valueSerializer = buildValueSerializer();

        // 设置redis key的序列化器  redis 的key会正常显示
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(valueSerializer);
        redisTemplate.setHashValueSerializer(valueSerializer);

        return redisTemplate;
    }

    /**
     * Spring Cache（@Cacheable / @CacheEvict）使用的缓存管理器。
     *
     * <p>若不显式声明，Spring Boot 会使用默认的 RedisCacheManager，
     * 其 value 序列化器为 JdkSerializationRedisSerializer，
     * 于是 setmealCache::5 这类 key 会存成二进制 Java 对象，
     * 可读性差、跨语言不兼容、且反序列化依赖类结构（改字段易报错）。
     *
     * <p>这里统一改为 Jackson JSON（与上面的 RedisTemplate 保持一致），
     * key 用 StringRedisSerializer 保证可读；
     * setmealCache 缓存的是套餐列表（读多写少、由管理端主动失效），
     * 默认不设 TTL，与原先 Jdk 序列化下的行为保持一致（永久直至 evict）。
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        log.info("创建 redis 缓存管理器（Spring Cache，JSON 序列化）");

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                // key 可读：setmealCache::5
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                // value 使用 JSON，而不是 JDK 二进制
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(buildValueSerializer()))
                // 不允许缓存 null，避免穿透时写入无意义的空值
                .disableCachingNullValues()
                // 默认不设置过期时间：由 @CacheEvict 主动清理（与原行为一致）
                .entryTtl(Duration.ZERO);

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }

    /**
     * 构造 Redis value 的 JSON 序列化器
     */
    private GenericJackson2JsonRedisSerializer buildValueSerializer() {
        ObjectMapper objectMapper = new JacksonObjectMapper();
        // 允许序列化所有字段（含 private），使实体/VO 无需 getter 也能正常缓存
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // 写入类型信息，保证反序列化后类型正确（注意：要求 JSON 中存在 @class 字段）
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
        );
        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }

}
