import os
import pymysql

conn = pymysql.connect(
    host=os.environ.get('DB_HOST', '127.0.0.1'),
    port=int(os.environ.get('DB_PORT', '3306')),
    user=os.environ.get('DB_USERNAME', 'root'),
    password=os.environ.get('DB_PASSWORD', ''),
    database='kefu_center',
    charset='utf8',
)
cur = conn.cursor()

columns = [
    ("recorder_device_id", "VARCHAR(64) DEFAULT '' COMMENT '记录仪设备ID'", "recorder_model"),
    ("antenna_position", "VARCHAR(128) DEFAULT '' COMMENT '天线位置'", "recorder_device_id"),
    ("no_position_reason", "VARCHAR(256) DEFAULT '' COMMENT '不定位原因'", "antenna_position"),
    ("no_position_issue", "VARCHAR(256) DEFAULT '' COMMENT '未定位问题现象'", "no_position_reason"),
    ("antenna_damaged", "VARCHAR(16) DEFAULT '' COMMENT '天线是否损坏'", "no_position_issue"),
    ("qiyu_ticket_status", "INT DEFAULT NULL COMMENT '七鱼工单状态'", "antenna_damaged"),
    ("qiyu_ticket_category", "VARCHAR(64) DEFAULT '' COMMENT '七鱼工单分类'", "qiyu_ticket_status"),
]

for name, col_def, after in columns:
    sql = f"ALTER TABLE sessions ADD COLUMN {name} {col_def} AFTER {after}"
    try:
        cur.execute(sql)
        print(f"OK: {name}")
    except Exception as e:
        print(f"SKIP: {name} - {e}")

conn.commit()
conn.close()
print("Done")
