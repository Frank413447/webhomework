package com.itheima.webhomework.config;

import com.itheima.webhomework.ai.tool.AiAssistantTools;
import com.itheima.webhomework.repository.Impl.RedisChatMemoryRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class CommonConfiguration {

    /**
     * 会话记忆最大消息数（从 application.yml 读取，默认 10）
     *
     * 工程化思维体现：
     * - 不硬编码，通过配置文件灵活调整
     * - 可根据不同业务场景调整（客服场景可设为 20，简单问答可设为 5）
     * - 控制 Token 消耗，避免对话过长导致 API 成本飙升或超出上下文窗口
     */
    @Value("${ai.chat.max-messages:10}")
    private int maxMessages;

    /**
     * 注册 Redis 版会话记忆仓储（替代默认内存版）
     *
     * 【架构分层设计】
     * Spring AI 1.1.x 将"存储"和"裁剪"拆成了两层：
     * - ChatMemoryRepository：纯存储层，只负责读写全量消息，不做任何裁剪
     * - MessageWindowChatMemory：业务层，包装 Repository，负责按 maxMessages 滑动窗口裁剪
     *
     * 本方法注册的是存储层，使用 Redis List 持久化对话记忆，解决两个核心问题：
     * 1. 持久化：服务重启后对话记忆不丢失（内存版 MessageWindowChatMemory 重启即丢）
     * 2. 分布式共享：多实例部署时共享同一份对话记忆（内存版各实例独立，上下文断裂）
     *
     * 【面试话术】
     * "我实现了 ChatMemoryRepository 接口，对接 Redis 做持久化存储。
     *  Spring AI 的分层设计很好，存储和裁剪解耦，我只需要实现存储层，
     *  裁剪逻辑由 MessageWindowChatMemory 自动处理，Controller 层代码完全不需要改动。"
     *
     * @param redisTemplate Spring 自动注入的 Redis 操作模板
     * @return Redis 版会话记忆仓储实例
     */
    @Bean
    public ChatMemoryRepository chatMemoryRepository(StringRedisTemplate stringRedisTemplate) {
        return new RedisChatMemoryRepository(stringRedisTemplate);
    }

    /**
     * 注册 ChatMemory Bean（使用 Redis 持久化 + 滑动窗口裁剪）
     *
     * MessageWindowChatMemory 的工作原理：
     * 1. 通过 ChatMemoryRepository 从 Redis 读取该会话的全量历史消息
     * 2. 将新消息追加到历史列表
     * 3. 按 maxMessages 裁剪，只保留最近 N 条（滑动窗口）
     * 4. 将裁剪后的完整列表通过 saveAll 写回 Redis（覆盖写入）
     *
     * @param chatMemoryRepository Redis 版仓储（上一步注册的 Bean）
     * @return ChatMemory 实例
     */
    @Bean
    public ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)  // 注入 Redis 版仓储（替代默认内存版）
                .maxMessages(maxMessages)                     // 滑动窗口大小，从配置文件读取
                .build();
    }

    /**
     * 注册 ChatClient Bean
     *
     * 注意参数中的 model 就是使用的模型，这里用的是通义千问（OpenAI 兼容模式）
     *
     * @param model            Spring AI 自动注入的 ChatModel（通义千问）
     * @param chatMemory       Redis 版 ChatMemory（上一步注册的 Bean）
     * @param aiAssistantTools AI 工具集（Function Calling）
     * @return ChatClient 实例
     */
    @Bean
    public ChatClient chatClient(ChatModel model, ChatMemory chatMemory, AiAssistantTools aiAssistantTools) {
        return ChatClient
                .builder(model) // 创建ChatClient工厂
                .defaultSystem("""
                    你是一名专业的学生管理AI助手，负责帮助老师和学生查询学生信息、成绩数据等。
                    
                    【你的职责】
                    1. 当用户询问学生信息时，使用 queryStudentByName 工具查询
                    2. 当用户询问成绩时，使用 queryStudentScoreById 工具查询
                    3. 当用户要求查看排名时，使用 queryAllStudentScoresRanking 工具
                    4. 当用户查询班级学生时，使用 queryStudentsByClass 工具
                    5. 当用户查询不及格学生时，使用 queryFailedStudents 工具
                    
                    【回答规范】
                    - 优先使用工具获取真实数据，不要编造信息
                    - 如果工具返回"未找到"，友好地告知用户
                    - 保持专业、友善的语气
                    - 对于敏感信息（如具体分数），注意隐私保护
                    """)
                .defaultAdvisors(new SimpleLoggerAdvisor(),
                                 MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .defaultTools(aiAssistantTools)
                .build(); // 构建ChatClient实例

    }
}