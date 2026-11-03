package com.itheima.webhomework.entity;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatHistoryEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String type;
    private String chatId;
    private String role;
    private String content;
    private LocalDateTime createTime;
}
