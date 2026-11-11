#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
【第1步】文件层解密 (第一层)
输入: assets/script.lr 解包出的 *.lua (0x86 'LUA' 开头)
输出: 解密后的 Lua 二进制 chunk (版本3仍带第二层字段加密)

用法: python3 1_文件解密.py <输入目录或文件> <输出目录>
"""
import sys, os

HEAD = 562

def decrypt(path):
    f = open(path, 'rb').read()
    if len(f) < HEAD or f[0] != 0x86 or f[1:4] != b'LUA':
        return None
    ver = f[4]
    key = f[12:42]
    head = bytes(0xFF & ~(f[8 + i] ^ key[i]) for i in range(4))
    body = bytes(0xFF & ~(f[HEAD + j] ^ key[(HEAD + j) % 30])
                 for j in range(len(f) - HEAD))
    return head + body, ver, key

def main():
    if len(sys.argv) < 3:
        print(__doc__); sys.exit(1)
    src, dst = sys.argv[1], sys.argv[2]
    files = [src] if os.path.isfile(src) else [
        os.path.join(r, f) for r, _, fs in os.walk(src) for f in fs
        if f.lower().endswith('.lua')]
    ok = 0
    for p in sorted(files):
        r = decrypt(p)
        if not r:
            print("跳过(非LR):", p); continue
        data, ver, key = r
        rel = os.path.relpath(p, src if os.path.isdir(src) else os.path.dirname(src))
        out = os.path.join(dst, rel)
        os.makedirs(os.path.dirname(out), exist_ok=True)
        open(out, 'wb').write(data)
        print("OK ver=%d key=%s %s (%d字节)" % (ver, key.hex(), rel, len(data)))
        ok += 1
    print("完成 %d 个" % ok)

if __name__ == '__main__':
    main()
