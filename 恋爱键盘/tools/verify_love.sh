#!/bin/sh
# 恋爱键盘 一键验证 (设备已连接时运行)
#
#   LK_KEY=你的智谱Key sh tools/verify_love.sh          # 全流程(含真实AI调用)
#   sh tools/verify_love.sh                              # 不带Key: 只验安装/权限/悬浮窗/分类提示
#
# 验证内容:
#   1. 安装 APK + 授予悬浮窗/无障碍权限
#   2. 启动悬浮窗面板
#   3. 写入测试消息到剪贴板 -> 读取 -> 生成回复 -> 看分类结果
export MSYS_NO_PATHCONV=1
ROOT=$(cd "$(dirname "$0")/.." && pwd)
ADB="${ADB:-/c/Users/Administrator/AppData/Local/Android/Sdk/platform-tools/adb.exe}"
PKG=com.sixzs.lovekb
REC="$PKG/.LoveReceiver"
APK="$ROOT/dist/恋爱键盘-debug.apk"

DEV=$("$ADB" devices | awk '$2=="device"{print $1; exit}')
if [ -z "$DEV" ]; then echo "没有可用设备, 先启动模拟器并连上 adb"; exit 1; fi
echo "== 设备: $DEV =="

echo "== 1. 安装 =="
"$ADB" -s "$DEV" install -r "$APK" 2>&1 | tail -1
"$ADB" -s "$DEV" shell appops set $PKG SYSTEM_ALERT_WINDOW allow
"$ADB" -s "$DEV" shell settings put secure enabled_accessibility_services $PKG/$PKG.AccessService
"$ADB" -s "$DEV" shell settings put secure accessibility_enabled 1

if [ -n "$LK_KEY" ]; then
  echo "== 2. 写入 API Key 与风格 =="
  "$ADB" -s "$DEV" shell am broadcast -n "$REC" -a $PKG.SET_CONFIG --es glmKey "$LK_KEY" --es style "温柔一点, 口语化" | tail -1
else
  echo "== 2. 跳过 Key 配置(未设置 LK_KEY, 无法验证真实AI调用) =="
fi

echo "== 3. 启动悬浮窗 =="
"$ADB" -s "$DEV" shell am broadcast -n "$REC" -a $PKG.START_PANEL | tail -1
sleep 3

echo "== 4. 写入测试消息并读取 =="
MSG="你今天穿的这件外套好好看呀 是在哪买的"
"$ADB" -s "$DEV" shell am broadcast -n "$REC" -a $PKG.SEND_CLIP --es text "$MSG" | tail -1
"$ADB" -s "$DEV" shell am broadcast -n "$REC" -a $PKG.READ_CLIP | tail -1
sleep 2

echo "== 5. 触发生成 =="
"$ADB" -s "$DEV" shell am broadcast -n "$REC" -a $PKG.READ_CLIP | tail -1
sleep 12

echo "== 6. 日志 =="
"$ADB" -s "$DEV" logcat -d -s LOVEKB 2>/dev/null | tail -15

echo "== 7. 截图 =="
"$ADB" -s "$DEV" exec-out screencap -p > "$ROOT/verify_love.png" 2>/dev/null
echo "截图: $ROOT/verify_love.png"
