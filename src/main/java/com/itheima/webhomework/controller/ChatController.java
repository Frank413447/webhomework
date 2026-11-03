package com.itheima.webhomework.controller;

import com.itheima.webhomework.repository.ChatHistoryRepository;  // 导入聊天历史仓储接口
import lombok.RequiredArgsConstructor;                              // Lombok 注解，生成构造函数
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;              // Spring AI 聊天客户端
import org.springframework.ai.chat.memory.ChatMemory;              // 聊天记忆接口
import org.springframework.web.bind.annotation.*;                   // Spring MVC 注解
import reactor.core.publisher.Flux;                                 // 响应式流（用于流式输出）

/**
 * AI 聊天控制器
 * 处理前端的聊天请求，调用 Spring AI 与大模型交互
 * 支持流式输出（逐字显示）和会话历史保存
 */
@Slf4j
@RequiredArgsConstructor
@RestController                  // Spring MVC：标记为 REST 控制器，返回 JSON 数据
@RequestMapping("/ai")           // 统一路径前缀：/ai
@CrossOrigin(origins = "*", maxAge = 3600)  // 允许跨域请求，预检请求缓存1小时
public class ChatController {

    private final ChatHistoryRepository chatHistoryRepository;  // 聊天历史仓储
    private final ChatClient chatClient;                        // Spring AI 聊天客户端

    /**
     * 聊天接口（流式输出）
     *
     * 工作流程：
     * 1. 接收用户消息和会话ID
     * 2. 保存会话ID到 Redis（维护会话列表）
     * 3. 保存用户消息到数据库和缓存
     * 4. 调用 AI 模型生成回复（流式输出）
     * 5. 累积 AI 回复内容
     * 6. 流式输出完成后，保存 AI 回复到数据库和缓存
     *
     * @param prompt 用户输入的提示词（默认值："讲个笑话"）
     * @param chatId 会话ID，前端生成，用于区分不同对话
     * @return Flux<String> 流式输出的 AI 回复内容
     */
    @RequestMapping(value = "/chat", produces = "text/html;charset=UTF-8")
    public Flux<String> chat(@RequestParam(defaultValue = "讲个笑话") String prompt,
                             String chatId) {

        log.info("收到聊天请求 - chatId: {}, prompt: {}", chatId, prompt);
        // 第1步：保存会话ID（如果是新会话，会添加到 Redis Set）
        chatHistoryRepository.save("chat", chatId);

        // 第2步：保存用户消息到数据库和缓存
        chatHistoryRepository.saveMessage("chat", chatId, "user", prompt);

        // 第3步：创建 StringBuilder 用于累积 AI 的完整回复
        StringBuilder assistantResponse = new StringBuilder();

        // 第4步：调用 Spring AI 聊天客户端，生成流式响应
        return chatClient
                .prompt(prompt)                                              // 设置用户提示词
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))  // 启用对话记忆（多轮对话）
                .stream()                                                    // 启用流式输出
                .content()                                                   // 提取文本内容
                .doOnNext(chunk -> {                                         // 每收到一个数据块时执行
                    assistantResponse.append(chunk);                         // 累积到 StringBuilder
                })
                .doOnComplete(() -> {
                    if (assistantResponse.length() > 0) {
                        String response = assistantResponse.toString();
                        log.info("AI回复完成 - chatId: {}, 长度: {}", chatId, response.length());
                        chatHistoryRepository.saveMessage("chat", chatId, "assistant", response);
                    }
                })
                .doOnError(error -> {
                    log.error("聊天出错 - chatId: {}, error: {}", chatId, error.getMessage());
                });
    }
}
