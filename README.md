# 客服中心后端 — kefu-center-server

> 项目全景与交接说明见仓库上级目录 `README.md`，本文件描述后端自身。

## 技术栈

| 层面 | 选择 |
|------|------|
| 框架 | Spring Boot 2.7.18 |
| 语言 | Java 8 |
| 构建 | Maven |
| 数据库 | MySQL 8.x（库 `kefu_center`） |
| ORM | MyBatis-Plus 3.5.5 |
| API 文档 | Knife4j（http://localhost:8080/doc.html） |

## 项目结构

```
src/main/java/com/smartlink/
├── SmartlinkApplication.java       # 启动类（@MapperScan）
├── config/                         # MybatisPlusConfig / AiOpenProperties / ReportExportProperties
├── controller/
│   ├── SessionController.java      # 工作表：列表/详情/更新/导入/导出/补全/analytics 聚合
│   ├── AiChatController.java       # AI 对话（NDJSON 流式，转发火山方舟）
│   ├── ChatSessionController.java  # 会话记录（kind 过滤、消息 payload）
│   ├── CustomViewController.java   # AI 内容块存储（全字段编辑）
│   ├── DocumentController.java     # 知识库文档（列表/详情/更新）
│   └── ReportExportController.java # 报表截图导出
├── service/
│   ├── SessionService(Impl)        # 工单业务逻辑
│   ├── AiProxyService.java         # 火山方舟转发 + RAG 知识注入
│   ├── KnowledgeService.java       # 文档分块检索（ngram 全文 + LIKE 兜底）
│   ├── ReportExportService.java    # Node+Playwright 截图
│   ├── VehicleInfoService.java     # 车辆补全（ID/SIM 真实 API，其余 mock 占位）
│   └── ImportFileService.java      # Excel/Word/PDF 识别导入
├── util/                           # OutServiceUtil（运营平台 API）、ExcelService
├── mapper/ / entity/ / common/     # Mapper、实体、Result/PageResult
└── sql/                            # 建表 + 幂等迁移脚本
```

## 快速启动

```bash
# 1. 建库建表（sql/ 目录：schema.sql + create_*.sql + migrate_*.sql 幂等迁移）
mysql -u root -p < sql/schema.sql
mysql -u root -p kefu_center < sql/create_chat_sessions.sql
mysql -u root -p kefu_center < sql/create_custom_views.sql
# 已存在的库按需执行 migrate_*.sql（information_schema 判断，可重复执行）

# 2. 环境变量
# DB_PASSWORD：MySQL root 密码（必填，否则连不上库）
# ARK_API_KEY：火山方舟 key（AI 功能必填）
# REPORT_EXPORT_NODE_PATH / VEHICLE_INFO_MODE 等按需覆盖

# 3. 编译启动
mvn compile
# VSCode Java 扩展 Run，或：
# java -cp target/classes;<maven依赖> com.smartlink.SmartlinkApplication
# → http://localhost:8080
```

## API 端点

### 工作表 `/api/sessions`
- `GET /api/sessions` — 分页列表（?keyword=&workRecordType=）
- `GET /api/sessions/{id}` — 详情
- `PUT /api/sessions/{id}` — 更新（记录修改历史）
- `POST /api/sessions/import-file` — 导入 xlsx/xls/docx/doc/pdf（`import-excel` 为兼容别名）
- `POST /api/sessions/export-excel` — 按 ids+columns 导出
- `POST /api/sessions/sync-vehicle` — 批量补全车辆信息
- `GET /api/sessions/analytics?range=all|30d|month` — 8 维聚合（AI 内容设计的真实数据源）

### AI `/api/ai/chat`
- `POST /api/ai/chat` — NDJSON 流式：`t:c` 内容增量 / `t:r` 思考 / `t:done` / `t:ping` 心跳 / `t:e` 错误；请求前经 RAG 注入知识库检索结果

### 会话 `/api/chat-sessions`
- `GET /api/chat-sessions?createdBy=&kind=` — 会话列表（kind: chat/designer，不传则全部）
- `POST /api/chat-sessions` — 创建 `{title?, kind, createdBy}`
- `GET /api/chat-sessions/{id}/messages` — 消息（含 payload）
- `POST /api/chat-sessions/{id}/messages` — 追加 `{role, content, payload?}`（首条用户消息前 20 字作标题）
- `DELETE /api/chat-sessions/{id}` — 删除（级联删消息）

### 内容块 `/api/custom-views`
- `GET /api/custom-views` — 全部列表
- `POST /api/custom-views` — 批量保存 `{pageKey, sectionKey, views[], createdBy}`
- `PUT /api/custom-views/{id}` — 局部更新（白名单：title/dataRule/type/labels/data/columns/value/subtitle/content/items/tone/width/sourceRef/src/caption；统一 LambdaUpdateWrapper 显式 set，dataRule 可置 null）
- `DELETE /api/custom-views/{id}` — 删除

### 文档 `/api/documents`
- `GET /api/documents?platform=` — 列表
- `GET /api/documents/{id}` — 详情
- `PUT /api/documents/{id}` — 更新（version+1）

### 报表 `/api/reports`
- `POST /api/reports/export-image` — 传 `{url, title}`，Node+Playwright 截图返回 Base64

## 外部集成（application.yml）

| 配置 | 说明 |
|---|---|
| `ai.open` | 火山方舟标准端（`ARK_API_KEY` 注入；model 必须用接入点 ID `glm-5-2-260617`） |
| `report.export` | node-path / script-path / work-dir 指向前端仓库的 scripts/report-export.cjs（⚠️ 路径硬编码 `D:/kefu_center_web/...`，换机器需改） |
| `vehicle-info` | mode=api 时 ID号/SIM号调运营平台真实 API；车型/燃料/厂家/记录仪型号为 mock 占位（detail-url 预留待接） |
| `spring.datasource` | `DB_USERNAME`/`DB_PASSWORD` 环境变量 |

## 数据库表

| 表 | 状态 |
|---|---|
| sessions | ✓ 有代码（schema.sql） |
| chat_sessions / chat_messages | ✓ 有代码（create_chat_sessions.sql + payload 迁移） |
| custom_views | ✓ 有代码（create_custom_views.sql + data_rule 迁移） |
| documents / document_chunks | ✓ 有代码（⚠️ 无建表 DDL，需从现有库导出） |
| qa_pairs / reviews / iterations / feedback / reports | ✗ 有表无代码（待接，前端已有 mock 壳） |

## 前端仓库

https://github.com/chuanyu001/kefu-center-web
