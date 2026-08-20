-- 客服中心知识库：文档来源分类与在线文档种子数据
-- 1) documents 表新增 platform（来源平台：dingtalk/tencent）与 url（在线文档链接）两列
-- 2) 插入记录仪/车队相关的腾讯文档、金山文档（在线文档），供“腾讯文档管理”展示
USE kefu_center;

-- 幂等：仅当列不存在时才新增
SET @col_platform := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'kefu_center' AND TABLE_NAME = 'documents' AND COLUMN_NAME = 'platform');
SET @col_url := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'kefu_center' AND TABLE_NAME = 'documents' AND COLUMN_NAME = 'url');

SET @sql_platform := IF(@col_platform = 0,
    'ALTER TABLE documents ADD COLUMN platform VARCHAR(32) NOT NULL DEFAULT ''dingtalk'' COMMENT ''文档来源平台: dingtalk/tencent''',
    'SELECT 1');
PREPARE stmt FROM @sql_platform; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql_url := IF(@col_url = 0,
    'ALTER TABLE documents ADD COLUMN url VARCHAR(1024) DEFAULT '''' COMMENT ''在线文档链接''',
    'SELECT 1');
PREPARE stmt FROM @sql_url; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 种子数据：在线文档（platform=tencent），category 记录具体平台（腾讯文档/金山文档）
INSERT INTO documents
    (id, title, format, category, subcategory, file_size, upload_time, parse_status, version, updated_by, content, platform, url)
VALUES
    ('DOC-TX-001', '行驶记录仪算法升级需求', 'link', '腾讯文档', '', 0, NOW(), 'PARSED', 1, '', '', 'tencent', 'https://docs.qq.com/sheet/DZkJ6UFl1eFVHWEx0?tab=000001'),
    ('DOC-TX-002', '11-T盒-设备健康度检查流程20220818-李清雯', 'link', '金山文档', '', 0, NOW(), 'PARSED', 1, '', '', 'tencent', 'https://www.kdocs.cn/l/cp7VDIfHOESN'),
    ('DOC-TX-003', '24-26年车队开通车辆明细', 'link', '金山文档', '', 0, NOW(), 'PARSED', 1, '', '', 'tencent', 'https://www.kdocs.cn/l/cb2428ImV0zn'),
    ('DOC-TX-004', '车辆登记信息表汇总', 'link', '金山文档', '', 0, NOW(), 'PARSED', 1, '', '', 'tencent', 'https://www.kdocs.cn/l/clYHf77V0TRe'),
    ('DOC-TX-005', '群报表服务开通-2024-01-08更新', 'link', '金山文档', '', 0, NOW(), 'PARSED', 1, '', '', 'tencent', 'https://www.kdocs.cn/l/clelHGHcTYh8'),
    ('DOC-TX-006', '12-主动安全-设备健康度检查流程20220823版本', 'link', '金山文档', '', 0, NOW(), 'PARSED', 1, '', '', 'tencent', 'https://www.kdocs.cn/l/cb7Qtf6hgkQm'),
    ('DOC-TX-007', '安全外呼-主动安全服务客户清单新', 'link', '金山文档', '', 0, NOW(), 'PARSED', 1, '', '', 'tencent', 'https://www.kdocs.cn/l/cazDVnlLwhZA'),
    ('DOC-TX-008', '基础流量充值登记表格', 'link', '金山文档', '', 0, NOW(), 'PARSED', 1, '', '', 'tencent', 'https://www.kdocs.cn/l/cjT0Dz9VwfqY'),
    ('DOC-TX-009', 'T盒流量充值登记表-车队', 'link', '金山文档', '', 0, NOW(), 'PARSED', 1, '', '', 'tencent', 'https://www.kdocs.cn/l/cbzKipmoswtQ'),
    ('DOC-TX-010', '记录仪流量充值登记表-车队', 'link', '金山文档', '', 0, NOW(), 'PARSED', 1, '', '', 'tencent', 'https://www.kdocs.cn/l/clK7UO4ZOgYx'),
    ('DOC-TX-011', '记录仪车机内部知识库', 'link', '腾讯文档', '', 0, NOW(), 'PARSED', 1, '', '', 'tencent', 'https://docs.qq.com/aio/DYm9vbk5EdWViTmx0')
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    format = VALUES(format),
    category = VALUES(category),
    url = VALUES(url),
    platform = VALUES(platform);
