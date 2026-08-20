-- 客服中心 AI 自定义视图：补齐 data_rule 列
-- custom_views 表新增 data_rule（数据生成规则 JSON:{dimension,range}），
-- 与 CustomViewEntity.dataRule / CustomViewController 读写字段对齐（修复建表 DDL 缺列问题）
USE kefu_center;

-- 幂等：仅当列不存在时才新增
SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'kefu_center' AND TABLE_NAME = 'custom_views' AND COLUMN_NAME = 'data_rule');
SET @sql := IF(@col = 0,
    'ALTER TABLE custom_views ADD COLUMN data_rule TEXT NULL COMMENT ''数据生成规则(JSON:{dimension,range})'' AFTER title',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
