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

## 脚本热加载与可视化调试（2026-11-12 新增）

改脚本**不用重新打包 APK**，调试循环：

```
改 assets/lua/xxx.lua（PC 上随便用什么编辑器）
  → sh dev.sh push          （推到 /sdcard/AI私信助手数据/lua/）
  → 悬浮窗点「重启」         （重新读取最新脚本）
  → 悬浮窗日志区直接看执行过程（也可 sh dev.sh log 看 logcat）
```

- 脚本加载优先级：`/sdcard/AI私信助手数据/lua/*.lua`（覆盖）> APK 内置 assets。只有被推送过的文件会被覆盖，其余仍用内置版
- 首次运行会把 assets 释放到内部存储并保留，之后不再覆盖（`extractLua` 只在文件不存在时释放）
- 悬浮窗日志区：10 行可滚动、带时间戳、自动滚到底，`print/hudlog/toast` 及宿主异常全进这里
- 悬浮窗「开始」按钮运行中变「重启」= 一键热重启
- `sh dev.sh push 文件` 可只推单个文件；`sh dev.sh install` 装最新 APK
- 前提：App 已授予「所有文件访问」权限（主页权限向导里的存储项）；没有权限时自动退回内置脚本，不会崩

### 涉及改动

| 文件 | 改动 |
|------|------|
| `src/com/sixzs/local/LuaHost.java` | `overlayLua()` sdcard 覆盖加载 + `restart()` 一键重启 + 主脚本从本地文件读 |
| `src/com/sixzs/local/Logx.java` | 时间戳环形日志缓冲（80 行），推给悬浮窗 |
| `src/com/sixzs/local/FloatService.java` | 日志区可滚动自动滚底；「重启」按钮逻辑 |
| `res/layout/float_panel.xml` | 日志区 300x140dp、10 行、竖向滚动 |
| `dev.sh` | PC 端联调工具（push/log/install） |

## 可视化联调台（2026-11-12 新增）

**双击 `tools/monitor.bat`**，同时启动两个窗口：

1. **scrcpy 投屏**（`tools/scrcpy/`，v5.0.1 便携版）：实时手机画面，低延迟，可直接用鼠标操作手机
2. **网页控制台** `http://127.0.0.1:8123`（`tools/dashboard.py`，纯 Python 标准库，无依赖）：
   - 手机画面轮询显示（约 2 帧/秒，PNG）
   - 脚本日志实时滚动（adb logcat -s DMSX，自动滚底）
   - 按钮：**推送脚本到手机** / **安装APK** / 截图 / 清空日志

只想要控制台不投屏：`python tools/dashboard.py`；多设备时用 `ASERIAL=设备号 python tools/dashboard.py` 指定。

完整调试循环（全 GUI）：

```
编辑器改 assets/lua/xxx.lua
  → 网页控制台点「推送脚本到手机」
  → 手机悬浮窗点「重启」
  → 网页日志区 / scrcpy 画面 实时观察效果
```

已实测：模拟器（emulator-5554）+ 抖音前台场景，截屏/日志/推送全部通过。

---

## ③ AI 智能控制（智谱 GLM 看屏操作）

设置页新增「③ AI 智能控制」区，App 可由**智谱 GLM 视觉模型**驱动（Open-AutoGLM 思路，但完全跑在手机本地，不依赖电脑）：

| 配置项 | 说明 |
|--------|------|
| 智谱 API Key | open.bigmodel.cn 的 Key，本地保存（`glmKey`） |
| 任务描述 | 自然语言，如「打开抖音，搜索美食，进入第一个用户主页」 |
| 模型 | 默认 `glm-4.5v`（可换 glm-4v-plus 等视觉模型） |
| 最多步数 | 默认 15 步，防跑飞 |

**两种启动方式**：设置页「立即启动AI执行」；悬浮窗「AI」按钮（跑/停切换，随时可中断）。
**「截屏问一次(不执行)」** 是安全测试模式：只截图问模型并把回答显示在页面上，**不执行任何动作**，用来验证 Key、模型、截图通道是否正常。

### 执行链路

