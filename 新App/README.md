# AI私信助手本地版（全新App / 自研宿主引擎）

不复用原 APK 的加密 SO；用干净 Lua 5.3.6 做宿主，复用原解密脚本（明文）。

## 构建与产物

```sh
cd /storage/emulated/0/MT2/mcp/新App
sh build.sh 32   # armeabi-v7a + 原版PaddleOCR插件与模型  -> dist/新App-debug.apk (~14MB)
sh build.sh 64   # arm64-v8a (无OCR)                     -> dist/新App-arm64-debug.apk (~177KB)
```

## 目录结构

```
新App/
├── AndroidManifest.xml          权限/活动/服务声明（无障碍/输入法/悬浮窗/前台服务）
├── build.sh                     一键构建（aapt2 + javac + d8 + zipalign + apksigner）
├── res/                         主页（权限+配置照抄原 main.ui）、悬浮窗（照抄原样式）
├── src/com/sixzs/local/
│   ├── MainActivity.java        权限申请 + 配置输入 + 权限向导 + 截图授权
│   ├── FloatService.java        悬浮窗前台服务（可拖动，开始/停止/设置/关闭）
│   ├── AccessService.java       无障碍（节点/手势/截图）
│   ├── NodeUtil.java            nodeLib 实现（findOne/findAll/click/setText/findIn…）
│   ├── Host.java                宿主 API（http/文件/json/md5/节点/OCR/找色）
│   ├── LuaHost.java             Lua 宿主入口 + assets 释放
│   ├── LocalImeService.java     内置输入法（原版同方案，commitText 打字）
│   ├── Ocr.java                 PaddleOCR 插件加载（DexClassLoader + 反射）
│   ├── ScreenCap.java           MediaProjection 截图（API<30）
│   └── Config.java              配置存取
├── jni/jni_host.c               JNI 桥（Lua <-> Java 的 __host.call）
├── assets/lua/                  明文脚本（main/page/lib/HWD/localdata/免登录空壳/prelude）
├── assets/ocr/                  OCR 模型 + 插件 dex（32位包）
├── lib/armeabi-v7a/             32位：Lua宿主 + 原版 OCR 五个 so
└── lib64/arm64-v8a/             64位：仅 Lua宿主
```

## 权限流程（主页）

| 权限 | 申请方式 | 备注 |
|------|----------|------|
| 悬浮窗 | ACTION_MANAGE_OVERLAY_PERMISSION | 悬浮窗服务必需 |
| 无障碍 | ACTION_ACCESSIBILITY_SETTINGS | 系统限制，必须手动开启 |
| 输入法 | ACTION_INPUT_METHOD_SETTINGS + 选择器 | 启用并选中「AI本地输入法」 |
| 电池优化 | ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS | 防后台被杀 |
| 存储 | API30+: 所有文件访问；以下: 读写权限 | 本地数据目录 /sdcard/AI私信助手数据 |
| 截图 | MediaProjection 授权（悬浮窗服务内生效） | API<30 需要；重启后需重新授权 |

主页「权限向导」按 悬浮窗→无障碍→输入法→电池→存储 顺序自动打开缺失项；状态行实时显示 ✓/✗。
（MIUI 的“自启动/后台弹出界面”属系统级开关，应用内无法申请。）

## 已接入功能

1. **脚本明文直用**：`assets/lua/`，`showUIEx`→主页配置注入，开抖音→`launchApp`，
   `/cx` 作者接口由 `localdata.lua` 本地化。
2. **内置输入法**：`LocalImeService`（`BIND_INPUT_METHOD`），`inputText` 顺序：
   无障碍写焦点 → 本输入法 `commitText` → 剪贴板粘贴。
3. **OCR**：直接搬原版 PaddleOCR 插件（dex+5个so+Db/Rec模型），
   `pdocr/ocrEx` = 截图→识别→`{text,score,bounds}`。
4. **颜色找图**：`findMultiColor(l,t,r,b,首色,偏移串,方向,相似度)` 真实现。
5. **截图**：API30+ 无障碍 `takeScreenshot`；API<30 MediaProjection。
6. **流程仿真**：假抖音界面驱动 main.lua，37 步跑到「setText+发送」成功。

## 32/64 位说明

- 32 位：含原版 OCR 插件（原版只有 arm32），功能全；
- 64 位：宿主/脚本/无障碍/输入法/HTTP 全部可用，**OCR 不可用**（插件无 arm64，
  pdocr/ocrEx 返回空 → 蓝V/店铺名/粉丝数分支退化，其余功能正常）。
- 需要 64 位+OCR 的话要替换 OCR 引擎（arm64 PaddleOCR 或 Tesseract/ML Kit）。

## 待办

1. 真机联调：授权 → 保存配置 → 悬浮窗「开始」，按 logcat 修边角
2. 如需 64 位+OCR：换 OCR 引擎
