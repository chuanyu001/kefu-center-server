package com.smartlink.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_sessions")
public class ChatSessionEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String title;
    /** chat-智能问答 / designer-内容设计 */
    private String kind;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
