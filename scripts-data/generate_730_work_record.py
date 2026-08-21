# -*- coding: utf-8 -*-
from zipfile import ZipFile, ZIP_DEFLATED
from pathlib import Path
from xml.sax.saxutils import escape

out = Path(r"D:\kefu_center_web\7.30工作记录.docx")

sections = [
    ("7.30 工作记录", []),
    ("一、基础资料保存与长期记忆", [
        "已记录客服 AI 平台 / Copilot 会议修改建议，包括业务范围、数据源接入方案、核心功能模块、分阶段实施路径和后续待办事项。",
        "已读取并记录《客服中心知识库产品功能需求.docx》的核心需求，包括 M1 在线登记表自动填写、M2 日常报表自动生成与发送、M3 业务文档知识库及 QA 问答库。",
        "已建立并更新长期记忆索引，后续继续处理该项目时可直接引用这些背景信息。",
    ]),
    ("二、前端仓库拉取与运行", [
        "用户提供前端仓库：https://github.com/chuanyu001/kefu-center-web。",
        "已协助处理 Git 未安装、GitHub 私有仓库认证、CMD 闪退等环境问题。",
        r"用户完成私有仓库认证并克隆到 D:\kefu_center_web\kefu-center-web。",
        "已安装前端依赖：npm install --legacy-peer-deps。",
        "已成功启动 Vite 前端服务，访问地址为 http://localhost:3000。",
        "已确认前端技术栈为 Vue 3 + Element Plus + TypeScript + Vite + Pinia + Vue Router。",
    ]),
    ("三、完整交接压缩包保存与分析", [
        r"已找到用户提供的完整压缩包：D:\kefu_center_web\客服中心产品自动化.zip。",
        r"已复制备份到：D:\kefu_center_web\archive_backup\客服中心产品自动化-20260730-144402.zip。",
        r"已解压到：D:\kefu_center_web\客服中心产品自动化_extracted\客服中心产品自动化。",
        "已分析压缩包内容，包括前端 kefu-center-web、后端 kefu-center-server、AI Agent Core、SQL 脚本、需求文档和鱼快 AI 开放接口文档。",
        "已判断交接包当前属于第一阶段产品原型 + 前后端基础框架 + Mock 数据演示版。",
    ]),
    ("四、交接包完成度判断", [
        "M1 工作表自动填写：前端页面、后端 sessions 表、列表/详情/更新接口已完成；真实企微会话抽取、ICCID 自动补全、腾讯/金山在线表写入仍需继续实现或联调。",
        "M2 日常报表：报表页面、reports/feedback 表、查询/详情/反馈接口已完成；真实阿里 BI 数据接入、自动截图/拉数、企微推送、模板配置仍未完全打通。",
        "M3 知识库与 QA：文档、QA、审核、迭代、看板页面和后端接口已具备；真实文档解析、QA 自动生成、人机精调、版本更新和导入导出仍需继续实现。",
        "AI 助手：前端 AI 组件、后端 /api/ai/chat 代理接口、鱼快 AI 接口文档已有；真实 AI_OPEN_API_KEY 尚未配置，正式业务工具链仍需联调。",
    ]),
    ("五、后端运行环境安装", [
        r"已安装 Java 8 JDK：Amazon Corretto 1.8.0_502，路径为 C:\Program Files\Amazon Corretto\jdk1.8.0_502。",
        r"已下载并配置 Maven 3.9.11，路径为 D:\kefu_center_web\tools\apache-maven-3.9.11。",
        r"已安装 MySQL Community Server 8.4.9，路径为 C:\Program Files\MySQL\MySQL Server 8.4。",
        r"已初始化本地 MySQL 数据目录：D:\kefu_center_web\mysql-data。",
        "已启动 MySQL 3306，并设置 root 密码为项目默认配置。",
    ]),
    ("六、数据库初始化与后端启动", [
        "已导入后端 schema.sql 和 seed.sql。",
        "已创建数据库 kefu_center。",
        "已确认 7 张表创建成功：documents、feedback、iterations、qa_pairs、reports、reviews、sessions。",
        "已启动 Spring Boot 后端服务，访问地址为 http://localhost:8080。",
        "已验证后端接口文档 http://localhost:8080/doc.html 返回 200。",
        "已验证后端接口 http://127.0.0.1:8080/api/sessions 返回 200。",
    ]),
    ("七、前后端联调", [
        "已验证前端代理接口 http://127.0.0.1:3000/api/sessions 返回 200。",
        "已解决此前前端 /api/sessions 代理到 localhost:8080 时出现 ECONNREFUSED 的问题。",
        "目前前端 http://localhost:3000 与后端 http://localhost:8080 已可联通。",
    ]),
    ("八、一键启动脚本", [
        r"已创建一键启动脚本：D:\kefu_center_web\启动客服中心.bat。",
        "脚本会自动检查并启动 MySQL、后端 Spring Boot、前端 Vite。",
        "脚本会在启动完成后自动打开 http://localhost:3000。",
        "后续重启电脑后，双击该脚本即可恢复项目运行环境。",
    ]),
    ("九、当前仍需后续确认/推进事项", [
        "M1：腾讯文档/金山文档在线表地址、API 权限、字段映射和自动写入方式。",
        "M2：阿里 BI 是否有正式接口、企微群机器人地址、推送模板和人工确认流程。",
        "M3：人工审核流程、权限边界、人机精调样本池和文档解析/QA 生成链路。",
        "AI：需要同事提供真实 AI_OPEN_API_KEY，并确认最终 system prompt、用户身份、权限与留痕策略。",
    ]),
]

