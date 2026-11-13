# 恋爱键盘 (LoveKeyboard)

> 一个**悬浮窗形态**的恋爱聊天助手：复制对方发来的消息 → AI 判断消息类型 → 给出可直接发送的候选回复 → **一键填进对方的聊天输入框**。
>
> 架构完全照搬「AI私信助手本地版」：无障碍服务 + 悬浮窗面板 + 智谱(清言) API，**纯 Java 实现，没有任何 native 库，所以任何架构的设备都能装**。

## 功能

| 能力 | 说明 |
|------|------|
| 读取剪贴板 | 一键取对方发来的那条消息（不用手输） |
| AI 分类 | 幽默型 / 搭讪型 / 温柔型 / 高冷型 / 直球型 / 共情型 / 反撩型 / 暧昧试探型 / 冷淡敷衍 / 求助型，类型可自定义 |
| 候选回复 | 每种类型给 1–5 条风格各异的回复（默认 3 条），点一下即选中并复制 |
| 填入聊天框 | 通过无障碍把回复直接写进对方 App 的聊天输入框；找不到输入框时自动退回「复制到剪贴板」 |
| 风格偏好 | 设置里写一句「温柔一点/别太油/多用短句」，会拼进提示词 |
| 目标 App 限制 | 填入前校验前台 App 包名，防止在别的 App 里误填 |

## 使用流程

```
① 对方发来消息 → 长按复制
② 点悬浮窗「读取剪贴板」   (面板显示: 已读取)
③ 点「生成回复」            (几秒后出现类型标签 + 3 条候选)
④ 点其中一条                 (选中并复制)
⑤ 点「填入聊天框」           (直接写进对方的输入框, 发送即可)
```

也可以在设置页用「测试分类」验证 Key 是否可用。

## 悬浮窗面板

- 标题栏可**拖动**（右上角 X 关闭）
- 状态区显示每一步日志
- 类型标签用粉色高亮（【幽默型】对方在试探/无聊）
- 候选回复每条一个浅粉卡片，点中变黄

## 权限

| 权限 | 用途 | 不给会怎样 |
|------|------|-----------|
| 悬浮窗 | 显示面板 | 面板出不来 |
| 无障碍服务 | 找到聊天输入框并填入 | 只能复制，不能自动填入 |
| 网络 | 调智谱 API | 无法生成回复 |

## 广播接口（网页/脚本可驱动）

```bash
R=com.sixzs.lovekb/.LoveReceiver
adb shell am broadcast -n $R -a com.sixzs.lovekb.START_PANEL
adb shell am broadcast -n $R -a com.sixzs.lovekb.READ_CLIP
adb shell am broadcast -n $R -a com.sixzs.lovekb.SEND_CLIP --es text "想你的第37天"
adb shell am broadcast -n $R -a com.sixzs.lovekb.SET_CONFIG --es glmKey "你的Key" --es style "温柔一点"
```

## 构建

```bash
sh build_windows.sh        # 产物: dist/恋爱键盘-debug.apk (纯 Java, 无需 native 库)
```

## 提示词要点（LoveAI.java）

- 类型与意图分离：`type` 用来选风格，`intent` 用来解释对方在干嘛
- 强调**分寸感**：对方冷淡就留台阶，对方热络就接住热度
- 禁止书面语、禁止括号表情说明、禁止复述原话、禁止承诺做不到的事
- 模型输出**容错解析**（全角标点归一化 → JSON → 正则兜底），模型偶尔不守格式也不丢结果

## 实测记录（模拟器 Android 9 / 抖音评论区场景）

```
LOVEKB: 已写入剪贴板: 你今天穿的这件外套好好看呀
LOVEKB: 已读取对方消息: 你今天穿的这件外套好好看呀
LOVEKB: 生成 3 条候选回复
LOVEKB: 选中第1条: 哈哈,谢谢夸奖,这件外套很配我。
LOVEKB: 已填入输入框: 哈哈,谢谢夸奖,这件外套很配我。
```

面板实际输出：

