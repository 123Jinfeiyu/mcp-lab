#!/bin/sh
# 恋爱键盘 一键构建 (纯 Java, 无 native 库 -> 任何架构都能装)
# 用法: sh build_windows.sh
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
cd "$ROOT"
KS="$HOME/.android/debug.keystore"
OUT=build
APKNAME=恋爱键盘-debug.apk

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

echo "== 4/5 打包 =="
PY=python3
command -v python3 >/dev/null 2>&1 || PY=python
WROOT=$(cygpath -w "$ROOT")
"$PY" - "$OUT" "$WROOT" <<'PY'
import sys, os, zipfile, shutil
out, root = sys.argv[1], sys.argv[2]
dst = os.path.join(out, "unsigned.apk")
shutil.copy(os.path.join(out, "base.apk"), dst)
z = zipfile.ZipFile(dst, "a", zipfile.ZIP_DEFLATED)
z.write(os.path.join(out, "dex", "classes.dex"), "classes.dex")
z.close()
print("packed:", dst)
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
ls -la "$OUT/$APKNAME"
echo "构建完成 -> dist/$APKNAME"
