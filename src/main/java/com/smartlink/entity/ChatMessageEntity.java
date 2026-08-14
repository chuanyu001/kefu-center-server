package com.smartlink.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_messages")
public class ChatMessageEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String sessionId;
    /** user / assistant */
    private String role;
    private String content;
    private LocalDateTime createdAt;
}