```
截图(API30+ 无障碍 takeScreenshot / API<30 MediaProjection)
  → 缩放后 base64
  → 附上无障碍节点树的可操作元素清单(文本 + 0-1000 归一化坐标)
  → GLM 输出「思考 + 动作」
  → 动作解析(tap/swipe/text/back/home/launch/wait/finished)
  → 无障碍手势 / 内置输入法执行
```

关键设计：
- **坐标用 0-1000 归一化**（GLM-4V 原生 grounding 约定），不受图片缩放影响
- **点击优先用元素清单里的精确坐标**，让模型按名字选控件而不是目测像素
- 模型只描述不动手时，**自动追问一次**要求给出动作
- 每步思考/动作/结果都进悬浮窗日志区
- AI 看到的画面存到 `/sdcard/Android/data/com.sixzs.local/files/ai_last.jpg`，方便核对

### 使用前置权限

| 权限 | 为什么 | 怎么给 |
|------|--------|--------|
| 悬浮窗 | 悬浮窗是运行状态与日志面板 | 设置页「悬浮窗权限」 |
| 无障碍服务 | 点击/滑动手势 + 元素树 | 设置页「无障碍服务」 |
| 截图授权 | API<30 必须靠 MediaProjection 拿画面 | 设置页「截图授权」→ 系统弹窗点「立即开始」 |
| 输入法（本机） | AI 输入中文 | 设置页「输入法(启用并选中)」 |

> ⚠️ 授权截图后系统弹「截取屏幕」确认框，**要点「立即开始」**（点「取消」不生效，AI 会提示「未获取到画面」）。

### Lua 脚本也能问 AI

宿主新增 `aiAsk(问题)` 接口：

```lua
local advice = aiAsk("当前屏幕上有没有可私信的用户入口? 只回答有或没有")
```

### 已知坑（已修复）

- `AccessibilityService.TakeScreenshotCallback` 是 API 30+ 的类，直接写在 `AccessService` 里会让**整个无障碍服务在 Android 10 以下加载失败** → 已隔离到 `ScreenShot30` 并用反射调用
- 缺悬浮窗权限时 `addView` 抛异常会**拖垮整个 FloatService**（截图授权、脚本执行全废）→ 已改为捕获并继续
- 模型只描述不动手 → 提示词加铁律 + 自动追问

---

## ④ 评论区舆情与风控 + 意向线索

面向「自己作品下的评论区」做两件事：**识别违规引流广告**（风险）和 **识别有互动态向的用户**（意向）。

### 使用流程

```
① 设置页「搜索关键词」填关键词(如 古风舞) → 点「生成任务」
   → 点「立即启动AI执行」：AI 自动 打开抖音→搜索→打开第一个视频→进入评论区
② 悬浮窗点「风控扫描」：扫描当前前台界面的评论 → GLM 批量判定
   输出: 风险等级 / 意向标签 / 建议话术
③ 结果里点「定位」跳到该条评论；点「复制话术」粘贴后自行发送
④ 命中风险评论时：点「举报下一条」自动走 长按→举报→选原因→提交
   同时弹出系统通知: ⚠ 检出 N 条风险评论, 风险处理请加 QQ <可配置>
```

设置页「④ 评论区舆情与风控」区也有「仅扫描评论」「扫描+AI风险识别」按钮，输出表格化结果。

### 判定维度

| 字段 | 取值 |
|------|------|
| `label` 风控标签 | 赌博引流 / 色情引流 / 诈骗黑产 / 垃圾广告 / 正常互动 |
| `level` 等级 | high / mid / low |
| `intent` 意向 | 赞美认可 / 主动求联系方式 / 想了解详情 / 纯闲聊 / 无意向 |
| `reply` 话术 | AI 针对该条评论生成的个性化搭话（30 字内，可一键复制） |

> 只**读取与举报**别人发布的内容，**不向任何人自动发送消息**——消息一律由你确认后手动发出。

### 关键实现

- 只扫**评论列表容器**（自动锁定 RecyclerView/ListView），避免把导航栏、昵称、定位信息当成评论
- 模型输出用**容错解析**（全角标点归一化 + 逐条正则兜底），模型偶尔不守 JSON 格式也不影响
- 风险处理 QQ 在设置页可改，默认 `286622079`，会随风险预警一起给出

### 举报流程（多策略回退）

