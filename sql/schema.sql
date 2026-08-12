CREATE DATABASE IF NOT EXISTS kefu_center DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE kefu_center;

CREATE TABLE IF NOT EXISTS sessions (
    id              VARCHAR(64)     PRIMARY KEY             COMMENT '主键ID',
    customer_name   VARCHAR(128)    DEFAULT ''              COMMENT '客户名称',
    customer_phone  VARCHAR(32)     DEFAULT ''              COMMENT '客户电话',
    vin             VARCHAR(64)     DEFAULT ''              COMMENT '车辆VIN码',
    work_record_type VARCHAR(32)    DEFAULT ''              COMMENT '工作表类型',
    session_time    VARCHAR(64)     DEFAULT ''              COMMENT '会话时间',
    agent_name      VARCHAR(64)     DEFAULT ''              COMMENT '受理客服',
    iccid           VARCHAR(32)     DEFAULT ''              COMMENT 'ICCID',
    car_model       VARCHAR(128)    DEFAULT ''              COMMENT '车型',
    fuel_type       VARCHAR(32)     DEFAULT ''              COMMENT '燃料类型',
    terminal_number VARCHAR(64)     DEFAULT ''              COMMENT 'T-Box终端号',
    sim_card        VARCHAR(32)     DEFAULT ''              COMMENT 'SIM卡号',
    manufacturer    VARCHAR(64)     DEFAULT ''              COMMENT '厂家',
    recorder_model  VARCHAR(64)     DEFAULT ''              COMMENT '记录仪型号',
    recorder_device_id VARCHAR(64)  DEFAULT ''              COMMENT '记录仪设备ID',
    antenna_position VARCHAR(128)   DEFAULT ''              COMMENT '天线位置',
    no_position_reason VARCHAR(256) DEFAULT ''              COMMENT '不定位原因',
    no_position_issue VARCHAR(256)  DEFAULT ''              COMMENT '未定位问题现象',
    antenna_damaged VARCHAR(16)     DEFAULT ''              COMMENT '天线是否损坏',
    qiyu_ticket_status INT          DEFAULT NULL            COMMENT '七鱼工单状态',
    qiyu_ticket_category VARCHAR(64) DEFAULT ''             COMMENT '七鱼工单分类',
    consultation_scenario VARCHAR(128) DEFAULT ''           COMMENT '咨询场景',
    problem_type    VARCHAR(64)     DEFAULT ''              COMMENT '问题类型',
    temporary_solution TEXT                                 COMMENT '临时解决措施',
    special_notes   TEXT                                    COMMENT '特殊备注',
    chat_messages   TEXT                                    COMMENT '原始会话记录',
    modification_history TEXT                               COMMENT '人工修改记录',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作表';
