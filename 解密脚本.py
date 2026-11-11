#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
AI私信助手 (com.nx.dysixzs) 脚本解密工具 —— 单文件版
===============================================================================
功能:
  解开 assets/script.lr (ZIP) 内 脚本/*.lua 的两层加密, 输出标准 Lua 5.3 luac
  可直接交给 unluac / luac -l 继续反编译或检查

用法:
  python3 解密脚本.py <输入文件.lua> [输出文件.luac]
  python3 解密脚本.py <输入目录>   <输出目录>
  python3 解密脚本.py <输入文件> --info     # 只打印结构信息, 不写文件

加密结构 (两层):
  [第一层] 文件外层 (所有版本)
     f[0]=0x86  f[1:4]='LUA'  f[4]=版本(1/2/3)  f[12:42]=30字节密钥K
     head[i]  = ~(f[8+i] ^ K[i])            & 0xFF   (i=0..3)   -> "\x1bLua"
     body[j]  = ~(f[562+j] ^ K[(562+j)%30]) & 0xFF
  [第二层] 定制 Lua dump 字段/代码区 (仅版本3)
     linedefined     (u32) ^= K[0]+1
     lastlinedefined (u32) ^= K[1]+2
     numparams       (u8)  ^= K[2]
     is_vararg       (u8)  ^= K[3]
     maxstacksize    (u8)  ^= K[4]
     codeCount       (u32) ^= K[5]
     code[i] 仅低字节 ^= K[6 + (i % 20)]
  常量表 / 闭包 / protos / debug 段均为标准 5.3 格式, 不加密。

版本差异:
  ver=3  脚本/*.lua            : 两层全加密
  ver=2  插件/PaddLeOCRPluGin.lua: 仅第一层
  ver=1  插件/File.lua         : 算法不同, 本工具不支持
===============================================================================
"""
import sys
import os
import struct

LR_HEADER_SIZE = 562


def load_lr(path):
    """第一层解密: .lua -> (chunk, ver, key)"""
    f = open(path, 'rb').read()
    if len(f) < LR_HEADER_SIZE or f[0] != 0x86 or f[1:4] != b'LUA':
        raise ValueError("不是 LR 加密脚本 (f[0]=%s)" %
                         ("%#x" % f[0] if f else "EOF"))
    ver = f[4]
    key = f[12:42]
    head = bytes(0xFF & ~(f[8 + i] ^ key[i]) for i in range(4))
    body = bytes(0xFF & ~(f[LR_HEADER_SIZE + j] ^ key[(LR_HEADER_SIZE + j) % 30])
                 for j in range(len(f) - LR_HEADER_SIZE))
    return head + body, ver, key


def unmask_v3(buf, key):
    """第二层还原: 解密 ver3 的字段/代码区异或, 返回 bytearray(标准 luac)"""
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
        skip_string()                      # source
        off = pos[0]
        v = struct.unpack_from('<I', b, off)[0] ^ ((key[0] + 1) & 0xFFFFFFFF)
        struct.pack_into('<I', b, off, v)              # linedefined
        v = struct.unpack_from('<I', b, off + 4)[0] ^ ((key[1] + 2) & 0xFFFFFFFF)
        struct.pack_into('<I', b, off + 4, v)          # lastlinedefined
        b[off + 8] ^= key[2]                           # numparams
        b[off + 9] ^= key[3]                           # is_vararg
        b[off + 10] ^= key[4]                          # maxstacksize
        cc_off = off + 11
        cc = struct.unpack_from('<I', b, cc_off)[0] ^ key[5]
        struct.pack_into('<I', b, cc_off, cc)          # codeCount
        pos[0] = cc_off + 4
        for i in range(cc):                            # code[]
            b[pos[0]] ^= key[6 + (i % 20)]
            pos[0] += 4
        nk = u32()
        for _ in range(nk):
            t = u8()
            if t == 0x00:                              # nil
                pass
            elif t == 0x01:                            # boolean
                u8()
            elif t in (0x03, 0x13):                    # number / integer
                raw(8)
            elif t in (0x04, 0x14):                    # short/long string
                size = u8()
                if size == 0xFF:
                    ln = u32()
                    raw(ln - 1)
                elif size:
                    raw(size - 1)
            else:
                raise ValueError("未知常量tag %#x @%#x" % (t, pos[0] - 1))
        upc = u32()
        raw(2 * upc)                                   # upvalues
        pc = u32()
        for _ in range(pc):
            func()                                     # protos
        n1 = u32()
        raw(4 * n1)                                    # lineinfo (int32*n)
        nloc = u32()
        for _ in range(nloc):
            skip_string()
            raw(8)                                     # startpc/endpc
        nup = u32()
        for _ in range(nup):
            skip_string()                              # upvalue names
    if raw(4) != b'\x1bLua':
        raise ValueError("解密后不是 Lua 签名")
    u8()                                               # version
    u8()                                               # format
    raw(6)                                             # LUAC_DATA
    raw(5)                                             # checksizes
    raw(8)                                             # LUAC_INT
    raw(8)                                             # LUAC_NUM
    u8()                                               # main nupvalues
    func()
    if pos[0] != len(b):
        raise ValueError("尾部剩余 %#x 字节" % (len(b) - pos[0]))
    return b


def to_standard(path):
    """完整解密: 返回 (标准luac字节, ver)"""
    chunk, ver, key = load_lr(path)
    if ver == 3:
        return bytes(unmask_v3(chunk, key)), ver
    if ver == 2:
        return chunk, ver
    raise NotImplementedError("ver=%d 算法不同, 暂不支持" % ver)


def print_info(path):
    chunk, ver, key = load_lr(path)
    print("文件:", path)
    print("版本: ver=%d   第一层解密长度=%d" % (ver, len(chunk)))
    print("密钥: %s" % key.hex())
    if ver != 3:
        print("(非 ver3, 无第二层加密)")
    data, _ver2 = to_standard(path)
    b = data
    pos = [0x21]

    def u8():
        v = b[pos[0]]
        pos[0] += 1
        return v

    def u32():
        v = struct.unpack_from('<I', b, pos[0])[0]
        pos[0] += 4
        return v

    def raw(n):
        pos[0] += n

    def skip_string():
        size = u8()
        if size == 0:
            return
        if size == 0xFF:
            ln = u32()
            raw(ln - 1)
        else:
            raw(size - 1)

    total = [0]

    def func(depth=0):
        total[0] += 1
        ind = "  " * depth
        skip_string()
        line, last = u32(), u32()
        np, va, ms = u8(), u8(), u8()
        cc = u32()
        raw(4 * cc)
        nk = u32()
        for _ in range(nk):
            t = u8()
            if t == 0x00:
                pass
            elif t == 0x01:
                u8()
            elif t in (0x03, 0x13):
                raw(8)
            else:
                skip_string()
        upc = u32()
        raw(2 * upc)
        pc = u32()
        print("%s函数#%d 行%d-%d 参数%d 变参%d 栈%d 指令%d 常量%d 子函数%d"
              % (ind, total[0], line, last, np, va, ms, cc, nk, pc))
        for _ in range(pc):
            func(depth + 1)
        n1 = u32()
        raw(4 * n1)
        nloc = u32()
        for _ in range(nloc):
            skip_string()
            raw(8)
        nup = u32()
        for _ in range(nup):
            skip_string()
    u8()
    func()
    print("总函数数: %d   解析结束于 %#x/%#x" % (total[0], pos[0], len(b)))


def main():
    args = sys.argv[1:]
    if not args:
        print(__doc__)
        return
    src = args[0]
    if len(args) >= 2 and args[1] == "--info":
        print_info(src)
        return
    dst = args[1] if len(args) >= 2 else None
    if os.path.isfile(src):
        data, ver = to_standard(src)
        if dst is None:
            dst = os.path.splitext(src)[0] + ".std.luac"
        with open(dst, 'wb') as f:
            f.write(data)
        print("OK ver=%d %s -> %s (%d字节)" % (ver, src, dst, len(data)))
        return
    outroot = dst or (src.rstrip('/\\') + "_std")
    n_ok = 0
    n_skip = 0
    for root, _, files in os.walk(src):
        for fn in sorted(files):
            if not fn.lower().endswith('.lua'):
                continue
            p = os.path.join(root, fn)
            rel = os.path.relpath(p, src)
            out = os.path.join(outroot, os.path.splitext(rel)[0] + ".luac")
            os.makedirs(os.path.dirname(out), exist_ok=True)
            try:
                data, ver = to_standard(p)
            except Exception as e:
                print("跳过 %s (%s)" % (rel, e))
                n_skip += 1
                continue
            with open(out, 'wb') as f:
                f.write(data)
            n_ok += 1
    print("完成: 成功 %d, 跳过 %d, 输出目录 %s" % (n_ok, n_skip, outroot))


if __name__ == '__main__':
    main()