```
① 长按评论正文 → 检查是否弹出含「举报」的菜单
② 没弹出 → 找「更多/···」入口再试
③ 找到举报 → 选原因(垃圾广告/色情低俗/涉赌/欺诈...) → 提交
```
每步都有日志（`举报步骤1/2`），不同 App/版本入口不同时可直接看日志定位。

---

## 网页联调控制台（已同步 App 全部新功能）

`tools/dashboard.py` 现在不只是看画面和日志，而是**能直接驱动 App 的全部新能力**：

启动：`python tools/dashboard.py`（或双击 `tools/monitor.bat`），浏览器打开 http://127.0.0.1:8123

| 分区 | 网页能做什么 | 底层动作 |
|------|--------------|----------|
| ① AI 智能控制 | 填任务 / 关键词一键生成任务 / 模型 / 步数 / Key，「启动 AI 执行」「停止 AI」「保存配置到手机」 | 广播 `START_AI` `STOP_AI` `SET_CONFIG` |
| ② 评论区风控 | 自动下滑开关 + 轮数 + 风险QQ，「扫描当前页评论」「扫描 + AI 风险识别」「举报下一条风险」，结果按风险/意向分区展示 | 广播 `SCAN_GUARD` `REPORT_NEXT` |
| ③ 私信守候 | 自动回复开关 + **你自己写死的回复内容** + 目标包名 + 每小时上限，「开启守候」「停止守候」 | 广播 `DM_GUARD_ON` `DM_GUARD_OFF` |
| ④ 权限体检 | 悬浮窗 / 无障碍 / 截图授权 / 悬浮窗服务 四个状态实时刷新，「一键补授悬浮窗/无障碍」 | appops + settings |
| ⑤ AI 看到的画面 | 一键拉取手机上的 `ai_last.jpg`，直接核对模型到底看见了什么 | adb cat |

其它：配置保存在 `tools/.dash_cfg.json`，下次打开自动回填；**空 Key 不会覆盖手机里已保存的 Key**。

### 新增的广播接口（网页/脚本都能用）

```bash
A=com.sixzs.local/.GuardReceiver
adb shell am broadcast -n $A -a com.sixzs.local.SET_CONFIG  --es aiTask "..." --es glmKey "..."
adb shell am broadcast -n $A -a com.sixzs.local.START_AI
adb shell am broadcast -n $A -a com.sixzs.local.STOP_AI
adb shell am broadcast -n $A -a com.sixzs.local.SCAN_GUARD
adb shell am broadcast -n $A -a com.sixzs.local.REPORT_NEXT
adb shell am broadcast -n $A -a com.sixzs.local.DM_GUARD_ON
adb shell am broadcast -n $A -a com.sixzs.local.DM_GUARD_OFF
```

> 注意：这七个 action 必须在 `AndroidManifest.xml` 的 `<receiver>` 里都声明，否则广播不会被投递（踩过这个坑：只声明 SCAN_GUARD 时，START_AI 广播会「发送成功但毫无反应」）。

---

## 构建与架构（重要）

```bash
sh build_windows.sh 32    # armeabi-v7a, 含原版 PaddleOCR 插件与模型 (14MB, 32位真机)
sh build_windows.sh 64    # arm64-v8a, 只有宿主(206KB, 不带OCR, OCR接口自动降级)
sh build_windows.sh both  # 双架构通用包 (14.4MB) —— 32位真机用OCR, arm64用宿主, x86模拟器可用
```

> 产物：`dist/新App-debug.apk`（32位）/ `dist/新App-arm64-debug.apk`（arm64）/ `dist/新App-通用-debug.apk`（双架构通用，**推荐**）

### 原生库与架构的坑

- `liblua_host.so` 只有 **armeabi-v7a** 与 **arm64-v8a** 两份，**没有 x86**。
  在 x86_64 模拟器上：
  - 32 位包 → `dlopen failed: failed to link liblua_host.so` → **点击「开始」会把 App 点崩**（已修复：现在捕获异常并提示）
  - arm64 包 → 原生库可加载，脚本能跑
- OCR（ncnn / PaddleOCR）**只有 armeabi-v7a 一份**，arm64 真机上 OCR 会降级，
  原版 Lua 脚本里靠图片找图标的逻辑会失效（表现为脚本反复「返回/1」空转）。
