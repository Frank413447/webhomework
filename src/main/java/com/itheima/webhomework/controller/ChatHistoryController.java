package com.itheima.webhomework.controller;


import com.itheima.webhomework.entity.ChatHistoryEntity;
import com.itheima.webhomework.vo.MessageVO;
import com.itheima.webhomework.repository.ChatHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 聊天历史控制器
 * 提供会话历史查询功能
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/ai/history")
public class ChatHistoryController {

    private final ChatHistoryRepository chatHistoryRepository;

    /**
     * 查询会话历史列表
     *
     * GET /ai/history/chat
     * @return 会话ID列表，按最新时间排序
     */
    @GetMapping("/chat")
    public List<String> getChatIds() {
        return chatHistoryRepository.getChatIds("chat");
    }

    /**
     * 根据会话ID查询完整历史消息
     *
     * GET /ai/history/chat/{chatId}
     * @param chatId 会话ID
     * @return 历史消息列表，按时间升序
     */
    @GetMapping("/chat/{chatId}")
    public List<MessageVO> getChatHistory(@PathVariable("chatId") String chatId) {
        List<ChatHistoryEntity> entities = chatHistoryRepository.getMessages("chat", chatId);

        if (entities == null || entities.isEmpty()) {
            return List.of();
        }

        return entities.stream()
                .map(entity -> new MessageVO(entity.getRole(), entity.getContent()))
                .collect(Collectors.toList());
    }

    /**
     * 分页查询会话历史消息
     *
     * GET /ai/history/chat/{chatId}/page?pageNum=1&pageSize=20
     * @param chatId 会话ID
     * @param pageNum 页码（从1开始，默认1）
     * @param pageSize 每页数量（默认20，最大100）
     * @return 分页后的历史消息列表
     */
    @GetMapping("/chat/{chatId}/page")
    public List<MessageVO> getChatHistoryWithPage(@PathVariable("chatId") String chatId,
                                                   @RequestParam(defaultValue = "1") int pageNum,
                                                   @RequestParam(defaultValue = "20") int pageSize) {

        pageSize = Math.min(pageSize, 100);

        List<ChatHistoryEntity> entities = chatHistoryRepository.getMessagesWithPage("chat", chatId, pageNum, pageSize);

        if (entities == null || entities.isEmpty()) {
            return List.of();
        }

        return entities.stream()
                .map(entity -> new MessageVO(entity.getRole(), entity.getContent()))
                .collect(Collectors.toList());
    }

    /**
     * 删除某个会话的所有历史记录
     *
     * DELETE /ai/history/chat/{chatId}
     * @param chatId 会话ID
     * @return 操作结果提示
     */
    @DeleteMapping("/chat/{chatId}")
    public String deleteChatHistory(@PathVariable("chatId") String chatId) {
        chatHistoryRepository.deleteMessages("chat", chatId);
        return "删除成功";
    }
}
