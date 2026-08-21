# -*- coding: utf-8 -*-
"""
导入钉钉知识库到 kefu_center.documents 表（platform='dingtalk'）。

- 遍历 d:/kefu_center_web/钉钉知识库/ 下所有 .md 文件，每篇 → 一篇文档
- 目录映射（“按照目录保存”）：
    title       = 文档所在目录名（优先 _meta.json 的 name）
    category    = 一级目录（如“客服中心培训”“知识体系（FAQ&SOP）”…）
    subcategory = 中间目录路径（用 / 连接，不含一级目录和标题目录）
    url         = 同目录 _meta.json 的 docUrl（原始钉钉链接）
- 正文：图片链接替换为【图片：…】占位，md → HTML（去掉标题行/钉钉链接引用行）
- 幂等：先清空 platform='dingtalk' 再全量写入，重跑即重建
- DB 凭据从环境变量 DB_USERNAME / DB_PASSWORD 读取（运行前从 bat 加载）
"""
import os
import re
import json
import hashlib
import datetime

import pymysql
import markdown as md_lib

KB_ROOT = r"d:\kefu_center_web\钉钉知识库"

DB = dict(
    host="127.0.0.1",
    port=3306,
    user=os.environ["DB_USERNAME"],
    password=os.environ["DB_PASSWORD"],
    database="kefu_center",
    charset="utf8mb4",
)

MD_EXTS = ["tables", "fenced_code", "sane_lists"]


def ms_to_dt(ms):
    """毫秒时间戳 → datetime；失败返回 None。"""
    try:
        return datetime.datetime.fromtimestamp(int(ms) / 1000.0)
    except Exception:
        return None


def load_meta(meta_path):
    """读取 _meta.json 返回 dict；不存在/解析失败返回 {}。"""
    if not os.path.exists(meta_path):
        return {}
    try:
        with open(meta_path, encoding="utf-8") as f:
            return json.load(f)
    except Exception:
        return {}


def md_to_html(text):
    """钉钉 md → HTML：去标题行/钉钉链接引用行、图片占位、markdown 转换。"""
    # 去掉 `> 钉钉文档链接: ...` 引用行（url 已单独入库）
    text = re.sub(r"^\s*>\s*钉钉文档链接\s*:.*$", "", text, flags=re.MULTILINE)
    # 去掉开头的一级标题（标题已存 title 列，避免渲染重复）
    text = re.sub(r"^\s*#\s+.*$", "", text, count=1, flags=re.MULTILINE)
    # 图片 → 占位（OSS 签名链接会过期，不嵌入）
    text = re.sub(
        r"!\[([^\]]*)\]\([^)]*\)",
        lambda m: "【图片：%s】" % (m.group(1) or "图"),
        text,
    )
    return md_lib.markdown(text, extensions=MD_EXTS).strip()


def collect_docs():
    """遍历知识库，返回 [(rel_path, md_path, meta_path)]，按路径排序。"""
    out = []
    for root, _dirs, fnames in os.walk(KB_ROOT):
        for fn in fnames:
            if not fn.endswith(".md"):
                continue
            md_path = os.path.join(root, fn)
            rel = os.path.relpath(md_path, KB_ROOT).replace("\\", "/")
            meta_path = os.path.join(root, "_meta.json")
            out.append((rel, md_path, meta_path))
    out.sort(key=lambda x: x[0])
    return out


def main():
    docs = collect_docs()
    print("发现 %d 篇 .md 文档" % len(docs), flush=True)

    conn = pymysql.connect(**DB)
    cur = conn.cursor()

    # 深层目录路径较长，先放宽相关列（幂等，重跑安全）
    for sql in (
        "ALTER TABLE documents MODIFY COLUMN content MEDIUMTEXT",
        "ALTER TABLE documents MODIFY COLUMN category VARCHAR(128)",
        "ALTER TABLE documents MODIFY COLUMN subcategory VARCHAR(512)",
    ):
        try:
            cur.execute(sql)
            conn.commit()
            print("已执行:", sql, flush=True)
        except Exception as e:
            print("ALTER 异常(可能已扩):", e, flush=True)

    cur.execute("DELETE FROM documents WHERE platform='dingtalk'")
    print("已清空 platform='dingtalk' 旧记录: %d 行" % cur.rowcount, flush=True)
    conn.commit()

    cat_count = {}
    inserted = 0
    skipped = 0
    now = datetime.datetime.now()

    for rel, md_path, meta_path in docs:
        parts = rel.split("/")
        parent = parts[:-1]  # 去掉末尾的 .md 文件名
        if not parent:
            skipped += 1
            continue
        category = parent[0]
        subcategory = "/".join(parent[1:-1]) if len(parent) > 2 else ""

        meta = load_meta(meta_path)
        title = meta.get("name") or parent[-1]
        url = meta.get("docUrl") or ""

        try:
            with open(md_path, encoding="utf-8") as f:
                text = f.read()
        except Exception as e:
            print("  [跳过] 读取失败 %s: %s" % (rel, e), flush=True)
            skipped += 1
            continue

        content = md_to_html(text)
        file_size = os.path.getsize(md_path)
        upload_time = ms_to_dt(meta.get("createTime")) or now
        updated_at = ms_to_dt(meta.get("updateTime")) or now
        doc_id = "DOC-DD-" + hashlib.md5(rel.encode("utf-8")).hexdigest()[:12].upper()

        cur.execute(
            """
            INSERT INTO documents
              (id, title, format, category, subcategory, file_size, upload_time,
               updated_at, parse_status, version, updated_by, content, platform, url)
            VALUES
              (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)
            """,
            (
                doc_id, title, "html", category, subcategory, file_size,
                upload_time, updated_at, "PARSED", 1, "钉钉知识库导入",
                content, "dingtalk", url,
            ),
        )
        inserted += 1
        cat_count[category] = cat_count.get(category, 0) + 1

    conn.commit()
    conn.close()

    print("\n各一级目录导入数量：", flush=True)
    for c in sorted(cat_count):
        print("  %-30s %d" % (c, cat_count[c]), flush=True)
    print("\n导入完成：成功 %d 篇，跳过 %d 篇。" % (inserted, skipped), flush=True)


if __name__ == "__main__":
    main()
