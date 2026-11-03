package com.itheima.webhomework.repository;

import com.itheima.webhomework.entity.ChatHistoryEntity;

import java.util.List;                                      // 列表类型

/**
 * 聊天历史仓储接口
 * 定义聊天历史的核心业务操作方法
 * 采用"缓存+数据库"双层架构，提升性能并保证数据可靠性
 */
public interface ChatHistoryRepository {

    /**
     * 保存会话ID（用于维护会话列表）
     * @param type 业务类型：chat/service/pdf
     * @param chatId 会话ID
     */
    void save(String type, String chatId);

    /**
     * 获取某个业务类型下的所有会话ID列表
     * @param type 业务类型
     * @return 会话ID列表，按最新时间排序
     */
    List<String> getChatIds(String type);

    /**
     * 保存单条消息（同时写入数据库和Redis缓存）
     * @param type 业务类型
     * @param chatId 会话ID
     * @param role 消息角色：user/assistant/system
     * @param content 消息内容
     */
    void saveMessage(String type, String chatId, String role, String content);

    /**
     * 获取某个会话的完整历史消息（优先从Redis读取，未命中则查数据库）
     * @param type 业务类型
     * @param chatId 会话ID
     * @return 消息列表，按时间升序
     */
    List<ChatHistoryEntity> getMessages(String type, String chatId);

    /**
     * 分页获取某个会话的历史消息
     * @param type 业务类型
     * @param chatId 会话ID
     * @param pageNum 页码（从1开始）
     * @param pageSize 每页数量
     * @return 分页后的消息列表
     */
    List<ChatHistoryEntity> getMessagesWithPage(String type, String chatId, int pageNum, int pageSize);

    /**
     * 删除某个会话的所有历史记录（同时清理Redis缓存）
     * @param type 业务类型
     * @param chatId 会话ID
     */
    void deleteMessages(String type, String chatId);

    /**
     * 统计某个业务类型下的总会话数
     * @param type 业务类型
     * @return 会话数量
     */
    int countChatIds(String type);
}
