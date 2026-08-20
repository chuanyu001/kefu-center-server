-- AI 智能助手会话记录
CREATE TABLE IF NOT EXISTS chat_sessions (
    id          VARCHAR(64) NOT NULL COMMENT '会话ID',
    title       VARCHAR(128) NOT NULL DEFAULT '' COMMENT '会话标题（首条消息截断）',
    kind        VARCHAR(16) NOT NULL DEFAULT 'chat' COMMENT '会话类型：chat-智能问答/designer-内容设计',
    created_by  VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建人',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后消息时间',
    PRIMARY KEY (id),
    INDEX idx_created_by (created_by, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI助手会话';

CREATE TABLE IF NOT EXISTS chat_messages (
    id          VARCHAR(64) NOT NULL COMMENT '消息ID',
    session_id  VARCHAR(64) NOT NULL COMMENT '会话ID',
    role        VARCHAR(16) NOT NULL COMMENT 'user/assistant',
    content     TEXT COMMENT '消息内容',
    payload     TEXT COMMENT '消息附加数据(JSON，如设计器生成的views)',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    PRIMARY KEY (id),
    INDEX idx_session (session_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI助手会话消息';