- 类型：**【温柔型】夸赞对方**
- 候选 1：哈哈,谢谢夸奖,这件外套很配我。
- 候选 2：这件外套是最近新买的,感觉还不错。
- 候选 3：这件外套颜色很衬我肤色,谢谢你发现。

## 踩过的坑

1. `startForegroundService` 之后必须 `startForeground()`，否则系统直接判崩
   （`Context.startForegroundService() did not then call Service.startForeground()`）。
2. `NotificationChannel.IMPORTANCE_LOW` 不存在，常量在 `NotificationManager` 上。
3. Android 8+ 后台启动限制：广播里直接 `startService` 会被拒
   （`Not allowed to start service ... app is in background`）→ 用前台服务方式解决。
4. 模拟器重启后无障碍服务不会自动绑定，需要重写
   `settings put secure enabled_accessibility_services <pkg>/<pkg>.AccessService` + `accessibility_enabled 1`，
   用 `dumpsys accessibility` 确认 `services:{...}` 里出现服务才算好。

---

## v2 更新：剪贴板读取 + 悬浮窗伸缩 + 一键发送（11-13 08:50-09:15）

### ① 剪贴板读取（Android 10+ 的硬限制）

Android 10 起，**只有「默认输入法」和「前台应用」能读剪贴板**，普通悬浮窗读不到别人复制的内容。
解决办法：本 App 同时注册成一个**输入法**（`LoveImeService`），成为默认输入法后就有剪贴板权限。

| 改动 | 说明 |
|------|------|
| `LoveImeService` | 真正的输入法：底部面板显示候选回复，点一下 `commitText` 写进聊天框，并可 `performEditorAction(IME_ACTION_SEND)` 直接发送 |
| 剪贴板自动监听 | `OnPrimaryClipChangedListener`：别人一复制，悬浮窗自动读取并显示（日志「检测到剪贴板变化, 自动读取」），不用手动点 |
| 读取失败提示 | 不再只说"剪贴板是空的"，而是明确告诉用户：Android 10+ 需要把「恋爱键盘」设为默认输入法 |
| 设置页新增 | 「设为默认输入法」按钮（直接跳转系统输入法设置页）+ 权限体检里显示「默认输入法 ✓/✗」 |
| `LoveEngine` | 悬浮窗与输入法共用的状态与生成逻辑（peerMsg / replies / analyze），两边结果同步 |

### ② 悬浮窗自动伸缩

| 能力 | 做法 |
|------|------|
| 拖拽缩放 | 右下角有缩放把手，拖动改窗口宽高（220dp~全屏），松手后尺寸存进 SharedPreferences，下次启动恢复 |
| 折叠/展开 | 标题栏的 `－`／`＋` 按钮，收起后只剩粉色标题栏一行；**生成出新回复时自动展开** |
| 脚本控制 | 广播 `FOLD`（--ez folded true/false）、`SET_SIZE`（--ei w/h） |

### ③ 点一下就发（替代「复制→粘贴→发送」）

点候选回复后自动完成三件事：**复制一份备用 → 填进对方聊天输入框 → 点发送**。

两条发送通道，按场景自动选：
1. **输入法通道**（本 App 正是当前输入法时）：`commitText` + `performEditorAction(IME_ACTION_SEND)`，不受键盘遮挡影响。
2. **无障碍通道**：定位输入框 `ACTION_SET_TEXT` → 找「发送/发表/Send」可点节点 `ACTION_CLICK`（找不到就按 contentDescription 找图标按钮，最多重试 3 次）。

设置页有「点候选回复后直接填入并发送」开关，关掉就只填入不发送。

实测日志：
```
选中第1条: 谢谢夸奖呀,你也有特别的地方呢~
已填入输入框: 谢谢夸奖呀,你也有特别的地方呢~
已发送: 谢谢夸奖呀,你也有特别的地方呢~
```

### 新增广播

