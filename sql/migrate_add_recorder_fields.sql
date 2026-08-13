ALTER TABLE sessions ADD COLUMN recorder_device_id VARCHAR(64) DEFAULT '' COMMENT '记录仪设备ID' AFTER recorder_model;
ALTER TABLE sessions ADD COLUMN antenna_position VARCHAR(128) DEFAULT '' COMMENT '天线位置' AFTER recorder_device_id;
ALTER TABLE sessions ADD COLUMN no_position_reason VARCHAR(256) DEFAULT '' COMMENT '不定位原因' AFTER antenna_position;
ALTER TABLE sessions ADD COLUMN no_position_issue VARCHAR(256) DEFAULT '' COMMENT '未定位问题现象' AFTER no_position_reason;
ALTER TABLE sessions ADD COLUMN antenna_damaged VARCHAR(16) DEFAULT '' COMMENT '天线是否损坏' AFTER no_position_issue;
ALTER TABLE sessions ADD COLUMN qiyu_ticket_status INT DEFAULT NULL COMMENT '七鱼工单状态' AFTER antenna_damaged;
ALTER TABLE sessions ADD COLUMN qiyu_ticket_category VARCHAR(64) DEFAULT '' COMMENT '七鱼工单分类' AFTER qiyu_ticket_status;
