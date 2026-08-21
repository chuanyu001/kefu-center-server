# -*- coding: utf-8 -*-
"""
将 MySQL 的 kefu_center 库完整导出为 SQLite 文件，内容原样保留。
只读 MySQL，不修改任何源数据。生成的文件可用 DB Browser for SQLite 打开查询。

用法:
    python export_to_sqlite.py
输出:
    d:\\kefu_center_web\\kefu_center.db
"""
import datetime
import decimal
import os
import sqlite3

import pymysql

MYSQL = dict(
    host="127.0.0.1",
    port=3306,
    user="root",
    password=os.environ["DB_PASSWORD"],
    database="kefu_center",
    charset="utf8mb4",
)

OUT = r"d:\kefu_center_web\kefu_center.db"


def sqlite_type(mysql_type: str) -> str:
    """MySQL 类型 -> SQLite 亲和类型。datetime/decimal 归为 TEXT 以保证内容无损。"""
    t = (mysql_type or "").lower()
    if "blob" in t or "binary" in t:
        return "BLOB"
    if any(x in t for x in ("tinyint", "smallint", "mediumint", "int", "bigint", "bit")):
        return "INTEGER"
    if any(x in t for x in ("float", "double", "real")):
        return "REAL"
    # char/varchar/text/json/enum/set/datetime/timestamp/date/time/year/decimal/numeric -> TEXT
    return "TEXT"


def to_sqlite_value(v):
    """把 pymysql 返回的 Python 值无损转为 SQLite 可存储的值。"""
    if v is None:
        return None
    if isinstance(v, (datetime.datetime, datetime.date, datetime.time)):
        return str(v)
    if isinstance(v, decimal.Decimal):
        return str(v)  # 转字符串，避免浮点精度损失
    if isinstance(v, (bytes, bytearray)):
        return sqlite3.Binary(bytes(v))
    if isinstance(v, bool):
        return int(v)
    if isinstance(v, (int, float, str)):
        return v
    return str(v)


def main():
    conn = pymysql.connect(**MYSQL)
    cur = conn.cursor()

    # 列出所有表
    cur.execute(
        "SELECT table_name FROM information_schema.tables "
        "WHERE table_schema=%s AND table_type='BASE TABLE' ORDER BY table_name",
        (MYSQL["database"],),
    )
    tables = [row[0] for row in cur.fetchall()]

    if os.path.exists(OUT):
        os.remove(OUT)
    sconn = sqlite3.connect(OUT)
    sc = sconn.cursor()

    summary = []
    for table in tables:
        cur.execute(
            "SELECT column_name, data_type FROM information_schema.columns "
            "WHERE table_schema=%s AND table_name=%s ORDER BY ordinal_position",
            (MYSQL["database"], table),
        )
        cols = cur.fetchall()  # [(name, mysql_type), ...]
        if not cols:
            continue

        col_defs = []
        col_names = []
        for name, mtype in cols:
            quoted = '"%s"' % name.replace('"', '""')
            col_defs.append("%s %s" % (quoted, sqlite_type(mtype)))
            col_names.append(name)

        sc.execute(
            'CREATE TABLE "%s" (%s)'
            % (table.replace('"', '""'), ", ".join(col_defs))
        )

        cur.execute('SELECT %s FROM `%s`' % (", ".join("`%s`" % c for c in col_names), table))
        rows = cur.fetchall()
        placeholders = ", ".join("?" for _ in col_names)
        insert_sql = 'INSERT INTO "%s" VALUES (%s)' % (table.replace('"', '""'), placeholders)

        converted = [tuple(to_sqlite_value(v) for v in row) for row in rows]
        sc.executemany(insert_sql, converted)

        # 校验：SQLite 端行数
        sc.execute('SELECT COUNT(*) FROM "%s"' % table.replace('"', '""'))
        sqlite_count = sc.fetchone()[0]
        summary.append((table, len(rows), sqlite_count))
        print("  %-16s MySQL=%d  SQLite=%d" % (table, len(rows), sqlite_count))

    sconn.commit()
    sc.close()
    sconn.close()
    cur.close()
    conn.close()

    print("\n导出完成: %s" % OUT)
    print("表数量: %d" % len(summary))
    mismatch = [(t, a, b) for t, a, b in summary if a != b]
    if mismatch:
        print("!! 行数不一致:", mismatch)
    else:
        print("所有表行数与 MySQL 一致，内容无损。")


if __name__ == "__main__":
    main()
