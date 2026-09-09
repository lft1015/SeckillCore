package com.reditickets.common.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 序列化配置
 * <p>
 * 配置 RedisTemplate 使用 JSON 序列化方式存储对象，替代默认的 JDK 序列化。
 * 改造后带来的好处：
 * <ul>
 *   <li><b>可读性</b>：Redis 中存储的是 JSON 字符串，可通过 redis-cli 直接查看</li>
 *   <li><b>跨语言</b>：JSON 格式可被其他语言客户端（如 Go、Python）直接读取</li>
 *   <li><b>体积小</b>：JSON 序列化比 JDK 序列化占用更少的存储空间</li>
 *   <li><b>无需 Serializable</b>：实体类无需实现 Serializable 接口</li>
 * </ul>
 * </p>
 *
 * @author gugu
 */
@Configuration
public class RedisConfig {

    /**
     * 创建并配置 RedisTemplate 实例
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        // 1. 创建 RedisTemplate 并设置连接工厂
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 2. 创建 Jackson 的 ObjectMapper，用于 JSON 序列化
        ObjectMapper objectMapper = new ObjectMapper();
        // 设置对所有字段（包括私有字段）可见，使 Jackson 能序列化所有属性
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // 开启默认类型信息嵌入，序列化时在 JSON 中写入 @class 字段，
        // 反序列化时根据 @class 自动还原为正确的 Java 对象类型
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);

        // 3. 创建 JSON 序列化器（用于 Value 序列化）
        Jackson2JsonRedisSerializer<Object> jacksonSerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);

        // 4. 创建字符串序列化器（用于 Key 序列化）
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // 5. 设置各类 Key/Value 的序列化方式
        // 普通 Key 使用字符串序列化，确保 Redis 中 Key 可读
        template.setKeySerializer(stringSerializer);
        // Hash 结构的 Key 使用字符串序列化
        template.setHashKeySerializer(stringSerializer);
        // 普通 Value 使用 JSON 序列化，对象自动转为 JSON 字符串
        template.setValueSerializer(jacksonSerializer);
        // Hash 结构的 Value 使用 JSON 序列化
        template.setHashValueSerializer(jacksonSerializer);

        // 6. 初始化 RedisTemplate，使上述配置生效
        template.afterPropertiesSet();
        return template;
    }
}