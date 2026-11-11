#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
对 unluac 反编译的主脚本做“纯本地化”源码改写:
  1) 移除启动期/循环内的 getpage 上报请求(作者服务器)
  2) 屏蔽词/回复屏蔽词 改读本地文件(本地读取列表)
  3) 停止时间 固定为 0/0 (停用“停止时间”自动退出窗口)
  4) 删除所有 exitScript() 调用(不再自动停止; 手动停止仍由App按钮控制)
  5) AI话术/AIkey/模型 可被本地 配置.txt 覆盖; 默认用本地话术
  6) AI 生成失败或关闭时回退本地话术, 保证流程继续
用法: python3 localize_main.py <main.fix.lua> <main.local.lua>
"""
import sys

PINGBI_LINE = b'_ENV["\\229\\177\\143\\232\\148\\189\\232\\175\\141"] = _ENV["\\230\\156\\172\\229\\156\\176\\232\\175\\187\\229\\143\\150\\229\\136\\151\\232\\161\\168"]("\\229\\177\\143\\232\\148\\189\\232\\175\\141")'
HUIFU_LINE = b'_ENV["\\229\\155\\158\\229\\164\\141\\229\\177\\143\\232\\148\\189\\232\\175\\141"] = _ENV["\\230\\156\\172\\229\\156\\176\\232\\175\\187\\229\\143\\150\\229\\136\\151\\232\\161\\168"]("\\229\\155\\158\\229\\164\\141\\229\\177\\143\\232\\148\\189\\232\\175\\141")'
TIME_SPLIT_LINE = b'_ENV["\\230\\151\\182\\233\\151\\180\\229\\136\\134\\229\\137\\178"] = {}'
STOP1_LINE = b'_ENV["\\229\\129\\156\\230\\173\\162\\230\\151\\182\\233\\151\\180"] = 0'
STOP2_LINE = b'_ENV["\\229\\129\\156\\230\\173\\162\\230\\151\\182\\233\\151\\1802"] = 0'

AI_HUASHU_LINE = b'_ENV["AI\\232\\175\\157\\230\\156\\175"] = _ENV["\\230\\156\\172\\229\\156\\176AI\\232\\175\\157\\230\\156\\175"] == "1"'
AIMODE_LINE = b'AImode = _ENV["\\230\\156\\172\\229\\156\\176AImode"] or "ep-20260308180203-vzdbk"'
AIKEY_LINE = b'AIkey = _ENV["\\230\\156\\172\\229\\156\\176AIkey"] or "64e65631-8544-4521-971e-c5102c97c30a"'
DAIKEY_LINE = b'DAIkey = _ENV["\\230\\156\\172\\229\\156\\176DAIkey"] or "sk-329d4326159245ec97441525a33fb72c"'
DAIMODE_LINE = b'DAImode = _ENV["\\230\\156\\172\\229\\156\\176DAImode"] or "deepseek-chat"'

# 主流程(发私信, 步骤19) AI失败/未启用时回退本地话术
MAIN_FALLBACK_OLD = (b'      else\n'
                     b'        return\n'
                     b'      end\n'
                     b'      if _ENV["\\229\\190\\133\\229\\143\\145\\233\\128\\129"] ~= nil then\n'
                     b'        _ENV["\\230\\173\\165\\233\\170\\164"] = 20\n'
                     b'      end\n')
MAIN_FALLBACK_NEW = (b'      else\n'
                     b'        _ENV["\\229\\190\\133\\229\\143\\145\\233\\128\\129"] = _ENV["\\230\\156\\172\\229\\156\\176\\232\\175\\187\\229\\143\\150\\230\\150\\135\\230\\156\\172"]("\\232\\175\\157\\230\\156\\175", "\\228\\189\\160\\229\\165\\189")\n'
                     b'      end\n'
                     b'      if _ENV["\\229\\190\\133\\229\\143\\145\\233\\128\\129"] == nil then\n'
                     b'        _ENV["\\229\\190\\133\\229\\143\\145\\233\\128\\129"] = _ENV["\\230\\156\\172\\229\\156\\176\\232\\175\\187\\229\\143\\150\\230\\150\\135\\230\\156\\172"]("\\232\\175\\157\\230\\156\\175", "\\228\\189\\160\\229\\165\\189")\n'
                     b'      end\n'
                     b'      _ENV["\\230\\173\\165\\233\\170\\164"] = 20\n')

# 私信回复流程(main 与 mainuid 各一处): AI失败/未启用时回退本地私信回复
PRIV_FALLBACK_OLD = (b'            else\n'
                     b'              return\n'
                     b'            end\n'
                     b'            if _ENV["\\229\\190\\133\\229\\143\\145\\233\\128\\129"] ~= nil then\n'
                     b'              _ENV["\\230\\173\\165\\233\\170\\164"] = 5\n'
                     b'            end\n')
PRIV_FALLBACK_NEW = (b'            else\n'
                     b'              _ENV["\\229\\190\\133\\229\\143\\145\\233\\128\\129"] = _ENV["\\230\\156\\172\\229\\156\\176\\232\\175\\187\\229\\143\\150\\230\\150\\135\\230\\156\\172"]("\\231\\167\\129\\228\\191\\161\\229\\155\\158\\229\\164\\141", "\\228\\189\\160\\229\\165\\189")\n'
                     b'            end\n'
                     b'            if _ENV["\\229\\190\\133\\229\\143\\145\\233\\128\\129"] == nil then\n'
                     b'              _ENV["\\229\\190\\133\\229\\143\\145\\233\\128\\129"] = _ENV["\\230\\156\\172\\229\\156\\176\\232\\175\\187\\229\\143\\150\\230\\150\\135\\230\\156\\172"]("\\231\\167\\129\\228\\191\\161\\229\\155\\158\\229\\164\\141", "\\228\\189\\160\\229\\165\\189")\n'
                     b'            end\n'
                     b'            _ENV["\\230\\173\\165\\233\\170\\164"] = 5\n')


def main():
    src, dst = sys.argv[1], sys.argv[2]
    data = open(src, 'rb').read()
    lines = data.split(b'\n')
    out = []
    i = 0
    stats = {'getpage': 0, 'exit': 0, 'blocks': 0}
    while i < len(lines):
        ln = lines[i]
        if b'?type=getpage' in ln:
            stats['getpage'] += 1
            i += 1
            continue
        if ln.strip() == b'exitScript()':
            stats['exit'] += 1
            i += 1
            continue
        if b'?type=pingbi' in ln:
            assert b'?type=huifupingbi' in lines[i + 2], "pingbi块结构不符"
            out.append(PINGBI_LINE)
            out.append(HUIFU_LINE)
            i += 4
            continue
        if b'?type=shijian' in ln:
            assert b'splitStr(ret, "-")' in lines[i + 1], "shijian块结构不符"
            out.append(TIME_SPLIT_LINE)
            out.append(STOP1_LINE)
            out.append(STOP2_LINE)
            i += 4
            continue
        st = ln.strip()
        if st.startswith(b'if _ENV["\\229\\189\\147\\229\\137\\141\\230\\151\\182\\233\\151\\180"]') and b'\\229\\129\\156\\230\\173\\162\\230\\151\\182\\233\\151\\180' in ln:
            assert lines[i + 3].strip() == b'end', "启动停时块结构不符"
            i += 4
            continue
        out.append(ln)
        i += 1
    data = b'\n'.join(out)
    stats['blocks'] = 0

    def sub(old, new, expect=1):
        nonlocal data
        n = data.count(old)
        if n != expect:
            raise AssertionError("匹配 %d 次(期望 %d): %r" % (n, expect, old[:60]))
        data = data.replace(old, new)
        stats['blocks'] += n

    sub(b'_ENV["AI\\232\\175\\157\\230\\156\\175"] = true', AI_HUASHU_LINE)
    sub(b'AImode = "ep-20260308180203-vzdbk"', AIMODE_LINE)
    sub(b'AIkey = "64e65631-8544-4521-971e-c5102c97c30a"', AIKEY_LINE)
    sub(b'DAIkey = "sk-329d4326159245ec97441525a33fb72c"', DAIKEY_LINE)
    sub(b'DAImode = "deepseek-chat"', DAIMODE_LINE)
    sub(MAIN_FALLBACK_OLD, MAIN_FALLBACK_NEW, 1)
    sub(PRIV_FALLBACK_OLD, PRIV_FALLBACK_NEW, 2)

    open(dst, 'wb').write(data)
    print("localize OK: 移除getpage=%d 移除exitScript=%d 覆写块=%d -> %s (%d 字节)" %
          (stats['getpage'], stats['exit'], stats['blocks'], dst, len(data)))


if __name__ == '__main__':
    main()