- **Lua 脚本的 `package.path` 依赖 `host('getWorkPath')`**，它返回的是纯字符串而非 JSON；
  prelude 里 `json.decode` 失败会返回 nil → 路径没设置 → `require("page") not found`。
  已修：prelude 的 `host()` 在 JSON 解析失败时**原样返回字符串**（顺带修好所有返回字符串的 host 方法）。

### 新增：直接启停脚本的广播

```bash
adb shell am broadcast -n com.sixzs.local/.GuardReceiver -a com.sixzs.local.START_SCRIPT
adb shell am broadcast -n com.sixzs.local/.GuardReceiver -a com.sixzs.local.STOP_SCRIPT
```

---

## 截图分析优化（v6, 11-13）

### 实测数据（同机同图对比）

| 项目 | 优化前 | 优化后 |
|------|--------|--------|
| 单次分析总耗时 | **40–57 秒** | **10.7 秒** |
| 图像体积(base64) | 93 KB | **22 KB**（裁剪后） |
| 视觉模型 | glm-4.5v | glm-4.6v |
| 读到的对方原话 | 常读不到 | **精确读到并展示** |

### 三项核心改动

**① 模型换成 glm-4.6v（快 7 倍）**

实测同一张截图同一段提示词：

| 模型 | 耗时 | 价格(输入/百万token) |
|------|------|------|
| glm-4.5v | 40–57 s | 2 元 |
| **glm-4.6v** | **5.8–8.4 s** | **1 元** |
| GLM-4.6V-Flash | 9–12 s | 免费（偶发 429） |

**② 两阶段流水线（读图 → 生成）**

```
阶段1  视觉模型只负责「读出对方说了什么」   → JSON: {latest, topic, visual}
阶段2  纯文本模型(flash)负责「分类 + 写回复」 → JSON: [{type, intent, replies}]
```

视觉模型不用再同时"看图+思考+写作"，思考 token 不再吃光 max_tokens；
阶段2 用便宜的纯文本模型，输出更稳定。

**③ 图像预处理（自建 ImgPrep）**

```
① 裁剪  只留聊天区域(去掉状态栏/导航栏/输入框)
② 缩放  长边压到 1280 (可配)
③ 压缩  JPEG 质量 70 (可配)
```

真机 1080×2400 实测：161KB(base64) → 79KB(缩放) → 51KB(缩放+裁剪)。

### 另外修掉的三个真问题

1. **`canTakeScreenshot="true"` 漏声明** → Android 11+ 无障碍截图直接报
   `Services don't have the capability of taking the screenshot.`
   （已在 `res/xml/access_service.xml` 补上）
2. **悬浮窗把自己截进去了** → 模型会把面板内容当成聊天消息。
   现在截图前先 `hideSelf()`，截完 `showSelf()`。
3. **`FOREGROUND_SERVICE_MEDIA_PROJECTION` 缺失** → Android 14+ 用
   MediaProjection 必须声明该权限 + `foregroundServiceType="mediaProjection"`，
   否则崩溃。targetSdk 也从 30 提到 34 让其真正生效。

### 新增可配置项

| 项 | 默认 | 说明 |
|----|------|------|
| 视觉模型 | glm-4.6v | 带截图时用 |
| 截图长边上限 | 1280 | 0=不缩放 |
| JPEG 质量 | 70 | 30–95 |
| 自动裁剪聊天区 | 开 | 省 token + 减少干扰 |

设置页与网页控制台均可改；网页新增「查看分析结果」按钮（读回 App 的分析日志，
含读图耗时/生成耗时/图像体积/读到的原话）。

### 新增广播

```bash
adb shell am broadcast -n com.sixzs.lovekb/.LoveReceiver -a com.sixzs.lovekb.TEST_SHOT
```

### 踩坑记录

- **targetSdk 提到 34 后要重新授权截图**：MediaProjection 的授权与 targetSdk 有关，
  升级后需在设置页重新点一次「授权截图」。
- **模拟器(API 28)走不了无障碍截图**，只有 MediaProjection 一条路；
  Android 11+ 真机才有免授权路径。
