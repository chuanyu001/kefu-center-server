#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
记录仪信息查询脚本
  输入 VIN 码 → 调用公司批量接口 → 打印结果 + 存进 CSV 文件(桌面)

用法:
  1) 命令行传入:   python3 记录仪查询.py LVxxxxxxxx LVyyyyyyyy
  2) 直接运行:     python3 记录仪查询.py      然后按提示输入(空格/逗号分隔，多个 VIN)

接口地址:
  默认走线上(本机可达): https://dr.smartlink.com.cn/.../batchVehicleInfo
  测试阶段在公司测试容器内,可用内网地址(本机/外网连不上):
      RECORDER_URL='http://172.29.30.157:32706/tob/openapi/business/batchVehicleInfo' python3 记录仪查询.py ...

返回字段映射:  carNumber→车牌号  recorderId→记录仪设备号  vin→VIN码  validDateEnd→到期时间
只依赖 Python3 标准库,无需 pip 安装。
"""

import sys
import os
import json
import csv
import urllib.request
import urllib.error
from datetime import datetime

PROD_URL = "https://dr.smartlink.com.cn/drapp/api/operate/tob/openapi/business/batchVehicleInfo"
URL = os.environ.get("RECORDER_URL", PROD_URL)

OUT_FILE = os.path.join(os.path.expanduser("~"), "Desktop", "记录仪信息.csv")
CSV_HEADER = ["车牌号", "记录仪设备号", "VIN码", "到期时间", "查询时间"]


def query_recorder(vin_list):
    """POST {vinList:[...]} → 返回 data 列表(每项含 vin/recorderId/carNumber/validDateEnd)。"""
    body = json.dumps({"vinList": vin_list}).encode("utf-8")
    req = urllib.request.Request(
        URL, data=body,
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=15) as resp:
        raw = resp.read().decode("utf-8")
    obj = json.loads(raw)
    code = obj.get("code")
    if code != 200:
        raise RuntimeError("接口返回非 200(code=%s): %s" % (code, raw[:300]))
    return obj.get("data") or []


def parse_vins(text):
    """空格/英文逗号/中文逗号/换行 分隔 → VIN 列表(去空、大写)。"""
    for ch in [",", "，", "\n", "\t", ";", "；"]:
        text = text.replace(ch, " ")
    return [v.strip().upper() for v in text.split() if v.strip()]


def main():
    args = sys.argv[1:]
    raw_in = " ".join(args) if args else input("请输入 VIN 码(多个用空格/逗号分隔): ").strip()
    vins = parse_vins(raw_in)
    if not vins:
        print("未输入 VIN，已退出。")
        return

    print("\n查询 %d 个 VIN …" % len(vins))
    print("接口: %s\n" % URL)

    try:
        data = query_recorder(vins)
    except urllib.error.URLError as e:
        print("✗ 网络错误: %s" % e)
        if "172.29." in URL or "10." in URL.split("//")[-1][:3]:
            print("  提示: 内网地址只能在公司测试容器内访问，本机/外网连不上。本机请用默认线上地址。")
        return
    except Exception as e:
        print("✗ 查询失败: %s" % e)
        return

    now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    by_vin = {}
    for d in data:
        by_vin[(d.get("vin") or "").upper()] = d

    rows = []
    for v in vins:
        d = by_vin.get(v)
        if d:
            car = d.get("carNumber") or ""
            dev = d.get("recorderId") or ""
            due = d.get("validDateEnd") or ""
            print("VIN: %s" % v)
            print("  车牌号:       %s" % (car or "(空)"))
            print("  记录仪设备号: %s" % (dev or "(空)"))
            print("  到期时间:     %s\n" % (due or "(空)"))
            rows.append([car, dev, v, due, now])
        else:
            print("VIN: %s  → 未查到记录仪信息\n" % v)
            rows.append(["", "", v, "未查到", now])

    # 追加写 CSV：新文件先写表头；utf-8-sig 带 BOM，Excel 打开中文不乱码
    new_file = not os.path.exists(OUT_FILE)
    with open(OUT_FILE, "a", encoding="utf-8-sig", newline="") as f:
        w = csv.writer(f)
        if new_file:
            w.writerow(CSV_HEADER)
        w.writerows(rows)

    print("已保存 %d 条到文件: %s" % (len(rows), OUT_FILE))


if __name__ == "__main__":
    main()