```bash
R=com.sixzs.lovekb/.LoveReceiver
adb shell am broadcast -n $R -a com.sixzs.lovekb.FOLD --ez folded true
adb shell am broadcast -n $R -a com.sixzs.lovekb.SET_SIZE --ei w 330 --ei h 420
adb shell am broadcast -n $R -a com.sixzs.lovekb.IME_READ      # 输入法读剪贴板并生成
adb shell am broadcast -n $R -a com.sixzs.lovekb.IME_COMMIT --ei index 0
```

> 注：模拟器上系统会强行把默认输入法切回自带拼音（`ime set` 立刻被改回），
> 所以 IME 通道需在真机上验证；无障碍通道在模拟器上已验证通过。

---

## v3 更新：自动截图 + 提取聊天上下文（11-13 09:15-09:40）

### 新增两个入口

| 按钮 | 做什么 |
|------|--------|
| **读当前聊天上下文** | 用**无障碍节点树**把当前聊天窗口的气泡文本读出来（最近 12 条，按屏幕从上到下排序），存为上下文 |
| **截图分析** | 截当前屏幕 → 交给**视觉模型**看图 → 结合上下文生成回复（对方 App 不暴露界面时用这条） |

### 为什么要两条路

- **节点树**：快、准、免费，但**对方 App 自绘/图片渲染时读不到**（抖音 feed 页就是这样）。
- **截图 + 视觉模型**：慢一点、要授权，但**什么界面都能读**。
  两条互补，自动选：先读节点树，读不到就用截图。

### 上下文怎么进提示词

```
这是你们最近的聊天记录(从早到晚):
- ...
- ...
对方最新发来的消息: xxx
请结合上面的聊天记录, 判断对方现在的状态与意图...
要接住上文已经聊过的话题, 不要重复问对方已经说过的事。
```

实测（抖音评论区真实评论）：类型判成【温柔型】"回应对方的夸赞"，回复"被你这么一夸,感觉今天一天的心情都会变好呢。"——明显是贴着截图里的评论写的。

### 截图能力（两种）

| 系统 | 方式 | 说明 |
|------|------|------|
| Android 11+ | `AccessibilityService.takeScreenshot` | 免授权弹窗；API30 的类**必须隔离到独立类 + 反射调用**，否则低版本加载失败（NoClassDefFoundError）→ 无障碍服务整个绑不上 |
| Android 10 及以下 | MediaProjection | 设置页点「授权截图」授权一次；也可用 `am start -n com.sixzs.lovekb/.MainActivity --ez capture true` 触发授权 |

### 视觉模型的坑（重要）

`glm-4.5v` 是**思考型模型**：它先把 token 花在 `reasoning_content` 上，
`max_tokens=800` 会被思考吃光 → `finish_reason: length` → **正文 content 为空**。

处理办法：
1. 带图时**自动切到视觉模型**（`glm-4.5v`；纯文本的 `glm-4-flash` 会报
   `messages.content.type 参数非法, 取值范围 ['text']`）；
2. `max_tokens` 提到 3000；
3. 正文为空时**带上 reasoning 再问一次**"直接只输出 JSON 数组结果"（最多续写 3 轮）。

### 新增广播

```bash
adb shell am broadcast -n com.sixzs.lovekb/.LoveReceiver -a com.sixzs.lovekb.READ_CONTEXT
adb shell am broadcast -n com.sixzs.lovekb/.LoveReceiver -a com.sixzs.lovekb.SHOT_ANALYZE
adb shell am start -n com.sixzs.lovekb/.MainActivity --ez capture true   # 触发截图授权
```

---

## v4 更新：UI 重做 + 自由伸缩（11-13 09:40-10:05）

### 面板新结构

```
┌──────────────────────────────────────────┐
│ 恋爱键盘  [温柔型]            [▼]  [✕] │  ← 标题栏(可拖动) + 类型徽章 + 矢量图标
├──────────────────────────────────────────┤
│ 【温柔型】分享伤感内容, 暗示内心情绪      │  ← 状态区(白底卡片)
│ 生成 3 条候选回复                          │
│ ┌──────────────────────────────────────┐ │
│ │ 1. 不用想以前啦,我觉得我们现在这样…  │ │  ← 候选区(带序号, 可滚动)
│ │ 2. 怎么突然看这么伤感的视频…        │ │
│ │ 3. 是被这首歌触动了呀?…             │ │
│ └──────────────────────────────────────┘ │
│ [读剪贴板][生成回复][填入发送]            │  ← 主操作
│ [读上下文][截图分析][设置][复制]           │  ← 次操作
│ 拖右下角可缩放        [复位]      ◣       │  ← 尺寸 + 复位 + L形缩放手柄
└──────────────────────────────────────────┘
```

