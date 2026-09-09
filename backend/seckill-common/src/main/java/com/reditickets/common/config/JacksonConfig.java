package com.reditickets.common.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Jackson 序列化配置
 * <p>
 * 统一配置整个应用的 JSON 序列化和反序列化行为，解决 Spring Boot 默认 Jackson 配置的以下痛点：
 * <ul>
 *   <li><b>日期格式统一</b>：LocalDateTime/LocalDate/LocalTime 统一输出为 yyyy-MM-dd HH:mm:ss，
 *       替代默认的数组格式 [2026,1,1,12,0,0]，可读性更强</li>
 *   <li><b>Long 精度保护</b>：Long 类型序列化为 String，防止前端 JavaScript 丢失精度
 *       （JavaScript 的 Number 类型最大安全整数为 2^53-1，超出会丢失精度）</li>
 *   <li><b>容错反序列化</b>：忽略未知 JSON 属性，避免接口升级后旧客户端传参导致反序列化失败</li>
 *   <li><b>禁用时间戳</b>：禁止将日期序列化为毫秒时间戳，强制使用格式化字符串</li>
 * </ul>
 * </p>
 *
 * @author gugu
 */
@Configuration
public class JacksonConfig {

    /** 日期时间格式：yyyy-MM-dd HH:mm:ss */
    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    /** 日期格式：yyyy-MM-dd */
    private static final String DATE_PATTERN = "yyyy-MM-dd";
    /** 时间格式：HH:mm:ss */
    private static final String TIME_PATTERN = "HH:mm:ss";

    /**
     * 全局 Jackson 自定义配置器
     * <p>
     * 通过 Jackson2ObjectMapperBuilderCustomizer 回调接口，
     * 在 Spring Boot 自动配置的 ObjectMapper 基础上追加自定义行为。
     * 该 Bean 会影响所有 Controller 返回的 JSON 序列化。
     * </p>
     *
     * @return Jackson 构建器定制器
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        return builder -> {
            // 1. 设置全局日期格式化（用于 java.util.Date 类型）
            builder.simpleDateFormat(DATE_TIME_PATTERN);

            // 2. 注册 Java 8 时间类型（LocalDateTime/LocalDate/LocalTime）的序列化器
            // 序列化时：Java 对象 → JSON 字符串，按指定格式输出
            builder.serializers(new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN)));
            builder.serializers(new LocalDateSerializer(DateTimeFormatter.ofPattern(DATE_PATTERN)));
            builder.serializers(new LocalTimeSerializer(DateTimeFormatter.ofPattern(TIME_PATTERN)));

            // 3. 注册 Java 8 时间类型（LocalDateTime/LocalDate/LocalTime）的反序列化器
            // 反序列化时：JSON 字符串 → Java 对象，按指定格式解析
            builder.deserializers(new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN)));
            builder.deserializers(new LocalDateDeserializer(DateTimeFormatter.ofPattern(DATE_PATTERN)));
            builder.deserializers(new LocalTimeDeserializer(DateTimeFormatter.ofPattern(TIME_PATTERN)));

            // 4. 禁用日期序列化为时间戳（默认行为是将日期转为毫秒数，如 1700000000000）
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            // 5. 忽略 JSON 中的未知属性，避免反序列化时抛出 UnrecognizedPropertyException
            builder.featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        };
    }

    /**
     * 自定义 ObjectMapper 实例
     * <p>
     * 注册 JavaTimeModule 模块，配置：
     * <ul>
     *   <li>Long 类型（含包装类和基本类型）序列化为 String，避免前端精度丢失</li>
     *   <li>Java 8 时间类型统一按 yyyy-MM-dd HH:mm:ss 格式序列化/反序列化</li>
     *   <li>禁用日期时间戳模式和未知属性失败策略</li>
     * </ul>
     * 该 ObjectMapper 可用于 Redis 序列化、消息队列序列化等需要手动配置的场景。
     * </p>
     *
     * @return 自定义配置的 ObjectMapper 实例
     */
    @Bean
    public ObjectMapper objectMapper() {
        // 1. 创建 ObjectMapper 实例
        ObjectMapper objectMapper = new ObjectMapper();

        // 2. 创建 Java 8 时间模块，集中注册序列化器和反序列化器
        JavaTimeModule javaTimeModule = new JavaTimeModule();

        // 3. Long 类型序列化为 String（防止前端 JavaScript 精度丢失）
        // Long.TYPE 对应 long 基本类型，Long.class 对应 Long 包装类
        javaTimeModule.addSerializer(Long.class, ToStringSerializer.instance);
        javaTimeModule.addSerializer(Long.TYPE, ToStringSerializer.instance);

        // 4. 注册 Java 8 时间类型的序列化器（Java 对象 → JSON 字符串）
        javaTimeModule.addSerializer(LocalDateTime.class,
                new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN)));
        javaTimeModule.addSerializer(LocalDate.class,
                new LocalDateSerializer(DateTimeFormatter.ofPattern(DATE_PATTERN)));
        javaTimeModule.addSerializer(LocalTime.class,
                new LocalTimeSerializer(DateTimeFormatter.ofPattern(TIME_PATTERN)));

        // 5. 注册 Java 8 时间类型的反序列化器（JSON 字符串 → Java 对象）
        javaTimeModule.addDeserializer(LocalDateTime.class,
                new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN)));
        javaTimeModule.addDeserializer(LocalDate.class,
                new LocalDateDeserializer(DateTimeFormatter.ofPattern(DATE_PATTERN)));
        javaTimeModule.addDeserializer(LocalTime.class,
                new LocalTimeDeserializer(DateTimeFormatter.ofPattern(TIME_PATTERN)));

        // 6. 将配置好的 JavaTimeModule 注册到 ObjectMapper
        objectMapper.registerModule(javaTimeModule);

        // 7. 禁用日期序列化为时间戳，确保日期以字符串格式输出
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 8. 忽略 JSON 中的未知属性，提高反序列化的容错性
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        return objectMapper;
    }
}