content_types = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
  <Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
</Types>'''

rels = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>'''

doc_rels = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"/>'''

styles = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:style w:type="paragraph" w:default="1" w:styleId="Normal"><w:name w:val="Normal"/><w:rPr><w:rFonts w:ascii="Microsoft YaHei" w:eastAsia="Microsoft YaHei"/><w:sz w:val="22"/></w:rPr></w:style>
  <w:style w:type="paragraph" w:styleId="Title"><w:name w:val="Title"/><w:basedOn w:val="Normal"/><w:pPr><w:jc w:val="center"/><w:spacing w:after="240"/></w:pPr><w:rPr><w:b/><w:rFonts w:ascii="Microsoft YaHei" w:eastAsia="Microsoft YaHei"/><w:sz w:val="36"/></w:rPr></w:style>
  <w:style w:type="paragraph" w:styleId="Heading1"><w:name w:val="heading 1"/><w:basedOn w:val="Normal"/><w:pPr><w:spacing w:before="240" w:after="120"/></w:pPr><w:rPr><w:b/><w:rFonts w:ascii="Microsoft YaHei" w:eastAsia="Microsoft YaHei"/><w:sz w:val="28"/></w:rPr></w:style>
</w:styles>'''

def para(text, style=None):
    style_xml = f'<w:pPr><w:pStyle w:val="{style}"/></w:pPr>' if style else ''
    return f'<w:p>{style_xml}<w:r><w:t xml:space="preserve">{escape(text)}</w:t></w:r></w:p>'

body = []
for i, (title, items) in enumerate(sections):
    body.append(para(title, "Title" if i == 0 else "Heading1"))
    for item in items:
        body.append(para("• " + item))

document = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:body>
''' + "\n".join(body) + '''
    <w:sectPr><w:pgSz w:w="11906" w:h="16838"/><w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440"/></w:sectPr>
  </w:body>
</w:document>'''

with ZipFile(out, "w", ZIP_DEFLATED) as z:
    z.writestr("[Content_Types].xml", content_types)
    z.writestr("_rels/.rels", rels)
    z.writestr("word/_rels/document.xml.rels", doc_rels)
    z.writestr("word/styles.xml", styles)
    z.writestr("word/document.xml", document)

print(out)