### 伸缩方式（三种）

| 方式 | 说明 |
|------|------|
| **拖右下角手柄** | L 形角标，34dp 触摸区，实时改宽高；松手后尺寸存 SharedPreferences，下次启动恢复 |
| **折叠 / 展开** | 标题栏 ▼ 收起成一条粉色标题栏（徽章还在），再点展开；**生成出新回复会自动展开** |
| **复位** | 底部「复位」一键恢复默认 340×520dp |

尺寸范围：宽 ≥220dp、高 ≥160dp，且不超过屏幕可用区域。窗口**宽度始终显式指定**（不设 WRAP_CONTENT）——否则内部用 weight 的徽章会把右侧按钮挤出面板（这是踩过的坑）。

### UI 细节上的几个坑

1. **徽章用 `0dp + weight=1` 会把按钮挤出面板**：父容器是 wrap_content 时，weighted 子项会吃掉整个屏幕宽度。改成「徽章按内容宽度(≤150dp) + 弹性 View 撑开 + 按钮固定宽」。
2. **`－`/`✕`/`×` 在部分系统上渲染成空白或怪字形** → 改用 **Vector 图形**（`ic_fold` 三角 / `ic_close` 叉），任何设备渲染一致。
3. **Button 文字被挤压**：Button 默认内边距会把 2 个字压成"▬▬"。改成 `TextView` 自绘（固定宽高 + gravity=center）就完全可控。
4. **候选区会撑破窗口** → 候选区套 `ScrollView`，窗口变小也能滚动查看。

### 新增广播

```bash
adb shell am broadcast -n com.sixzs.lovekb/.LoveReceiver -a com.sixzs.lovekb.SET_SIZE --ei w 330 --ei h 420
adb shell am broadcast -n com.sixzs.lovekb/.LoveReceiver -a com.sixzs.lovekb.FOLD --ez folded true
```

---

## v5 更新：Android 11+ 无障碍截图（免授权）+ 真机适配（11-13 10:35-10:45）

### 你的设备（Android 15 / iQOO Z7）怎么用

**Android 11+ 走无障碍截图，根本不需要"授权截图"那个弹窗**：

1. 开启「恋爱键盘」的无障碍服务（必须）
2. 直接用「截图分析」——App 会优先调 `AccessibilityService.takeScreenshot`，
   **应用在后台也能截**，不用授权、不弹窗、不怕被回收
3. 设置页有「测试无障碍截图」按钮，一点就知道能不能用；也可以在网页控制台点「截图自检」

```
截图自检: 方式=无障碍截图(免授权) 结果=无障碍截图成功 (尺寸 1080x2400)
```

### 三种截图方式与优先级

| 方式 | 何时用 | 备注 |
|------|--------|------|
| **无障碍截图**（API 30+） | Android 11+ **首选** | 免授权弹窗；后台可用；需要无障碍服务已连接 |
| MediaProjection | Android 10 及以下 / 无障碍截图失败时 | 设置页点「授权截图」授权一次 |
| 失败原因可见 | — | 受保护窗口 / 间隔太近 / 系统不支持 / 内部错误，都有中文提示 |

### 真机上会遇到的三个系统限制（都做了兜底）

1. **Android 12+ 禁止后台启动前台服务**
   浏览器/网页控制台点「启动悬浮窗」时 App 在后台 → 系统直接拒绝。
   现在会：先试前台服务 → 被拒则退普通服务 → 还不行就**发一条可点击的通知**
   （点一下打开 App 启动悬浮窗）。

