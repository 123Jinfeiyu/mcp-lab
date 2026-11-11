#!/system/bin/sh
# ==========================================================================
# 批量解密流水线: 原始加密脚本 -> 1层解密 -> 标准luac -> 反编译 -> 转义解码
# 用法: sh 批量解密.sh [原始目录] [输出根目录]
#  默认: 原始目录=../原始加密脚本  输出=../解密产物
# 依赖: python3, java (unluac)
# ==========================================================================
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/.." && pwd)"
SRC="${1:-$ROOT/原始加密脚本}"
OUT="${2:-$ROOT/解密产物}"

mkdir -p "$OUT/1_解密后LuaBinary" "$OUT/2_标准luac" "$OUT/3_反编译源码"

echo "== [1/3] 文件层解密 =="
python3 "$HERE/1_文件解密.py" "$SRC" "$OUT/1_解密后LuaBinary" || exit 1

echo "== [2/3] 转标准 luac =="
python3 "$HERE/2_转标准luac.py" "$SRC" "$OUT/2_标准luac" || exit 1

echo "== [3/3] unluac 反编译 (Java 不支持中文路径, 先复制到 /tmp) =="
# Java 无法加载中文路径 classpath: 复制到 ASCII 临时目录
UC="/tmp/_unluac_classes_$$"
rm -rf "$UC"
cp -r "$HERE/unluac-classes" "$UC"
cd "$OUT/2_标准luac" || exit 1
find . -name '*.luac' | while read f; do
  d="$OUT/3_反编译源码/${f%.luac}.lua"
  mkdir -p "$(dirname "$d")"
  tmp="/tmp/_unluac_$$.luac"
  cp "$f" "$tmp"
  java -cp "$UC" unluac.Main "$tmp" > "$d" 2>/dev/null
  rm -f "$tmp"
done
rm -rf "$UC"

echo "== 转义解码 =="
python3 "$HERE/3_转义解码.py" "$OUT/3_反编译源码"
echo "全部完成 -> $OUT"
