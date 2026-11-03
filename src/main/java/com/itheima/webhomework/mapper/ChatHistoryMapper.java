package com.itheima.webhomework.mapper;

import com.itheima.webhomework.entity.ChatHistoryEntity;
import org.apache.ibatis.annotations.*;                     // MyBatis 注解
import java.util.List;                                      // 列表类型

/**
 * 聊天历史数据访问层接口
 * 负责与数据库交互，执行 CRUD 操作
 * 使用注解方式编写 SQL，适合简单查询
 */
@Mapper  // MyBatis 注解，标记为 Mapper 接口，Spring 会自动生成实现类
public interface ChatHistoryMapper {

    /**
     * 插入一条聊天历史记录
     * @param entity 聊天历史实体对象
     * @return 无返回值，插入失败会抛出异常
     */
    @Insert("INSERT INTO chat_history(type, chat_id, role, content, create_time) " +
            "VALUES(#{type}, #{chatId}, #{role}, #{content}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")  // 返回自增的主键ID
    void insert(ChatHistoryEntity entity);

    /**
     * 查询某个业务类型下的所有会话ID（去重并按最新时间排序）
     * 用于前端展示"会话历史列表"
     * @param type 业务类型，如：chat
     * @return 会话ID列表，按最新消息时间降序排列
     */
    @Select("SELECT chat_id FROM chat_history " +
            "WHERE type = #{type} " +
            "GROUP BY chat_id " +
            "ORDER BY MAX(create_time) DESC")
    List<String> selectChatIdsByType(String type);

    /**
     * 查询某个会话的完整历史消息（按时间升序）
     * 用于前端展示"某次会话的完整对话"
     * @param type 业务类型
     * @param chatId 会话ID
     * @return 消息列表，按创建时间升序（从早到晚）
     */
    @Select("SELECT * FROM chat_history " +
            "WHERE type = #{type} AND chat_id = #{chatId} " +
            "ORDER BY create_time ASC")
    List<ChatHistoryEntity> selectByTypeAndChatId(@Param("type") String type,
                                                  @Param("chatId") String chatId);

    /**
     * 分页查询某个会话的历史消息
     * 用于大数据量时的分页加载，避免一次性查询过多数据
     * @param type 业务类型
     * @param chatId 会话ID
     * @param offset 偏移量（从第几条开始）
     * @param limit 每页数量
     * @return 分页后的消息列表
     */
    @Select("SELECT * FROM chat_history " +
            "WHERE type = #{type} AND chat_id = #{chatId} " +
            "ORDER BY create_time ASC " +
            "LIMIT #{limit} OFFSET #{offset}")
    List<ChatHistoryEntity> selectByTypeAndChatIdWithPage(@Param("type") String type,
                                                          @Param("chatId") String chatId,
                                                          @Param("offset") int offset,
                                                          @Param("limit") int limit);

    /**
     * 删除某个会话的所有历史记录
     * 用于"清空对话"功能
     * @param type 业务类型
     * @param chatId 会话ID
     */
    @Delete("DELETE FROM chat_history WHERE type = #{type} AND chat_id = #{chatId}")
    void deleteByTypeAndChatId(@Param("type") String type, @Param("chatId") String chatId);

    /**
     * 统计某个业务类型下的总会话数
     * 用于前端展示"共有多少次对话"
     * @param type 业务类型
     * @return 会话数量
     */
    @Select("SELECT COUNT(DISTINCT chat_id) FROM chat_history WHERE type = #{type}")
    int countChatIdsByType(String type);

    /**
     * 清理过期的聊天记录（保留最近N天）
     * 用于定时任务，避免数据库无限增长
     * @param days 保留天数，如：30 表示保留最近30天
     * @return 删除的记录数
     */
    @Delete("DELETE FROM chat_history WHERE create_time < DATE_SUB(NOW(), INTERVAL #{days} DAY)")
    int deleteExpiredRecords(int days);
}
