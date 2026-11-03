package com.itheima.webhomework.repository.Impl;

import com.itheima.webhomework.mapper.ChatHistoryMapper;           // 导入 Mapper 接口
import com.itheima.webhomework.entity.ChatHistoryEntity;           // 导入实体类
import com.itheima.webhomework.repository.ChatHistoryRepository;    // 导入 Repository 接口
import lombok.RequiredArgsConstructor;                              // Lombok 注解，自动生成构造函数
import org.springframework.data.redis.core.RedisTemplate;           // Redis 操作模板
import org.springframework.stereotype.Component;                    // Spring 组件注解

import java.time.LocalDateTime;                                     // Java 8 时间类型
import java.util.*;                                                 // 集合工具类
import java.util.concurrent.TimeUnit;                               // 时间单位
import java.util.stream.Collectors;                                 // 流式处理工具

/**
 * 聊天历史仓储实现类
 * 采用"Redis 缓存 + MySQL 数据库"双层架构
 *
 * 核心设计思想：
 * 1. 写操作：同时写入 MySQL（持久化）和 Redis（缓存加速）
 * 2. 读操作：优先读 Redis（快），未命中再读 MySQL（可靠），并回填缓存
 * 3. 缓存过期：24小时自动过期，避免内存无限增长
 * 4. 数据一致性：删除操作同时清理 Redis 和 MySQL
 */
@Component                        // Spring 组件注解，自动注册为 Bean
@RequiredArgsConstructor         // Lombok 注解，根据 final 字段生成构造函数
public class InMemoryChatHistoryRepository implements ChatHistoryRepository {

    private final ChatHistoryMapper chatHistoryMapper;          // MyBatis Mapper，操作数据库
    private final RedisTemplate<String, Object> redisTemplate;  // Redis 模板，操作缓存

    // Redis Key 前缀常量（统一管理，避免硬编码）
    private static final String CHAT_IDS_CACHE_KEY = "chat:ids:";              // 会话ID列表的 Key 前缀
    private static final String CHAT_MESSAGES_CACHE_KEY = "chat:messages:";    // 会话消息列表的 Key 前缀
    private static final long CACHE_EXPIRE_HOURS = 24;                         // 缓存过期时间：24小时

    /**
     * 保存会话ID到 Redis（用于维护会话列表）
     * 只在首次创建会话时调用，避免重复添加
     *
     * @param type 业务类型：chat/service/pdf
     * @param chatId 会话ID
     */
    @Override
    public void save(String type, String chatId) {
        // 构造 Redis Key：chat:ids:chat
        String cacheKey = CHAT_IDS_CACHE_KEY + type;

        // 检查该 chatId 是否已存在于 Set 中
        Boolean exists = redisTemplate.opsForSet().isMember(cacheKey, chatId);

        // 如果不存在，则添加到 Set 集合并设置过期时间
        if (Boolean.FALSE.equals(exists)) {
            redisTemplate.opsForSet().add(cacheKey, chatId);                      // 添加到 Set（自动去重）
            redisTemplate.expire(cacheKey, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);  // 设置24小时过期
        }
    }

