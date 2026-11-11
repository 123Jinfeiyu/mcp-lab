#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
把 luac 调试段里含非 ASCII 字节的 局部变量名/upvalue名 替换成唯一 ASCII 名,
使 unluac 输出可再编译(中文标识符会被 unluac 打成 '?')。仅改调试信息, 不影响指令。
用法: python3 ascii_names.py <in.luac> <out.luac>
"""
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import luac


def is_simple_ascii(s):
    if s is None:
        return True
    for ch in s:
        if ch >= 0x80:
            return False
    return True


def main():
    src, dst = sys.argv[1], sys.argv[2]
    ch = luac.parse(open(src, 'rb').read())
    counter = [0]

    def rename(f):
        for i, (name, sp, ep) in enumerate(f.locvars):
            if name is not None and not is_simple_ascii(name):
                counter[0] += 1
                f.locvars[i] = (b'v%d' % counter[0], sp, ep)
        for i, name in enumerate(f.upnames):
            if name is not None and not is_simple_ascii(name):
                counter[0] += 1
                f.upnames[i] = b'v%d' % counter[0]
        for p in f.protos:
            rename(p)

    rename(ch.main)
    open(dst, 'wb').write(luac.serialize(ch))
    print("renamed %d names -> %s" % (counter[0], dst))


if __name__ == '__main__':
    main()
