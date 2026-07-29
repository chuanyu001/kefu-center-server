-- =============================================
-- 鱼快创领 客服中心产品自动化平台 - 数据库初始化脚本
-- 数据库: kefu_zhongxin
-- 引擎: InnoDB, 字符集: utf8mb4
-- =============================================

CREATE DATABASE IF NOT EXISTS kefu_zhongxin DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE kefu_zhongxin;

-- ----------------------------
-- 1. 工作表 (sessions)
-- ----------------------------
CREATE TABLE IF NOT EXISTS sessions (
    id              VARCHAR(64)     PRIMARY KEY             COMMENT '主键ID',
    customer_name   VARCHAR(128)    DEFAULT ''              COMMENT '客户名称',
    customer_phone  VARCHAR(32)     DEFAULT ''              COMMENT '客户电话',
    vin             VARCHAR(64)     DEFAULT ''              COMMENT '车辆VIN码',
    work_record_type VARCHAR(32)    DEFAULT ''              COMMENT '工单类型',
    export_status   VARCHAR(32)     DEFAULT 'PENDING'       COMMENT '导出状态',
    session_time    VARCHAR(64)     DEFAULT ''              COMMENT '会话时间',
    agent_name      VARCHAR(64)     DEFAULT ''              COMMENT '坐席名称',
    fill_status     VARCHAR(32)     DEFAULT ''              COMMENT '填写状态',
    ai_confidence   DECIMAL(5,4)    DEFAULT NULL            COMMENT 'AI置信度',
    form_data       TEXT                                    COMMENT '表单数据(JSON)',
    messages        TEXT                                    COMMENT '会话消息(JSON)',
    modification_history TEXT                               COMMENT '修改历史(JSON)',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作表';

-- ----------------------------
-- 2. 报告 (reports)
-- ----------------------------
CREATE TABLE IF NOT EXISTS reports (
    id              VARCHAR(64)     PRIMARY KEY             COMMENT '主键ID',
    customer_name   VARCHAR(128)    DEFAULT ''              COMMENT '客户名称',
    report_type     VARCHAR(64)     DEFAULT ''              COMMENT '报告类型',
    date_start      DATE            DEFAULT NULL            COMMENT '统计开始日期',
    date_end        DATE            DEFAULT NULL            COMMENT '统计结束日期',
    generated_time  DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
    push_status     VARCHAR(32)     DEFAULT 'NOT_PUSHED'    COMMENT '推送状态',
    summary_text    TEXT                                    COMMENT '摘要文本',
    anomaly_notes   TEXT                                    COMMENT '异常备注',
    chart_data      TEXT                                    COMMENT '图表数据(JSON)',
    table_data      TEXT                                    COMMENT '表格数据(JSON)',
    push_script     VARCHAR(512)    DEFAULT ''              COMMENT '推送话术'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报告';

-- ----------------------------
-- 3. 反馈 (feedback)
-- ----------------------------
CREATE TABLE IF NOT EXISTS feedback (
    id              VARCHAR(64)     PRIMARY KEY             COMMENT '主键ID',
    customer_name   VARCHAR(128)    DEFAULT ''              COMMENT '客户名称',
    report_id       VARCHAR(64)     DEFAULT ''              COMMENT '关联报告ID',
    content         TEXT                                    COMMENT '反馈内容',
    feedback_time   DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '反馈时间',
    status          VARCHAR(32)     DEFAULT 'PENDING'       COMMENT '状态',
    remark          VARCHAR(512)    DEFAULT ''              COMMENT '备注'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='反馈';

-- ----------------------------
-- 4. 文档 (documents)
-- ----------------------------
CREATE TABLE IF NOT EXISTS documents (
    id              VARCHAR(64)     PRIMARY KEY             COMMENT '主键ID',
    title           VARCHAR(256)    DEFAULT ''              COMMENT '文档标题',
    format          VARCHAR(32)     DEFAULT ''              COMMENT '文档格式',
    category        VARCHAR(64)     DEFAULT ''              COMMENT '分类',
    subcategory     VARCHAR(64)     DEFAULT ''              COMMENT '子分类',
    file_size       BIGINT          DEFAULT 0               COMMENT '文件大小(字节)',
    upload_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    parse_status    VARCHAR(32)     DEFAULT 'PENDING'       COMMENT '解析状态',
    content         TEXT                                    COMMENT '文档内容'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档';

-- ----------------------------
-- 5. 问答对 (qa_pairs)
-- ----------------------------
CREATE TABLE IF NOT EXISTS qa_pairs (
    id              VARCHAR(64)     PRIMARY KEY             COMMENT '主键ID',
    question        TEXT                                    COMMENT '问题',
    answer          TEXT                                    COMMENT '答案',
    answer_brief    VARCHAR(512)    DEFAULT ''              COMMENT '答案摘要',
    source_doc_id   VARCHAR(64)     DEFAULT ''              COMMENT '来源文档ID',
    category        VARCHAR(64)     DEFAULT ''              COMMENT '分类',
    tags            TEXT                                    COMMENT '标签(JSON)',
    status          VARCHAR(32)     DEFAULT 'DRAFT'         COMMENT '状态',
    confidence      DECIMAL(5,4)    DEFAULT NULL            COMMENT '置信度',
    multi_dimensional TEXT                                 COMMENT '多维数据(JSON)',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='问答对';

-- ----------------------------
-- 6. 审核 (reviews)
-- ----------------------------
CREATE TABLE IF NOT EXISTS reviews (
    id              VARCHAR(64)     PRIMARY KEY             COMMENT '主键ID',
    type            VARCHAR(32)     DEFAULT ''              COMMENT '审核类型',
    target_id       VARCHAR(64)     DEFAULT ''              COMMENT '审核目标ID',
    title           VARCHAR(256)    DEFAULT ''              COMMENT '标题',
    submitter       VARCHAR(64)     DEFAULT ''              COMMENT '提交人',
    submitted_at    DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    status          VARCHAR(32)     DEFAULT 'PENDING'       COMMENT '审核状态',
    reviewer        VARCHAR(64)     DEFAULT ''              COMMENT '审核人',
    review_comment  TEXT                                    COMMENT '审核意见'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审核';

-- ----------------------------
-- 7. 迭代记录 (iterations)
-- ----------------------------
CREATE TABLE IF NOT EXISTS iterations (
    id                  VARCHAR(64)     PRIMARY KEY         COMMENT '主键ID',
    source_session_id   VARCHAR(64)     DEFAULT ''          COMMENT '来源会话ID',
    session_summary     TEXT                                COMMENT '会话摘要',
    extracted_question  TEXT                                COMMENT '提取问题',
    extracted_answer    TEXT                                COMMENT '提取答案',
    existing_qa_id      VARCHAR(64)     DEFAULT ''          COMMENT '已有问答ID',
    comparison          TEXT                                COMMENT '对比结果(JSON)',
    created_at          DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    status              VARCHAR(32)     DEFAULT 'PENDING'   COMMENT '状态'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='迭代记录';