    /**
     * 保存单条消息（双层写入：MySQL + Redis）
     *
     * 执行流程：
     * 1. 创建实体对象并设置属性
     * 2. 插入 MySQL 数据库（持久化，保证数据不丢失）
     * 3. 追加到 Redis List（缓存加速，下次读取更快）
     * 4. 设置缓存过期时间
     *
     * @param type 业务类型
     * @param chatId 会话ID
     * @param role 消息角色：user/assistant/system
     * @param content 消息内容
     */
    @Override
    public void saveMessage(String type, String chatId, String role, String content) {
        // 第1步：创建聊天历史实体对象
        ChatHistoryEntity entity = new ChatHistoryEntity();
        entity.setType(type);                        // 设置业务类型
        entity.setChatId(chatId);                    // 设置会话ID
        entity.setRole(role);                        // 设置消息角色
        entity.setContent(content);                  // 设置消息内容
        entity.setCreateTime(LocalDateTime.now());   // 设置当前时间为创建时间

        // 第2步：插入 MySQL 数据库（持久化存储）
        chatHistoryMapper.insert(entity);

        // 第3步：追加到 Redis List（缓存加速）
        String cacheKey = CHAT_MESSAGES_CACHE_KEY + type + ":" + chatId;  // 构造 Key：chat:messages:chat:session_xxx
        redisTemplate.opsForList().rightPush(cacheKey, entity);            // 从右侧追加（保持时间顺序）

        // 第4步：设置缓存过期时间（24小时后自动删除）
        redisTemplate.expire(cacheKey, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
    }

    /**
     * 获取某个业务类型下的所有会话ID列表
     *
     * 读取策略：
     * 1. 优先从 Redis Set 读取（速度快）
     * 2. 如果 Redis 未命中，从 MySQL 查询并回填缓存
     * 3. 返回按最新时间排序的会话ID列表
     *
     * @param type 业务类型
     * @return 会话ID列表
     */
    @Override
    public List<String> getChatIds(String type) {
        // 构造 Redis Key
        String cacheKey = CHAT_IDS_CACHE_KEY + type;

        // 尝试从 Redis Set 中获取所有成员
        Set<Object> members = redisTemplate.opsForSet().members(cacheKey);

        // 如果 Redis 中有数据，直接返回（缓存命中）
        if (members != null && !members.isEmpty()) {
            return members.stream()
                    .map(Object::toString)              // 将 Object 转为 String
                    .collect(Collectors.toList());       // 收集为 List
        }

        // Redis 未命中，从 MySQL 查询
        List<String> chatIds = chatHistoryMapper.selectChatIdsByType(type);

        // 如果查询到数据，回填到 Redis 缓存
        if (!chatIds.isEmpty()) {
            redisTemplate.opsForSet().add(cacheKey, chatIds.toArray());       // 批量添加到 Set
            redisTemplate.expire(cacheKey, CACHE_EXPIRE_HOURS, TimeUnit.HOURS); // 设置过期时间
        }

        return chatIds;  // 返回查询结果
    }

    /**
     * 获取某个会话的完整历史消息
     *
     * 读取策略（经典缓存模式）：
     * 1. 先查 Redis List（快速响应）
     * 2. 如果缓存未命中，查 MySQL 数据库
     * 3. 将数据库结果写入 Redis 缓存（下次更快）
     * 4. 返回按时间升序的消息列表
     *
     * @param type 业务类型
     * @param chatId 会话ID
     * @return 消息列表
     */
    @Override
    public List<ChatHistoryEntity> getMessages(String type, String chatId) {
        // 构造 Redis Key
        String cacheKey = CHAT_MESSAGES_CACHE_KEY + type + ":" + chatId;

        // 尝试从 Redis List 中获取所有元素（0 到 -1 表示全部）
        List<Object> cached = redisTemplate.opsForList().range(cacheKey, 0, -1);

        // 如果 Redis 中有数据，直接返回（缓存命中）
        if (cached != null && !cached.isEmpty()) {
            return cached.stream()
                    .filter(obj -> obj instanceof ChatHistoryEntity)  // 过滤出正确类型的对象
                    .map(obj -> (ChatHistoryEntity) obj)               // 类型转换
                    .collect(Collectors.toList());                      // 收集为 List
        }

        // Redis 未命中，从 MySQL 查询
        List<ChatHistoryEntity> messages = chatHistoryMapper.selectByTypeAndChatId(type, chatId);

        // 如果查询到数据，回填到 Redis 缓存
        if (!messages.isEmpty()) {
            redisTemplate.opsForList().rightPushAll(cacheKey, messages);  // 批量追加到 List
            redisTemplate.expire(cacheKey, CACHE_EXPIRE_HOURS, TimeUnit.HOURS); // 设置过期时间
        }

        return messages;  // 返回查询结果
    }

    /**
     * 分页获取某个会话的历史消息
     *
     * 适用场景：
     * - 单次对话消息超过100条时，避免一次性加载过多数据
     * - 前端实现"滚动加载更多"功能
     *
     * 注意：分页查询不使用缓存，因为分页参数组合太多，缓存命中率低
     *
     * @param type 业务类型
     * @param chatId 会话ID
     * @param pageNum 页码（从1开始）
     * @param pageSize 每页数量
     * @return 分页后的消息列表
     */
    @Override
    public List<ChatHistoryEntity> getMessagesWithPage(String type, String chatId, int pageNum, int pageSize) {
        // 计算偏移量：(页码 - 1) × 每页数量
        int offset = (pageNum - 1) * pageSize;

        // 直接从数据库查询分页数据（不走缓存）
        return chatHistoryMapper.selectByTypeAndChatIdWithPage(type, chatId, offset, pageSize);
    }

    /**
     * 删除某个会话的所有历史记录
     *
     * 清理策略：
     * 1. 先删除 MySQL 中的数据（持久化层）
     * 2. 再删除 Redis 中的缓存（避免脏数据）
     * 3. 从会话ID列表中移除该 chatId
     *
     * @param type 业务类型
     * @param chatId 会话ID
     */
    @Override
    public void deleteMessages(String type, String chatId) {
        // 第1步：删除 MySQL 中的数据
        chatHistoryMapper.deleteByTypeAndChatId(type, chatId);

        // 第2步：删除 Redis 中的消息缓存
        String messageCacheKey = CHAT_MESSAGES_CACHE_KEY + type + ":" + chatId;
        redisTemplate.delete(messageCacheKey);

        // 第3步：从会话ID列表中移除该 chatId
        String idsCacheKey = CHAT_IDS_CACHE_KEY + type;
        redisTemplate.opsForSet().remove(idsCacheKey, chatId);
    }

    /**
     * 统计某个业务类型下的总会话数
     *
     * 用途：
     * - 前端展示"共有 X 次对话"
     * - 监控系统负载
     *
     * @param type 业务类型
     * @return 会话数量
     */
    @Override
    public int countChatIds(String type) {
        return chatHistoryMapper.countChatIdsByType(type);
    }
}
