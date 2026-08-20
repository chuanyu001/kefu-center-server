-- 客服中心 AI 会话消息：新增 payload 列
-- chat_messages 表新增 payload（消息附加数据 JSON），用于内容设计会话存生成的内容块快照
USE kefu_center;

-- 幂等：仅当列不存在时才新增
SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'kefu_center' AND TABLE_NAME = 'chat_messages' AND COLUMN_NAME = 'payload');
SET @sql := IF(@col = 0,
    'ALTER TABLE chat_messages ADD COLUMN payload TEXT NULL COMMENT ''消息附加数据(JSON，如设计器生成的views)'' AFTER content',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
