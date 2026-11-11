#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
把解密/本地化后的脚本源装配进 新App/assets/lua/:
  1) main.local.lua -> main.lua   (适配新宿主: 去掉插件 require、UI改本机配置、点抖音改 launchApp)
  2) 复制 page/lib/HWD、localdata、免登录stub
说明: 本工具只处理脚本文本, 不改动原始产物。
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
SRC = os.path.join(ROOT, "script_src")
DST = os.path.join(ROOT, "assets", "lua")
PATCH = "/storage/emulated/0/MT2/mcp/本地Patch/src"


def patch_main(data: bytes) -> bytes:
    out = data
    for req in (b'require("File")\n', b'require("EAD")\n',
                b'require("http")\n', b'require("PaddLeOCR")\n'):
        if req not in out:
            print("警告: 未找到", req)
        out = out.replace(req, b'')

    old_ui = b'local action, arr = showUIEx("main.ui", 50, 50, 980, 1720)'
    new_ui = 'local action, arr = 1, _ENV["本机配置"]'.encode("utf-8")
    if old_ui not in out:
        raise SystemExit("未找到 showUIEx 行")
    out = out.replace(old_ui, new_ui)

    lines = out.split(b'\n')
    hit = 0
    for i, ln in enumerate(lines):
        if b'tap(_ENV[' in ln and b'\\230\\138\\150\\233\\159\\179\\229\\157\\144\\230\\160\\135' in ln:
            lines[i] = b'    launchApp("com.ss.android.ugc.aweme")'
            hit += 1
    if hit < 1:
        raise SystemExit("抖音坐标 tap 行未匹配")
    print("替换 抖音坐标tap -> launchApp: %d 处" % hit)
    return b'\n'.join(lines)


def main():
    os.makedirs(DST, exist_ok=True)

    main_src = open(os.path.join(SRC, "main.local.lua"), "rb").read()
    open(os.path.join(DST, "main.lua"), "wb").write(patch_main(main_src))

    for name in ("page.lua", "lib.lua", "HWD.lua"):
        open(os.path.join(DST, name), "wb").write(open(os.path.join(SRC, name), "rb").read())

    ld = open(os.path.join(PATCH, "localdata.lua"), encoding="utf-8").read()
    old = 'if k ~= nil and v ~= nil and v ~= "" then'
    new = 'if k ~= nil and v ~= nil and v ~= "" and _ENV["本地" .. k] == nil then'
    if old not in ld:
        raise SystemExit("localdata 配置行未找到")
    ld = ld.replace(old, new)
    open(os.path.join(DST, "localdata.lua"), "w", encoding="utf-8").write(ld)

    stub = open(os.path.join(PATCH, "HWD_Login.stub.lua"), encoding="utf-8").read()
    open(os.path.join(DST, "HWD_Login.lua"), "w", encoding="utf-8").write(stub)

    print("脚本装配完成 ->", DST)
    for f in sorted(os.listdir(DST)):
        print("   ", f, os.path.getsize(os.path.join(DST, f)))


if __name__ == "__main__":
    main()
