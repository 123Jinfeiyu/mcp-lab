#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
重建 script.lr (标准ZIP): 保留原条目顺序/原始文件名字节/压缩方式,
替换或新增条目内容。
用法: python3 rebuild_script_lr.py <原script.lr> <输出.lr> <原名=新文件> [+新名=新文件] ...
"""
import sys
import os
import time
import zlib
import zipfile
import struct


def to_dos(dt):
    y, m, d, H, M, S = dt
    if y < 1980:
        y = 1980
    return ((H << 11) | (M << 5) | (S // 2), ((y - 1980) << 9) | (m << 5) | d)


def main():
    orig, out_path = sys.argv[1], sys.argv[2]
    repl = {}
    adds = []
    for a in sys.argv[3:]:
        name, path = a.split('=', 1)
        if name.startswith('+'):
            adds.append((name[1:], path))
        else:
            repl[name] = open(path, 'rb').read()
    zin = zipfile.ZipFile(orig)
    out = open(out_path, 'wb')
    central = []
    offset = 0

    def raw_name(i):
        try:
            return i.filename.encode('cp437')
        except UnicodeEncodeError:
            return i.filename.encode('utf-8')

    def real_name(raw):
        try:
            return raw.decode('utf-8')
        except UnicodeDecodeError:
            return raw.decode('cp437')

    def write_entry(name_raw, data, compress_type, date_time, external_attr, flag_bits):
        nonlocal offset
        if compress_type == 8:
            co = zlib.compressobj(6, zlib.DEFLATED, -15)
            comp = co.compress(data) + co.flush()
        else:
            comp = data
        crc = zlib.crc32(data) & 0xffffffff
        dostime, dosdate = to_dos(date_time)
        lh = struct.pack('<IHHHHHIIIHH', 0x04034b50, 20, flag_bits,
                         compress_type, dostime, dosdate, crc,
                         len(comp), len(data), len(name_raw), 0)
        out.write(lh)
        out.write(name_raw)
        out.write(comp)
        central.append((name_raw, crc, len(comp), len(data), offset,
                        compress_type, date_time, external_attr, flag_bits))
        offset += len(lh) + len(name_raw) + len(comp)

    for i in zin.infolist():
        rn = raw_name(i)
        name = real_name(rn)
        data = repl.get(name, zin.read(i))
        write_entry(rn, data, i.compress_type, i.date_time, i.external_attr, i.flag_bits)

    now = time.localtime()[:6]
    for name, path in adds:
        data = open(path, 'rb').read()
        write_entry(name.encode('utf-8'), data, 8, now, 0x81a40000, 0)

    cd_start = offset
    for rn, crc, csize, usize, off, ctype, dtime, eattr, fbits in central:
        dostime, dosdate = to_dos(dtime)
        ch = struct.pack('<IHHHHHHIIIHHHHHII', 0x02014b50, 20, 20,
                         fbits, ctype, dostime, dosdate,
                         crc, csize, usize, len(rn), 0, 0, 0, 0,
                         eattr, off)
        out.write(ch)
        out.write(rn)
        offset += len(ch) + len(rn)
    cd_size = offset - cd_start
    out.write(struct.pack('<IHHHHIIH', 0x06054b50, 0, 0,
                          len(central), len(central), cd_size, cd_start, 0))
    out.close()
    print("rebuild OK: %d 条目, 替换 %d 个, 新增 %d 个 -> %s (%d 字节)" %
          (len(central), len(repl), len(adds), out_path, os.path.getsize(out_path)))


if __name__ == '__main__':
    main()
