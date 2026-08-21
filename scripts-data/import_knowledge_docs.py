#!/usr/bin/env python3
"""导入客服中心知识文档到 kefu_center.documents 表"""
import zipfile, re, pymysql, os

DOCS = [
    ("d:/kefu_center_web/docs/客服中心人员异常情绪疏导方案.docx", "应急方案"),
    ("d:/kefu_center_web/docs/客服中心断电、断网应急方案.docx", "应急方案"),
]

def extract_docx(path):
    z = zipfile.ZipFile(path)
    xml = z.read('word/document.xml').decode('utf-8')
    # 提取段落
    paragraphs = re.findall(r'<w:p[ >].*?</w:p>', xml, re.DOTALL)
    lines = []
    for p in paragraphs:
        texts = re.findall(r'<w:t[^>]*>(.*?)</w:t>', p, re.DOTALL)
        line = ''.join(texts).strip()
        if line:
            lines.append(line)
    return '\n'.join(lines)

conn = pymysql.connect(host='127.0.0.1', port=3306, user='root', password=os.environ['DB_PASSWORD'], database='kefu_center', charset='utf8mb4')
cur = conn.cursor()

for path, category in DOCS:
    title = path.split('/')[-1].replace('.docx', '')
    content = extract_docx(path)
    doc_id = 'DOC' + str(abs(hash(title)) % 10**10).zfill(10)
    # 先删除旧的
    cur.execute("DELETE FROM documents WHERE title=%s", (title,))
    cur.execute("""INSERT INTO documents (id, title, format, category, subcategory, content, parse_status)
                   VALUES (%s, %s, %s, %s, %s, %s, %s)""",
                (doc_id, title, 'docx', category, '', content, 'PARSED'))
    print(f"已导入: {title} (id={doc_id}, 内容{len(content)}字)")

conn.commit()
cur.close(); conn.close()
print("完成")
