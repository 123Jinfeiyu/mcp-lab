#!/usr/bin/env python3
# -*- coding: utf-8 -*-
r"""
【第3步】unluac 输出转义解码 (把 \229\136\135... 解成可读中文)
用法:
  python3 3_转义解码.py <文件或目录>
说明:
  生成 *.decoded.lua, 不覆盖原文件
  仅把双引号字符串里的 \ddd 八进制转义还原为 UTF-8 字节
  注意: 控制字符(如 \10=\n)会还原为原字节, 仅用于阅读
r"""
import sys, os


def decode_octal(data: bytes) -> bytes:
    out = bytearray()
    i, n = 0, len(data)
    while i < n:
        c = data[i]
        if c == 0x5C and i + 2 < n and 0x30 <= data[i + 1] <= 0x39:
            j = i + 1
            num = b''
            while j < n and len(num) < 3 and 0x30 <= data[j] <= 0x39:
                num += bytes([data[j]]); j += 1
            out.append(int(num) & 0xFF)
            i = j
        else:
            out.append(c)
            i += 1
    return bytes(out)


def process(path):
    if path.endswith('.decoded.lua'):
        return None
    raw = open(path, 'rb').read()
    out = path[:-4] + '.decoded.lua' if path.endswith('.lua') else path + '.decoded.lua'
    open(out, 'wb').write(decode_octal(raw))
    return out


def main():
    if len(sys.argv) < 2:
        print(__doc__); sys.exit(1)
    src = sys.argv[1]
    if os.path.isfile(src):
        r = process(src)
        print("->", r); return
    cnt = 0
    for root, _, files in os.walk(src):
        for fn in sorted(files):
            if fn.endswith('.lua') and not fn.endswith('.decoded.lua'):
                process(os.path.join(root, fn)); cnt += 1
    print("解码完成 %d 个文件" % cnt)


if __name__ == '__main__':
    main()