2. **API 30 的类不能直接写在无障碍服务里**
   `TakeScreenshotCallback` 是 API 30 才有的类，直接引用会让 **Android 10 以下
   无障碍服务整个加载失败**（NoClassDefFoundError）→ 服务绑不上。
   解决：隔离到 `ScreenShot30` 独立类 + **反射调用**，低版本根本不加载这个类。

3. **连续截图有最小间隔**
   系统要求两次截图间隔 ≥1 秒，否则返回 `ERROR_TAKE_SCREENSHOT_INTERVAL_TIME_SHORT`。
   已加 1.1 秒节流 + 该错误码的中文提示。

### 新增

| 功能 | 位置 |
|------|------|
| 测试无障碍截图 | 设置页按钮，直接看能否用 |
| 保存截图到相册 | 设置页按钮，存到 Pictures/lovekb_*.jpg |
| 截图自检 | 网页控制台「截图自检」按钮 / 广播 `TEST_SHOT` |
| 截图方式与失败原因 | 设置页状态显示、悬浮窗日志 |

```bash
adb shell am broadcast -n com.sixzs.lovekb/.LoveReceiver -a com.sixzs.lovekb.TEST_SHOT
```

### v6.1 补充：裁剪回退与界面文案校验

- **裁剪裁歪了会自动回退**：如果裁剪版读出来的是界面文案（"大家都在搜"、"综合"、"推荐"…）
  而非人话，会**自动用整屏重试一次**（`LoveAI.looksLikeUiNoise` 判定）。
- **阶段1 maxTokens 从 600 提到 1500**：600 会被 glm-4.6v 的思考 token 吃光，
  触发续写反而更慢（实测 18s → 10s）。
- **网页控制台新增「查看分析结果」**：远程读回 App 的分析日志
  （读图耗时 / 生成耗时 / 图像体积 / 读到的原话）。
  注意：`logcat -s` 过滤在多设备环境下不稳，改用关键词过滤更可靠。

---

## v7：MediaProjection 正确架构（CaptureService 前台服务）

### 为什么必须重构

原来的写法是在 **Activity 里**直接 `getMediaProjection()` + `createVirtualDisplay()`，
在 **Android 14+ 会直接抛**：

```
SecurityException: Must start a foreground service of type mediaProjection
                  before creating a VirtualDisplay
```

而且 `resultCode + resultData` 是**一次性授权票**：必须拿到票后立刻换出
`MediaProjection` 对象，票就不能再用了。

### 现在的正确顺序

```
Activity 收到授权结果 (resultCode=-1 即 RESULT_OK)
   │  ① 把票放进同进程静态槽位 (最可靠, 跨进程传嵌套 Intent 会被系统剥离)
   ▼
CaptureService (foregroundServiceType="mediaProjection")
   │  ② startForeground(id, notification, FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
   │  ③ getMediaProjection(resultCode, resultData)   ← 用掉那张一次性票
   │  ④ registerCallback(onStop → 释放)
   │  ⑤ createVirtualDisplay(...)                     ← 必须在服务上下文里
   ▼
投影会话常驻(START_STICKY) → 随时 grab() 取一帧
```

### 关键实现细节

| 点 | 做法 | 原因 |
|----|------|------|
| 服务类型 | `foregroundServiceType="mediaProjection"`（0x20） | Android 14+ 建虚拟屏的前提 |
| 权限 | `FOREGROUND_SERVICE_MEDIA_PROJECTION` | 同上；必须在 `<application>` 之前声明 |
| targetSdk | 30 → **34** | 否则该权限与类型不生效 |
| 通知类型 | `startForeground(id, n, FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)` | Android 10+ 必须显式传类型 |
| 授权票传递 | **同进程静态槽位** + Intent extra 双通道 | 跨进程传嵌套 Intent 会被系统剥离 |
| resultCode 判断 | `rc == Activity.RESULT_OK`（**-1**） | ⚠️ RESULT_OK 是 **-1**，用 `rc < 0` 判断会把合法值当失败 |
| 撤销授权 | `MediaProjection.Callback.onStop()` → 释放会话 | 用户在系统设置里撤销后不再取帧 |
| 释放 | `unregisterCallback(cb)` + `projection.stop()` | 需传 Callback 参数（无参重载不存在） |

