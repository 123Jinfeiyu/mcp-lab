#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
【第2步】转标准 luac —— 包装脚本, 实际调用上级目录的 解密脚本.py
输入: 原始加密 *.lua 或"第1步"输出? (本工具直接读原始加密文件, 一步到位)
输出: 标准 Lua 5.3 luac (字段/代码区已还原)

用法: python3 2_转标准luac.py <输入目录或文件> <输出目录>
"""
import sys, os, subprocess

HERE = os.path.dirname(os.path.abspath(__file__))
TOOL = os.path.join(os.path.dirname(HERE), "解密脚本.py")
if not os.path.isfile(TOOL):
    TOOL = os.path.join(HERE, "解密脚本.py")
sys.exit(subprocess.call([sys.executable, TOOL] + sys.argv[1:]))
