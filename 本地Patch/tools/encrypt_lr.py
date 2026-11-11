#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
把标准 Lua 5.3 luac (32位格式) 重新加密回引擎的 .lr .lua 格式。

两层加密与解密是同一运算(异或自逆):
  第二层: 对 proto 的 行号/参数/栈/指令数/指令低字节 再异或一遍 K
  第一层: f[8+i]=~(chunk[i])^K[i] ; f[562+j]=~(chunk[4+j])^K[(562+j)%30]

用法:
  python3 encrypt_lr.py <标准.luac> <原始加密.lua> <输出.lua> [--info]
原始加密.lua 仅用于取出 562 字节文件头和 30 字节密钥 K。
"""
import sys
import struct

LR_HEADER_SIZE = 562


def unmask_v3(buf, key):
    b = bytearray(buf)
    pos = [0]

    def u8():
        v = b[pos[0]]
        pos[0] += 1
        return v

    def u32():
        v = struct.unpack_from('<I', b, pos[0])[0]
        pos[0] += 4
        return v

    def raw(n):
        v = bytes(b[pos[0]:pos[0] + n])
        pos[0] += n
        return v

    def skip_string():
        size = u8()
        if size == 0:
            return
        if size == 0xFF:
            ln = u32()
            raw(ln - 1)
        else:
            raw(size - 1)

    def func():
        skip_string()
        off = pos[0]
        v = struct.unpack_from('<I', b, off)[0] ^ ((key[0] + 1) & 0xFFFFFFFF)
        struct.pack_into('<I', b, off, v)
        v = struct.unpack_from('<I', b, off + 4)[0] ^ ((key[1] + 2) & 0xFFFFFFFF)
        struct.pack_into('<I', b, off + 4, v)
        b[off + 8] ^= key[2]
        b[off + 9] ^= key[3]
        b[off + 10] ^= key[4]
        cc_off = off + 11
        cc = struct.unpack_from('<I', b, cc_off)[0] ^ key[5]
        struct.pack_into('<I', b, cc_off, cc)
        pos[0] = cc_off + 4
        for i in range(cc):
            b[pos[0]] ^= key[6 + (i % 20)]
            pos[0] += 4
        nk = u32()
        for _ in range(nk):
            t = u8()
            if t == 0x00:
                pass
            elif t == 0x01:
                u8()
            elif t in (0x03, 0x13):
                raw(8)
            elif t in (0x04, 0x14):
                size = u8()
                if size == 0xFF:
                    ln = u32()
                    raw(ln - 1)
                elif size:
                    raw(size - 1)
            else:
                raise ValueError("未知常量tag %#x @%#x" % (t, pos[0] - 1))
        upc = u32()
        raw(2 * upc)
        pc = u32()
        for _ in range(pc):
            func()
        n1 = u32()
        raw(4 * n1)
        nloc = u32()
        for _ in range(nloc):
            skip_string()
            raw(8)
        nup = u32()
        for _ in range(nup):
            skip_string()

    if raw(4) != b'\x1bLua':
        raise ValueError("输入不是 Lua 5.3 标准签名")
    u8()
    u8()
    raw(6)
    raw(5)
    raw(8)
    raw(8)
    u8()
    func()
    if pos[0] != len(b):
        raise ValueError("尾部剩余 %#x 字节" % (len(b) - pos[0]))
    return bytes(b)


def encrypt_layer1(header, chunk, key):
    out = bytearray(header)
    for i in range(4):
        out[8 + i] = (0xFF & ~(chunk[i] ^ key[i]))
    body = chunk[4:]
    for j, x in enumerate(body):
        out.append(0xFF & ~(x ^ key[(LR_HEADER_SIZE + j) % 30]))
    return bytes(out)


def mask_v3(buf, key):
    """与 unmask_v3 同为异或变换, 但按明文控制流解析(用于加密方向)。"""
    b = bytearray(buf)
    pos = [0]

    def u8():
        v = b[pos[0]]
        pos[0] += 1
        return v

    def u32():
        v = struct.unpack_from('<I', b, pos[0])[0]
        pos[0] += 4
        return v

    def raw(n):
        v = bytes(b[pos[0]:pos[0] + n])
        pos[0] += n
        return v

    def skip_string():
        size = u8()
        if size == 0:
            return
        if size == 0xFF:
            ln = u32()
            raw(ln - 1)
        else:
            raw(size - 1)

    def func():
        skip_string()
        off = pos[0]
        plain_ld = struct.unpack_from('<I', b, off)[0]
        struct.pack_into('<I', b, off, plain_ld ^ ((key[0] + 1) & 0xFFFFFFFF))
        plain_ll = struct.unpack_from('<I', b, off + 4)[0]
        struct.pack_into('<I', b, off + 4, plain_ll ^ ((key[1] + 2) & 0xFFFFFFFF))
        b[off + 8] ^= key[2]
        b[off + 9] ^= key[3]
        b[off + 10] ^= key[4]
        cc_off = off + 11
        plain_cc = struct.unpack_from('<I', b, cc_off)[0]
        struct.pack_into('<I', b, cc_off, plain_cc ^ key[5])
        pos[0] = cc_off + 4
        for i in range(plain_cc):
            b[pos[0]] ^= key[6 + (i % 20)]
            pos[0] += 4
        nk = u32()
        for _ in range(nk):
            t = u8()
            if t == 0x00:
                pass
            elif t == 0x01:
                u8()
            elif t in (0x03, 0x13):
                raw(8)
            elif t in (0x04, 0x14):
                size = u8()
                if size == 0xFF:
                    ln = u32()
                    raw(ln - 1)
                elif size:
                    raw(size - 1)
            else:
                raise ValueError("未知常量tag %#x @%#x" % (t, pos[0] - 1))
        upc = u32()
        raw(2 * upc)
        pc = u32()
        for _ in range(pc):
            func()
        n1 = u32()
        raw(4 * n1)
        nloc = u32()
        for _ in range(nloc):
            skip_string()
            raw(8)
        nup = u32()
        for _ in range(nup):
            skip_string()

    if raw(4) != b'\x1bLua':
        raise ValueError("输入不是 Lua 5.3 标准签名")
    u8()
    u8()
    raw(6)
    raw(5)
    raw(8)
    raw(8)
    u8()
    func()
    if pos[0] != len(b):
        raise ValueError("尾部剩余 %#x 字节" % (len(b) - pos[0]))
    return bytes(b)


def main():
    src, orig, dst = sys.argv[1], sys.argv[2], sys.argv[3]
    chunk = open(src, 'rb').read()
    origdata = open(orig, 'rb').read()
    if len(origdata) < LR_HEADER_SIZE or origdata[0] != 0x86:
        raise ValueError("原始文件不是 LR 加密脚本")
    header = origdata[:LR_HEADER_SIZE]
    ver = header[4]
    key = header[12:42]
    if ver == 3:
        chunk = mask_v3(chunk, key)
    elif ver == 2:
        pass
    else:
        raise ValueError("不支持 ver=%d" % ver)
    out = encrypt_layer1(header, chunk, key)
    open(dst, 'wb').write(out)
    print("OK ver=%d %s -> %s (%d字节, 头562 + 体%d)" % (ver, src, dst, len(out), len(chunk) - 4))


if __name__ == '__main__':
    main()
