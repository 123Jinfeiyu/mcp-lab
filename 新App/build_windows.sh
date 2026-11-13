#!/bin/sh
# 新App 一键构建 (Windows / Git Bash 版): aapt2 + javac + d8 + zipalign + apksigner
# 用法: sh build_windows.sh [32|64|both]
#   32(默认) = armeabi-v7a, 含原版 PaddleOCR 插件与模型 (约14MB)
#   64       = arm64-v8a, 只有宿主(不带OCR, 体积小; OCR接口会安全降级)
#   both     = 双架构通用包: armeabi-v7a(含OCR) + arm64-v8a(仅宿主)
#              真机(32/64位ARM)与x86模拟器都能装, 装不上OCR的机型自动降级
#
# 依赖路径(按本机实际安装位置写死, 如有变动改这里):
#   SDK  = C:\Users\Administrator\AppData\Local\Android\Sdk  (build-tools 36.1.0 + platforms/android-36)
#   JAVA = D:\1\jbr  (Android Studio 自带 JBR 21, 含 javac/keytool)
set -e
export LANG=C.UTF-8
export LC_ALL=C.UTF-8

SDK="/c/Users/Administrator/AppData/Local/Android/Sdk"
BT="$SDK/build-tools/36.1.0"
AJ="$SDK/platforms/android-36/android.jar"
export JAVA_HOME="D:\\1\\jbr"
export PATH="/d/1/jbr/bin:$PATH"
JAVAC=javac
KEYTOOL=keytool
AAPT2="$BT/aapt2.exe"
D8="$BT/d8.bat"
ZIPALIGN="$BT/zipalign.exe"
APKSIGNER="$BT/apksigner.bat"

ROOT=$(cd "$(dirname "$0")" && pwd)
# 注意: aapt2 等原生 exe 收到中文绝对路径会乱码, 统一 cd 进项目后用相对路径
cd "$ROOT"
KS="$HOME/.android/debug.keystore"
OUT=build
V=${1:-32}
if [ "$V" = "64" ]; then
  ABI=arm64-v8a
  APKNAME=新App-arm64-debug.apk
  PACK_OCR=0
elif [ "$V" = "both" ]; then
  ABI=both
  APKNAME=新App-通用-debug.apk
  PACK_OCR=1
else
  ABI=armeabi-v7a
  APKNAME=新App-debug.apk
  PACK_OCR=1
fi

rm -rf "$OUT"
mkdir -p "$OUT/gen" "$OUT/classes" "$OUT/dex"

echo "== 1/5 编译资源 =="
"$AAPT2" compile --dir res -o "$OUT/res.zip"
"$AAPT2" link -o "$OUT/base.apk" -I "$AJ" \
  --manifest AndroidManifest.xml \
  --java "$OUT/gen" -R "$OUT/res.zip" --auto-add-overlay

echo "== 2/5 javac =="
"$JAVAC" -encoding UTF-8 -source 8 -target 8 -nowarn -bootclasspath "$AJ" \
  -d "$OUT/classes" $(find src "$OUT/gen" -name "*.java")

echo "== 3/5 d8 =="
"$D8" --min-api 24 --lib "$AJ" \
  --output "$OUT/dex" $(find "$OUT/classes" -name "*.class")

echo "== 4/5 打包 ($ABI) =="
PY=python3
command -v python3 >/dev/null 2>&1 || PY=python
# Python 能正确处理中文参数, ROOT 用 Windows 风格绝对路径传入
WROOT=$(cygpath -w "$ROOT")
"$PY" - "$OUT" "$WROOT" "$ABI" "$PACK_OCR" <<'PY'
import sys, os, zipfile, shutil
out, root, abi, pack_ocr = sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4] == '1'
dst = os.path.join(out, "unsigned.apk")
shutil.copy(os.path.join(out, "base.apk"), dst)
z = zipfile.ZipFile(dst, "a", zipfile.ZIP_DEFLATED)
z.write(os.path.join(out, "dex", "classes.dex"), "classes.dex")
for f in os.listdir(os.path.join(root, "assets", "lua")):
    p = os.path.join(root, "assets", "lua", f)
    z.write(p, "assets/lua/" + f)
if pack_ocr:
    for f in os.listdir(os.path.join(root, "assets", "ocr")):
        p = os.path.join(root, "assets", "ocr", f)
        z.write(p, "assets/ocr/" + f)
libdirs = []
if abi == "both":
    libdirs = [("armeabi-v7a", os.path.join(root, "lib", "armeabi-v7a")),
               ("arm64-v8a", os.path.join(root, "lib64", "arm64-v8a"))]
else:
    d = os.path.join(root, "lib", abi) if abi == "armeabi-v7a" else os.path.join(root, "lib64", abi)
    libdirs = [(abi, d)]
for name, d in libdirs:
    if not os.path.isdir(d):
        print("skip missing lib dir:", d)
        continue
    for f in os.listdir(d):
        z.write(os.path.join(d, f), "lib/" + name + "/" + f)
z.close()
print("packed:", dst, "abi=", abi, "ocr=", pack_ocr)
PY

echo "== 5/5 对齐+签名 =="
"$ZIPALIGN" -f 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"
if [ ! -f "$KS" ]; then
  mkdir -p "$(dirname "$KS")"
  "$KEYTOOL" -genkeypair -keystore "$KS" -storepass android -keypass android \
    -alias androiddebugkey -dname "CN=Android Debug,O=Android,C=US" \
    -keyalg RSA -keysize 2048 -validity 10000
fi
"$APKSIGNER" sign --ks "$KS" --ks-pass pass:android --key-pass pass:android \
  --out "$OUT/$APKNAME" "$OUT/aligned.apk"

mkdir -p "$ROOT/dist"
cp "$OUT/$APKNAME" "$ROOT/dist/$APKNAME"
ls -la "$OUT/$APKNAME" "$ROOT/dist/$APKNAME"
echo "构建完成 ($ABI)"
