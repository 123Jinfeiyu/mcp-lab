#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
比较两个 luac 的语义内容(指令/常量/子函数/upvalue), 忽略调试段与行号。
用于验证 unluac 反编译源码重编译后的保真度。
用法: python3 cmp_luac.py <原始.luac> <重编译.luac> [--verbose]
"""
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import luac


def cmpf(a, b, path, diffs, maxdiff=20):
    if a.numparams != b.numparams:
        diffs.append("%s numparams %d!=%d" % (path, a.numparams, b.numparams))
    if a.is_vararg != b.is_vararg:
        diffs.append("%s is_vararg %d!=%d" % (path, a.is_vararg, b.is_vararg))
    if a.maxstacksize != b.maxstacksize:
        diffs.append("%s maxstacksize %d!=%d" % (path, a.maxstacksize, b.maxstacksize))
    if a.upvals != b.upvals:
        diffs.append("%s upvals %s != %s" % (path, a.upvals, b.upvals))
    if len(a.code) != len(b.code):
        diffs.append("%s code count %d!=%d" % (path, len(a.code), len(b.code)))
    n = min(len(a.code), len(b.code))
    for i in range(n):
        if a.code[i] != b.code[i]:
            diffs.append("%s code[%d] %08x != %08x" % (path, i, a.code[i], b.code[i]))
            if len(diffs) > maxdiff:
                return
    if len(a.constants) != len(b.constants):
        diffs.append("%s const count %d!=%d" % (path, len(a.constants), len(b.constants)))
    nk = min(len(a.constants), len(b.constants))
    for i in range(nk):
        ta, va = a.constants[i]
        tb, vb = b.constants[i]
        if (ta & 0x13) != (tb & 0x13) or va != vb:
            diffs.append("%s const[%d] (%#x,%r) != (%#x,%r)" % (path, i, ta, va[:20] if isinstance(va, bytes) else va, tb, vb[:20] if isinstance(vb, bytes) else vb))
    if len(a.protos) != len(b.protos):
        diffs.append("%s proto count %d!=%d" % (path, len(a.protos), len(b.protos)))
    for i in range(min(len(a.protos), len(b.protos))):
        cmpf(a.protos[i], b.protos[i], path + '/%d' % i, diffs, maxdiff)


def main():
    a = luac.parse(open(sys.argv[1], 'rb').read())
    b = luac.parse(open(sys.argv[2], 'rb').read())
    diffs = []
    cmpf(a.main, b.main, 'main', diffs)
    if diffs:
        print("DIFF %d 处 (显示前 %d):" % (len(diffs), min(20, len(diffs))))
        for d in diffs[:20]:
            print("  " + d)
        sys.exit(1)
    print("IDENTICAL: 指令/常量/子函数/upvalue 完全一致")


if __name__ == '__main__':
    main()
