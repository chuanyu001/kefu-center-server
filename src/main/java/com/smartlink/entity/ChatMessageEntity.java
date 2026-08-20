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
    /** 消息附加数据（JSON文本，如内容设计会话生成的内容块快照） */
    private String payload;
    private LocalDateTime createdAt;
}
