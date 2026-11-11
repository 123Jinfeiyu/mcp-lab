#!/bin/sh
# 新App 一键构建: aapt2 + javac + d8(r8.jar) + zipalign + apksigner
# 用法: sh build.sh [32|64]
#   32(默认) = armeabi-v7a, 含原版 PaddleOCR 插件与模型 (约13.7MB)
#   64       = arm64-v8a, 只有宿主(不带OCR, 体积小; OCR接口会安全降级)
set -e
export LANG=C.UTF-8
export LC_ALL=C.UTF-8
ROOT=$(cd "$(dirname "$0")" && pwd)
AAPT2=/usr/bin/aapt2
AJ=/usr/lib/android-sdk/platforms/android-34/android.jar
R8JAR=/tmp/cmdline-tools/cmdline-tools/lib/r8.jar
KS=${HOME:-/root}/.android/debug.keystore
OUT=$ROOT/build
V=${1:-32}
if [ "$V" = "64" ]; then
  ABI=arm64-v8a
  APKNAME=新App-arm64-debug.apk
  PACK_OCR=0
else
  ABI=armeabi-v7a
  APKNAME=新App-debug.apk
  PACK_OCR=1
fi

rm -rf "$OUT"
mkdir -p "$OUT/gen" "$OUT/classes" "$OUT/dex"

echo "== 1/5 编译资源 =="
"$AAPT2" compile --dir "$ROOT/res" -o "$OUT/res.zip"
"$AAPT2" link -o "$OUT/base.apk" -I "$AJ" \
  --manifest "$ROOT/AndroidManifest.xml" \
  --java "$OUT/gen" -R "$OUT/res.zip" --auto-add-overlay

echo "== 2/5 javac =="
javac -encoding UTF-8 -source 8 -target 8 -bootclasspath "$AJ" \
  -d "$OUT/classes" $(find "$ROOT/src" "$OUT/gen" -name "*.java")

echo "== 3/5 d8 =="
java -cp "$R8JAR" com.android.tools.r8.D8 --min-api 24 --lib "$AJ" \
  --output "$OUT/dex" $(find "$OUT/classes" -name "*.class")

echo "== 4/5 打包 ($ABI) =="
python3 - "$OUT" "$ROOT" "$ABI" "$PACK_OCR" <<'PY'
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
libdir = os.path.join(root, "lib", abi) if abi == "armeabi-v7a" else os.path.join(root, "lib64", abi)
for f in os.listdir(libdir):
    z.write(os.path.join(libdir, f), "lib/" + abi + "/" + f)
z.close()
print("packed:", dst, "abi=", abi, "ocr=", pack_ocr)
PY

echo "== 5/5 对齐+签名 =="
/usr/bin/zipalign -f 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"
if [ ! -f "$KS" ]; then
  mkdir -p "$(dirname "$KS")"
  keytool -genkeypair -keystore "$KS" -storepass android -keypass android \
    -alias androiddebugkey -dname "CN=Android Debug,O=Android,C=US" \
    -keyalg RSA -keysize 2048 -validity 10000
fi
/usr/bin/apksigner sign --ks "$KS" --ks-pass pass:android --key-pass pass:android \
  --out "$OUT/$APKNAME" "$OUT/aligned.apk"

mkdir -p "$ROOT/dist"
cp "$OUT/$APKNAME" "$ROOT/dist/$APKNAME"
ls -la "$OUT/$APKNAME" "$ROOT/dist/$APKNAME"
echo "构建完成 ($ABI)"
