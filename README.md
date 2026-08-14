# 客服中心后端 — kefu-center-server

## 技术栈

| 层面 | 选择 |
|------|------|
| 框架 | Spring Boot 2.7.18 |
| 语言 | Java 8 |
| 构建 | Maven |
| 数据库 | MySQL 8.x |
| ORM | MyBatis-Plus 3.5.5 |
| API 文档 | Knife4j（Swagger）|

## 项目结构

```
src/main/java/com/smartlink/
├── SmartlinkApplication.java       # 启动类
├── config/
│   ├── WebMvcConfig.java           # CORS、静态资源
│   ├── MyBatisPlusConfig.java      # MyBatis-Plus 分页插件
│   └── AiOpenProperties.java       # 火山方舟标准端配置
├── controller/
│   ├── SessionController.java      # M1 工作表 API
│   ├── ReportController.java       # M2 报表 API
│   ├── KnowledgeController.java    # M3 知识库 API
│   └── AiChatController.java       # AI 对话转发（→ 火山方舟标准端）
├── service/
│   ├── SessionService.java         # M1 业务逻辑
│   ├── ReportService.java          # M2 业务逻辑
│   ├── KnowledgeService.java       # M3 业务逻辑
│   └── AiProxyService.java         # AI 代理转发服务
├── service/impl/                   # 实现类
├── mapper/                         # MyBatis-Plus Mapper
├── entity/                         # 数据库实体
├── dto/
│   ├── request/                    # 请求 DTO
│   └── response/                   # 响应 VO
├── common/                         # 统一返回 Result、PageResult
└── sql/
    ├── schema.sql                  # 建表脚本
    └── seed.sql                    # 测试数据
```

## 快速启动

```bash
# 1. 建库建表
mysql -u root -p < sql/schema.sql
# 数据库名：kefu_center

# 2. 导入测试数据（可选）
mysql -u root -p kefu_center < sql/seed.sql

# 3. 修改数据库密码
# 编辑 src/main/resources/application.yml
# spring.datasource.password: 你的密码

# 4. 启动
mvn spring-boot:run
# → http://localhost:8080
# → API文档：http://localhost:8080/doc.html
```

## 数据库（kefu_center）— 7 张表

| 表 | 用途 | 模块 |
|------|------|:--:|
| sessions | 工作表记录（AI自动填充） | M1 |
| reports | 日报/周报/月报 | M2 |
| feedback | 用户反馈 | M2 |
| documents | 知识库文档（含版本管理） | M3 |
| qa_pairs | QA问答对（含同步标记） | M3 |
| reviews | 审核记录 | M3 |
| iterations | 迭代优化记录 | M3 |

## API 接口

### M1 工作表
- `GET /api/sessions` — 列表（?keyword=&workRecordType=）
- `GET /api/sessions/{id}` — 详情
- `PUT /api/sessions/{id}` — 更新 ICCID、确认

### M2 报表
- `GET /api/reports` — 列表
- `GET /api/reports/{id}` — 详情
- `GET/PUT /api/feedback` — 反馈

### M3 知识库
- `GET/POST /api/documents` — 文档
- `GET/POST/PUT/DELETE /api/qa` — QA CRUD
- `GET/PUT /api/reviews` — 审核
- `GET /api/iterations` — 迭代
- `GET /api/analytics` — 看板

### AI 对话
- `POST /api/ai/chat` — 流式对话（转发到火山方舟标准端）

## AI 接入

AI 能力直连火山方舟标准端（OpenAI 兼容接口），不自己部署大模型。

配置：`application.yml` → `ai.open`
- `chat-url`: 火山方舟标准端对话地址（已配）
- `api-key`: 通过环境变量 `ARK_API_KEY` 注入（ark-xxx 火山方舟标准端 Key）
- `model`: 推理接入点，默认 `glm-5-2-260617`

数据流：前端 → kefu-server → 火山方舟标准端

## 前端仓库

https://github.com/chuanyu001/kefu-center-web
