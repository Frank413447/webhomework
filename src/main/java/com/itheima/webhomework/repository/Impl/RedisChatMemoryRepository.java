package com.itheima.webhomework.repository.Impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的会话记忆持久化实现（使用 StringRedisTemplate + 统一 JSON 序列化）
 *
 * 设计要点：
 * - 使用 StringRedisTemplate 存储 JSON 字符串，避免 GenericJackson2JsonRedisSerializer 的类型擦除问题
 * - 所有序列化/反序列化使用同一个 ObjectMapper，保证格式一致
 * - MessageRecord 改为 public，确保 Jackson 能正常序列化和反序列化
 */
@Slf4j
public class RedisChatMemoryRepository implements ChatMemoryRepository {

    private static final String KEY_PREFIX = "chat:memory:";
    private static final long TTL_DAYS = 3;

    /** 统一的 ObjectMapper，读写两端共用，保证序列化格式一致 */
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final StringRedisTemplate stringRedisTemplate;

    public RedisChatMemoryRepository(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 消息记录（public → Jackson 可正常访问构造器）
     */
    public record MessageRecord(String role, String content) {}

    // ======================== ChatMemoryRepository 接口实现 ========================

    @Override
    public List<String> findConversationIds() {
        Set<String> keys = stringRedisTemplate.keys(KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return new ArrayList<>();
        }
        return keys.stream()
                .map(key -> key.substring(KEY_PREFIX.length()))
                .toList();
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        String key = KEY_PREFIX + conversationId;

        // 从 Redis List 读取全部 JSON 字符串
        List<String> rawMessages = stringRedisTemplate.opsForList().range(key, 0, -1);
        if (rawMessages == null || rawMessages.isEmpty()) {
            return new ArrayList<>();
        }

        List<Message> messages = new ArrayList<>();
        for (String json : rawMessages) {
            try {
                MessageRecord record = MAPPER.readValue(json, MessageRecord.class);
                if (record == null) continue;

                if (MessageType.USER.getValue().equals(record.role())) {
                    messages.add(new UserMessage(record.content()));
                } else if (MessageType.ASSISTANT.getValue().equals(record.role())) {
                    messages.add(new AssistantMessage(record.content()));
                }
            } catch (JsonProcessingException e) {
                log.warn("反序列化消息失败，跳过该条记录: {}", e.getMessage());
            }
        }
        log.debug("从 Redis 加载会话记忆 - conversationId: {}, 消息数: {}", conversationId, messages.size());
        return messages;
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        String key = KEY_PREFIX + conversationId;

        // 删除旧数据（全量覆盖）
        stringRedisTemplate.delete(key);

        if (messages == null || messages.isEmpty()) {
            return;
        }

        // 将 Message 转换为 MessageRecord → JSON 字符串
        List<String> jsonList = new ArrayList<>();
        for (Message msg : messages) {
            try {
                MessageRecord record = new MessageRecord(
                        msg.getMessageType().getValue(),
                        msg.getText()
                );
                jsonList.add(MAPPER.writeValueAsString(record));
            } catch (JsonProcessingException e) {
                log.warn("序列化消息失败，跳过该条: {}", e.getMessage());
            }
        }

        if (!jsonList.isEmpty()) {
            stringRedisTemplate.opsForList().rightPushAll(key, jsonList);
            stringRedisTemplate.expire(key, TTL_DAYS, TimeUnit.DAYS);
        }

        log.debug("保存会话记忆到 Redis - conversationId: {}, 消息数: {}", conversationId, messages.size());
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        String key = KEY_PREFIX + conversationId;
        stringRedisTemplate.delete(key);
        log.debug("清空会话记忆 - conversationId: {}", conversationId);
    }
}
