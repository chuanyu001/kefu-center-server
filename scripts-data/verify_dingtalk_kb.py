# -*- coding: utf-8 -*-
"""验证钉钉知识库导入与检索：documents/chunks 计数、目录映射、ngram 全文检索命中。"""
import os
import sys

import pymysql

sys.stdout.reconfigure(encoding="utf-8")

conn = pymysql.connect(
    host="127.0.0.1",
    port=3306,
    user=os.environ["DB_USERNAME"],
    password=os.environ["DB_PASSWORD"],
    database="kefu_center",
    charset="utf8mb4",
)
cur = conn.cursor()

print("=== documents 按平台计数 ===")
cur.execute("SELECT platform, COUNT(*) FROM documents GROUP BY platform ORDER BY platform")
for p, n in cur.fetchall():
    print("  %-10s %d" % (p or "(null)", n))

print("\n=== document_chunks 按平台计数 ===")
cur.execute("SELECT platform, COUNT(*) FROM document_chunks GROUP BY platform ORDER BY platform")
for p, n in cur.fetchall():
    print("  %-10s %d" % (p or "(null)", n))

print("\n=== 钉钉文档目录映射抽查（含多级 subcategory）===")
cur.execute(
    "SELECT title, category, subcategory FROM documents "
    "WHERE platform='dingtalk' AND subcategory <> '' "
    "ORDER BY CHAR_LENGTH(subcategory) DESC LIMIT 3"
)
for title, cat, sub in cur.fetchall():
    print("  标题=%s | 分类=%s | 子目录=%s" % (title, cat, sub))

print("\n=== 钉钉文档带 url 抽查 ===")
cur.execute(
    "SELECT title, LEFT(url, 60) FROM documents "
    "WHERE platform='dingtalk' AND url <> '' LIMIT 3"
)
for title, url in cur.fetchall():
    print("  标题=%s | url=%s..." % (title, url))

print("\n=== ngram 全文检索测试 ===")
for kw in ["保养话术", "故障诊断", "质检"]:
    cur.execute(
        "SELECT doc_title, chunk_text FROM document_chunks "
        "WHERE platform='dingtalk' AND MATCH(chunk_text) AGAINST(%s IN NATURAL LANGUAGE MODE) LIMIT 1",
        (kw,),
    )
    row = cur.fetchone()
    if row:
        print("  [%s] 命中 -> %s | %s" % (kw, row[0], (row[1] or "")[:50].replace("\n", " ")))
    else:
        print("  [%s] 无命中" % kw)

conn.close()
print("\n验证完成。")
