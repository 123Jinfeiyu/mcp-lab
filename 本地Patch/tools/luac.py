#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Lua 5.3 luac 二进制解析 / 重排工具（支持 32 与 64 位 size_t）

用途: 在 Android arm64 上用 luac 5.3.6 编译源码后, 把 64 位 dump 转成
      App 引擎(32 位)需要的 32 位 size_t 格式; 以及给 strip_debug / 对比工具用。
"""
import struct
import sys

TAG_NIL = 0x00
TAG_BOOL = 0x01
TAG_NUM = 0x03
TAG_INT = 0x13
TAG_SHRSTR = 0x04
TAG_LNGSTR = 0x14

LUA_SIGNATURE = b'\x1bLua'
LUAC_DATA = b'\x19\x93\r\n\x1a\n'
LUAC_INT = 0x5678
LUAC_NUM = 370.5


class Proto:
    __slots__ = ('source', 'linedefined', 'lastlinedefined', 'numparams',
                 'is_vararg', 'maxstacksize', 'code', 'constants', 'upvals',
                 'protos', 'lineinfo', 'locvars', 'upnames')

    def __init__(self):
        self.source = None
        self.linedefined = 0
        self.lastlinedefined = 0
        self.numparams = 0
        self.is_vararg = 0
        self.maxstacksize = 0
        self.code = []
        self.constants = []
        self.upvals = []
        self.protos = []
        self.lineinfo = []
        self.locvars = []
        self.upnames = []


class LuaChunk:
    def __init__(self):
        self.version = 0x53
        self.format = 0
        self.sizes = [4, 4, 4, 8, 8]
        self.luac_int = LUAC_INT
        self.luac_num = LUAC_NUM
        self.nupvalues = 0
        self.main = None


def _str_size_width(sizes):
    return sizes[1]


class Reader:
    def __init__(self, b, sizes):
        self.b = b
        self.p = 0
        self.sw = _str_size_width(sizes)

    def u8(self):
        v = self.b[self.p]
        self.p += 1
        return v

    def i32(self):
        v = struct.unpack_from('<i', self.b, self.p)[0]
        self.p += 4
        return v

    def u32(self):
        v = struct.unpack_from('<I', self.b, self.p)[0]
        self.p += 4
        return v

    def u64(self):
        v = struct.unpack_from('<Q', self.b, self.p)[0]
        self.p += 8
        return v

    def raw(self, n):
        v = self.b[self.p:self.p + n]
        self.p += n
        return v

    def string(self):
        size = self.u8()
        if size == 0xFF:
            if self.sw == 4:
                size = self.u32()
            else:
                size = self.u64()
        if size == 0:
            return None
        return self.raw(size - 1)

    def string_raw(self, s):
        if s is None:
            return b'\x00'
        size = len(s) + 1
        if size < 0xFF:
            return bytes([size]) + s
        out = b'\xff' + (struct.pack('<I', size) if self.sw == 4 else struct.pack('<Q', size))
        return out + s


class Writer:
    def __init__(self, sizes):
        self.out = bytearray()
        self.sw = _str_size_width(sizes)

    def u8(self, v):
        self.out.append(v & 0xFF)

    def i32(self, v):
        self.out += struct.pack('<i', v)

    def u32(self, v):
        self.out += struct.pack('<I', v & 0xFFFFFFFF)

    def raw(self, b):
        self.out += b

    def string(self, s):
        if s is None:
            self.u8(0)
            return
        size = len(s) + 1
        if size < 0xFF:
            self.u8(size)
        else:
            self.u8(0xFF)
            if self.sw == 4:
                self.u32(size)
            else:
                self.out += struct.pack('<Q', size)
        self.raw(s)


def parse(data):
    assert data[:4] == LUA_SIGNATURE, "not a luac chunk"
    ch = LuaChunk()
    ch.version = data[4]
    ch.format = data[5]
    assert data[6:12] == LUAC_DATA, "bad LUAC_DATA"
    ch.sizes = list(data[12:17])
    ch.luac_int = struct.unpack_from('<q', data, 17)[0]
    ch.luac_num = struct.unpack_from('<d', data, 25)[0]
    r = Reader(data, ch.sizes)
    r.p = 33
    ch.nupvalues = r.u8()

    def read_proto():
        f = Proto()
        f.source = r.string()
        f.linedefined = r.i32()
        f.lastlinedefined = r.i32()
        f.numparams = r.u8()
        f.is_vararg = r.u8()
        f.maxstacksize = r.u8()
        n = r.i32()
        f.code = [r.u32() for _ in range(n)]
        nk = r.i32()
        for _ in range(nk):
            t = r.u8()
            if t == TAG_NIL:
                f.constants.append((t, None))
            elif t == TAG_BOOL:
                f.constants.append((t, r.u8()))
            elif t in (TAG_NUM, TAG_INT):
                f.constants.append((t, r.raw(8)))
            elif t in (TAG_SHRSTR, TAG_LNGSTR):
                f.constants.append((t, r.string()))
            else:
                raise ValueError("bad const tag %#x at %#x" % (t, r.p - 1))
        nu = r.i32()
        f.upvals = [(r.u8(), r.u8()) for _ in range(nu)]
        np = r.i32()
        f.protos = [read_proto() for _ in range(np)]
        n1 = r.i32()
        f.lineinfo = [r.i32() for _ in range(n1)]
        nl = r.i32()
        for _ in range(nl):
            name = r.string()
            sp = r.i32()
            ep = r.i32()
            f.locvars.append((name, sp, ep))
        nun = r.i32()
        f.upnames = [r.string() for _ in range(nun)]
        return f

    ch.main = read_proto()
    assert r.p == len(data), "trailing bytes: %#x/%#x" % (r.p, len(data))
    return ch


def serialize(ch, sizes=None, strip_debug=False):
    if sizes is None:
        sizes = ch.sizes
    w = Writer(sizes)
    w.raw(LUA_SIGNATURE)
    w.u8(ch.version)
    w.u8(ch.format)
    w.raw(LUAC_DATA)
    for s in sizes:
        w.u8(s)
    w.out += struct.pack('<q', ch.luac_int)
    w.out += struct.pack('<d', ch.luac_num)
    w.u8(ch.nupvalues)

    def write_proto(f):
        if strip_debug:
            w.string(None)
        else:
            w.string(f.source)
        w.i32(f.linedefined)
        w.i32(f.lastlinedefined)
        w.u8(f.numparams)
        w.u8(f.is_vararg)
        w.u8(f.maxstacksize)
        w.i32(len(f.code))
        for c in f.code:
            w.u32(c)
        w.i32(len(f.constants))
        for t, v in f.constants:
            w.u8(t)
            if t == TAG_BOOL:
                w.u8(v)
            elif t in (TAG_NUM, TAG_INT):
                w.raw(v)
            elif t in (TAG_SHRSTR, TAG_LNGSTR):
                w.string(v)
        w.i32(len(f.upvals))
        for a, b in f.upvals:
            w.u8(a)
            w.u8(b)
        w.i32(len(f.protos))
        for p in f.protos:
            write_proto(p)
        if strip_debug:
            w.i32(0)
            w.i32(0)
            w.i32(0)
        else:
            w.i32(len(f.lineinfo))
            for x in f.lineinfo:
                w.i32(x)
            w.i32(len(f.locvars))
            for name, sp, ep in f.locvars:
                w.string(name)
                w.i32(sp)
                w.i32(ep)
            w.i32(len(f.upnames))
            for name in f.upnames:
                w.string(name)

    write_proto(ch.main)
    return bytes(w.out)


def iter_protos(f, path='main'):
    yield path, f
    for i, p in enumerate(f.protos):
        yield from iter_protos(p, path + '/%d' % i)


def code_to_text(f):
    lines = []
    K = f.constants
    for i, ins in enumerate(f.code):
        op = ins & 0x3F
        lines.append('%4d %08x' % (i, ins))
    return lines


def main():
    if len(sys.argv) < 3:
        print("用法: luac.py <输入.luac> <输出.luac> [--to32|--strip|--info]")
        return
    src, dst = sys.argv[1], sys.argv[2]
    data = open(src, 'rb').read()
    ch = parse(data)
    if '--info' in sys.argv:
        for path, f in iter_protos(ch.main):
            print("%s cc=%d nk=%d protos=%d ld=%d" % (path, len(f.code), len(f.constants), len(f.protos), f.linedefined))
        return
    if '--to32' in sys.argv:
        sizes = [4, 4, 4, 8, 8]
    elif '--strip' in sys.argv:
        sizes = [4, 4, 4, 8, 8]
    else:
        sizes = None
    out = serialize(ch, sizes=sizes, strip_debug=('--strip' in sys.argv))
    open(dst, 'wb').write(out)
    print("OK %s -> %s (%d -> %d bytes, sizes=%s)" % (src, dst, len(data), len(out), sizes or ch.sizes))


if __name__ == '__main__':
    main()
