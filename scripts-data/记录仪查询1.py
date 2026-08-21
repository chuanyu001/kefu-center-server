#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
记录仪查询（调线上接口，按 VIN 取实时记录仪信息）
  交互式：每次输入【单个】VIN 码回车查询，结果直接打印在终端（不生成文件）。
  字段以中文显示：车牌号 / 记录仪设备号 / VIN码 / 到期时间。

用法:
  python3 记录仪查询.py            # 运行后按提示逐个输入 VIN，q 退出
  python3 记录仪查询.py LVxxxx     # 直接查一个，查完即退

接口默认走线上(本机可达)。测试阶段在公司测试容器内可用内网地址:
  RECORDER_URL='http://172.29.30.157:32706/tob/openapi/business/batchVehicleInfo' python3 记录仪查询.py
只依赖 Python3 标准库。
"""

import sys
import os
import json
import urllib.request
import urllib.error

PROD_URL = "https://dr.smartlink.com.cn/drapp/api/operate/tob/openapi/business/batchVehicleInfo"
URL = os.environ.get("RECORDER_URL", PROD_URL)

# 接口英文字段 → 中文显示
FIELDS = [
    ("carNumber", "车牌号"),
    ("recorderId", "记录仪设备号"),
    ("vin", "VIN码"),
    ("validDateEnd", "到期时间"),
]


def query_one(vin):
    """POST {vinList:[vin]} → 该 VIN 对应的记录仪信息 dict（查不到返回 None）。"""
    body = json.dumps({"vinList": [vin]}).encode("utf-8")
    req = urllib.request.Request(
        URL, data=body,
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=15) as resp:
        obj = json.loads(resp.read().decode("utf-8"))
    if obj.get("code") != 200:
        raise RuntimeError("接口返回非 200：%s" % json.dumps(obj, ensure_ascii=False)[:200])
    data = obj.get("data") or []
    for d in data:
        if (d.get("vin") or "").upper() == vin.upper():
            return d
    return data[0] if data else None


def show(vin):
    vin = vin.strip().upper()
    if not vin:
        return
    try:
        d = query_one(vin)
    except urllib.error.URLError as e:
        print("✗ 网络错误：%s" % e)
        if "172.29." in URL:
            print("  提示：内网地址只能在公司测试容器内访问，本机请用默认线上地址。")
        return
    except Exception as e:
        print("✗ 查询失败：%s" % e)
        return
    if not d:
        print("✗ %s  → 未查到记录仪信息\n" % vin)
        return
    print("✓ %s" % vin)
    for key, label in FIELDS:
        val = d.get(key)
        print("    %-8s %s" % (label + "：", val if val not in (None, "") else "(空)"))
    print("")


def main():
    args = sys.argv[1:]
    if args:                       # 直接传一个 VIN：查完即退
        show(args[0])
        return
    print("接口：%s" % URL)
    print("输入单个 VIN 码后回车查询；直接回车或输入 q 退出。")
    while True:
        try:
            raw = input("VIN> ").strip()
        except (EOFError, KeyboardInterrupt):
            print()
            break
        if not raw or raw.lower() in ("q", "quit", "exit"):
            break
        show(raw)
    print("已退出。")


if __name__ == "__main__":
    main()