### 实测日志

```
onActivityResult: requestCode=1001 resultCode=-1 data=有
授权票已交给 CaptureService(mediaProjection 前台服务)
截图服务已启动(mediaProjection 类型)
收到授权票 rc=-1, data=有 (有效)
投影会话已建立 1280x720 (mediaProjection 前台服务内)
```

后台验证（App 退到桌面、launcher 在前台）：

```
isForeground=true foregroundId=1002
截图自检: 方式=MediaProjection(后台录屏中) 结果=后台录屏截图成功 尺寸=1280x720
```

### 新增广播

```bash
adb shell am broadcast -n com.sixzs.lovekb/.LoveReceiver -a com.sixzs.lovekb.CAP_START
adb shell am broadcast -n com.sixzs.lovekb/.LoveReceiver -a com.sixzs.lovekb.CAP_STOP
```

### 踩坑

1. **`Activity.RESULT_OK == -1`**，不是 0。用 `rc < 0` 判失败会把成功当失败——
   这个 bug 一度让"投影会话已建立"的日志永远不出现。
2. **跨进程传嵌套 Intent 会被系统剥离**：`putExtra(String, Intent)` 在
   `startForegroundService` 后可能拿不到，用同进程静态字段最稳。
3. **`MediaProjection.unregisterCallback()` 必须传 Callback 参数**，无参重载不存在。
4. **`Service` 没有 `onNewIntent`**（那是 Activity 的方法），后续 Intent 走 `onStartCommand`。
5. **targetSdk 提到 34 后 MediaProjection 授权不跨重装**，需重新授权。

---

## v8：内存清理 + 清上下文 + 类型下拉框（12:00-12:20）

### ① 内存清理

**找到的真问题**：截图分析的原始 Bitmap（1080×2400 ARGB ≈ **10MB**）**从不回收**，
分析完就等 GC，频繁使用内存持续上涨。

修法：
- `analyzeByShot()` 里 `ImgPrep.prepare()` 之后立即 `bmp.recycle()`
- `ScreenCap` 增加截图缓存管理（`keepShot` / `releaseShot` / `shotBytes`）
- 新增 `Cleaner` 类统一做清理

**悬浮窗「清理」按钮做四件事**：

| 清理项 | 释放内容 |
|--------|---------|
| 截图 | 缓存的 Bitmap |
| 候选卡片 | 摘掉 TextView 并断开引用 |
| 上下文 | 对方消息 / 聊天记录 / 候选回复（持久化） |
| 录屏会话 | VirtualDisplay + ImageReader + MediaProjection（最大的一块） |

释放后主动 `System.gc()`，右下角**实时显示内存占用**（每 5 秒刷新），能直接看到效果。

### ② 清上下文按钮

「清上下文」只清会话状态（对方消息 / 聊天记录 / 候选回复），**不动录屏会话**——
换人聊天时用，不影响后台录屏。清除后下拉框和候选区一起归零。

### ③ 回复类型下拉框

悬浮窗状态区下面加 `Spinner`：

```
[自动判断]  ← 默认, 让模型在多个类型里挑
[幽默型]
[搭讪型]
[温柔型]
[高冷型]
[直球型]
[共情型]
[反撩型]
[暧昧试探型]
[冷淡敷衍]
[求助型]
```

- 类型列表来自设置页配置（可自定义），第一个固定是「自动判断」
- 选中立即写进 `Config.currentType`，**下次生成只用这一个类型**
- 通过广播改配置时**下拉框自动刷新**，不用重启面板

实测：限定「搭讪型」后，模型判的类型和回复风格都变成搭讪型
（"我也喜欢这件外套啊，我最近迷上了这种风格"）。

### 新增广播

