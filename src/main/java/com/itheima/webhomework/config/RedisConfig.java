
// 声明包名
package com.itheima.webhomework.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

// @Configuration：告诉 Spring 这是一个配置类，启动时会扫描并执行其中的 @Bean 方法
@Configuration
public class RedisConfig {

    // @Bean：这个方法返回的 RedisTemplate 对象会被注册到 Spring 容器中
    // 其他地方可以通过 @Autowired 注入使用
    // 参数 RedisConnectionFactory factory 由 Spring 自动注入
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        // 创建一个空的 RedisTemplate 实例，泛型 <String, Object> 表示 Key 是字符串，Value 可以是任意对象
        RedisTemplate<String, Object> template = new RedisTemplate<>();

        // 将 Spring 注入的连接工厂设置到 template 中，这样 template 就知道如何连接 Redis 了
        template.setConnectionFactory(factory);

        // 设置普通 Key 的序列化器（如 "login:admin"）
        // 使用 StringRedisSerializer 保证 Key 在 Redis 中以明文显示，不会乱码
        template.setKeySerializer(new StringRedisSerializer());

        // 设置 Hash 结构中 field 的序列化器
        // 同样使用字符串序列化，保证 Hash 的 field 也是明文
        template.setHashKeySerializer(new StringRedisSerializer());

        // 创建一个 Jackson 的 ObjectMapper 实例，用于配置 JSON 序列化规则
        // 注意：这里是局部变量，不会影响 HTTP 响应的 JSON 序列化
        ObjectMapper objectMapper = new ObjectMapper();

        // 设置所有属性都可见（包括 private、protected、public）
        // PropertyAccessor.ALL：包括所有访问级别的属性
        // JsonAutoDetect.Visibility.ANY：任何可见性级别都可以被序列化
        // 作用：确保对象的私有字段也能被序列化到 JSON 中
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

        // 激活默认类型信息：在 JSON 中添加 @class 字段，记录对象的完整类名
        // LaissezFaireSubTypeValidator.instance：允许所有类型（开发环境方便，生产环境建议用白名单）
        // ObjectMapper.DefaultTyping.NON_FINAL：对非 final 类添加类型信息
        // 作用：反序列化时能正确还原成原始类型，比如 ChatHistoryEntity 而不是 LinkedHashMap
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL
        );

        // 注册 JavaTimeModule 模块
        // 作用：支持 Java 8 时间类型（LocalDateTime、LocalDate、LocalTime 等）的序列化和反序列化
        // 不注册的后果：序列化 LocalDateTime 会报错：Java 8 date/time type not supported
        objectMapper.registerModule(new JavaTimeModule());

        // 创建通用 JSON 序列化器，传入我们刚才配置的 ObjectMapper
        // 作用：将 Java 对象序列化成 JSON 存入 Redis，从 Redis 读取时再反序列化成 Java 对象
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        // 设置普通 Value 的序列化器（如 string key -> value）
        // 使用 JSON 格式存储，可读性好，且包含类型信息
        template.setValueSerializer(serializer);

        // 设置 Hash 结构中 value 的序列化器（如 hash key -> field -> value）
        // 使用同一个 serializer，保证序列化规则一致
        template.setHashValueSerializer(serializer);

        // 初始化 template：检查所有必要的属性是否都已设置，完成内部的初始化工作
        // 必须在所有配置完成后调用，否则可能抛出异常
        template.afterPropertiesSet();

        // 返回配置好的 RedisTemplate 实例
        // Spring 会将这个对象放入容器中，其他类可以通过 @Autowired 注入使用
        return template;
    }
}