```bash
adb shell am broadcast -n com.sixzs.lovekb/.LoveReceiver -a com.sixzs.lovekb.CLEANUP
adb shell am broadcast -n com.sixzs.lovekb/.LoveReceiver -a com.sixzs.lovekb.SET_CONFIG --es currentType "幽默型"
```

网页控制台同步了「清理内存」按钮与类型序号输入框。

### 踩坑

1. **Button 默认内边距会把文字压成"读上""清理内"** → 次操作按钮全改成
   `TextView` 自绘（固定宽高 + gravity=center），文字完全可控。
2. **`Spinner` 的 `android:prompt` 不能直接写中文** → 会报
   `incompatible with attribute prompt (attr) reference`，去掉即可。
3. **技术日志不该刷状态区** → 新增 `Logx.quiet()`：只进 logcat，不显示在面板上。
   （原来"回复类型切换为""生成 3 条候选回复"都会挤占状态区）
4. **批量改 XML/Java 时用脚本容易插多份或截断字符串** → 优先用 Edit 工具改，
   改完用「异常字符扫描」检查一遍（控制字符 + 异常 Unicode 块）。

---

## v9：话术可人工修改 + 撤回（13:00-13:10）

### 问题
选中候选后**立即发送**，没有后悔药：
- 话术不合适想改 → 改不了
- 点错了想撤回 → 撤不回

### 现在的交互

```
① 选中候选(点一下)  → 高亮 + 文本进编辑框 + 复制备用 + 按开关决定是否立即发
   长按候选          → 只进编辑框, 不发送(给"先改再发"的机会)
② 在编辑框里人工修改
③ 点「填入发送」    → 发送编辑框里(改过的)内容
④ 点「撤回」        → 取消高亮 + 清编辑框 + 清掉对方输入框里已填入的文本 + 清剪贴板
```

面板结构多了**话术编辑框**（状态区下方、候选区上方）：

```
【温柔型】赞美对方眼光
生成 2.781s
[温柔型]                    ← 类型下拉框
[谢谢呀,这件外套很配我呢。]  ← 话术编辑框(可改)
1. 谢谢夸奖,这件外套真的很搭。
2. 哈哈,你也觉得好看呀,谢谢你的眼光。
3. ...
[读剪贴板][生成回复][填入发送]
[读上下文][截图分析][清上下文]
[设置][撤回][清理]   内存 174...
```

### 撤回做到了什么

`AccessService.clearChatInput()` —— 用 `ACTION_SET_TEXT` 把对方输入框里
我们填进去的话术清空。所以：

| 撤回时机 | 效果 |
|---------|------|
| 填了还没点发送 | ✅ 输入框内容真的被清空 |
| 已经点发送了 | ⚠️ 对方已收到，撤不回（需在对方App长按撤回） |

面板会明确区分这两种情况（"输入框内容已清空" vs "对方App输入框未找到, 需手动删除"）。

### 脚本/网页也走同一套三步

```bash
R=com.sixzs.lovekb/.LoveReceiver
# ① 选中(只选中, 不发送)
adb shell am broadcast -n $R -a com.sixzs.lovekb.PICK --ei index 1
# ② 改话术
adb shell am broadcast -n $R -a com.sixzs.lovekb.SET_DRAFT --es text "这外套哪买的 我也想要一件"
# ③ 发送
adb shell am broadcast -n $R -a com.sixzs.lovekb.SEND_DRAFT
# ④ 撤回
adb shell am broadcast -n $R -a com.sixzs.lovekb.UNDO
```

> 注意：`PICK` 的行为变了 —— 以前是"选中并立即发送"，现在是**只选中**。
> 要连发用 `pickAndSend()`（界面上点候选仍是"选中+按开关发送"）。

### 踩坑

**加了 EditText 后候选区不显示了**：候选区 ScrollView 用的是
`layout_height="wrap_content"` + `layout_weight="1"`，父容器是 wrap_content 时
**剩余空间为 0**，ScrollView 被压成 0 高度。
这是和之前"徽章 weight 撑破面板"同一类问题——**wrap_content 父容器里的 weight 不可靠**。
改用固定高度 `110dp` 解决。
