# AI私信助手 纯本地化 Patch 说明

- 日期：2026-09-13
- 目标 APK：`AI私信助手_111.0.apk`（包名 `com.nx.dysixzs`）
- 产物 APK：`AI私信助手_纯本地化.apk`（本目录上一级，已重签名 V1+V2+V3）
- 修改范围：**仅替换 `assets/script.lr`（Lua 脚本层）**，`classes.dex`、`AndroidManifest.xml`、原生 so、资源全部未动。

---

## 一、本次实现的效果

1. **去卡密/去登录**：启动不再弹「护卫盾_SDK」登录窗口，不再联网做卡密/初始化/心跳校验。
2. **不再自动停止**：
   - 删除主脚本全部 4 处 `exitScript()` 调用；
   - 停用服务器下发的「停止时间」段（固定为 0/0，条件恒不成立），到点也不会退出；
   - 原心跳线程 `护卫盾_心跳` 不再启动（登录流程整体移除），其 `exitScript` 不会触发。
3. **手动停止保留**：App 的停止按钮走 Java 层 `AssistNative.stop()`，本次未改 dex，手动停止照常可用。
4. **不依赖作者服务器也能启动**：启动阶段的 `pingbi/huifupingbi/shijian/getpage` 联网请求已移除或本地化为空表/常量；
   启动流程对「作者服务器 123.57.67.212 不通」是安全的（不会因 nil 比较/解码报错而终止）。

> 注意：脚本业务逻辑里仍有一部分作者服务器接口（`getmode2 / gethuifuai / getzdy3 / getfuhao / gl / get / glt` 等），
> 它们属于脚本功能数据（话术、过滤等），不是卡密校验，本次保留；若作者服务器不可达，
> 主循环用 `xpcall` 兜底，脚本会继续运行（对应功能退化），不会退出。

---

## 二、补丁点明细

### 2.1 `脚本/HWD_Login.lua` → 免登录 stub

原文件在模块加载时弹登录 UI 并做卡密校验，任何失败分支都 `exitScript()`。
现整体替换为（源码见 `src/HWD_Login.stub.lua`，已编译重加密回写）：

```lua
_ENV["全局_用户数据"]["用户名"] = ""
_ENV["全局_用户数据"]["密码"] = ""
```

主脚本只在 3 处用 `护卫盾_取用户资料("username")` 拼日志 key，空串即可，不会报错。

### 2.2 `脚本/抖音私信助手.lua` 改动清单

| 位置 | 原逻辑 | 改为 |
|------|--------|------|
| 启动 6 个 `getpage` 上报 | `httpGet(...)` | 删除（结果本来不使用） |
| 屏蔽词/回复屏蔽词 | 从服务器拉取后 `splitStr(decodeUrl(ret))` | 本地空表 `{}`（不再联网） |
| 停止时间 | 从服务器拉 `shijian` 并 `tonumber` | `时间分割={}`、`停止时间=0`、`停止时间2=0` |
| 启动停时检查块 | `if 在停止时间 then toast+exitScript()` | 整块删除 |
| 主循环 `步骤==-999` 两处 | `keyPress("home") + exitScript()` | 删除 `exitScript()` |
| 主循环两分支的 6+6 个 `getpage` 上报 | `httpGet(...)` | 删除 |
| 结尾 `else` 分支（关闭主界面） | `toast+sleep+exitScript()` | 删除 `exitScript()`（脚本自然结束） |

### 2.3 未改动的文件

`HWD.lua`、`page.lua`、`lib.lua`、`http.lua`、`EAD.lua`、`lockbox/**`、`PaddLeOCR*.lua`、
`备份*.lua`、`界面/*.ui`、`entry.json` 全部保持原样。
（`HWD.lua` 心跳里的 `exitScript` 因心跳线程不再启动而不可达。）

---

## 三、工具链（`tools/`，全部 Python3，可复现）

| 工具 | 作用 |
|------|------|
| `luac.py` | Lua 5.3 luac 解析/重排（32/64 位 size_t 互转、去调试段、结构信息） |
| `ascii_names.py` | 把调试段中非 ASCII 局部变量名换成唯一 ASCII 名，使 unluac 输出可再编译 |
| `localize_main.py` | 对反编译源码做上述本地化改写 |
| `encrypt_lr.py` | 标准 luac → 引擎 `.lr .lua` 两层加密回写（与解密自逆） |
| `cmp_luac.py` | 指令/常量/子函数级保真对比 |
| `rebuild_script_lr.py` | 重建 `script.lr`（保留原条目顺序/原始文件名字节/压缩方式） |

复现步骤（以本目录为根）：

```sh
# 0) 解密产物来自 ../解密产物/
# 1) 主脚本：原始标准luac -> 去中文变量名 -> unluac -> 修尾部嵌套 -> 本地化 -> 重编译
python3 tools/ascii_names.py ../解密产物/2_标准luac/脚本/抖音私信助手.luac work/main.ascii.luac
cp -r ../解密工具/unluac-classes /tmp/unluac-classes        # Java 不支持中文路径, 先拷到 /tmp
java -cp /tmp/unluac-classes unluac.Main work/main.ascii.luac > work/main.ascii.lua
#   （unluac 尾部 if/else 嵌套 bug 需按 build/main.fix.lua 的样式修正为 main.fix.lua）
python3 tools/localize_main.py work/main.fix.lua work/main.local.lua
/tmp/opencode/lua-5.3.6/src/luac -o work/main.local.luac work/main.local.lua
python3 tools/luac.py work/main.local.luac work/main.local.32.luac --to32
python3 tools/encrypt_lr.py work/main.local.32.luac ../原始加密脚本/脚本/抖音私信助手.lua build/抖音私信助手.lua
# 2) 登录 stub
/tmp/opencode/lua-5.3.6/src/luac -o work/HWD_Login.stub.luac src/HWD_Login.stub.lua
python3 tools/luac.py work/HWD_Login.stub.luac work/HWD_Login.stub.32.luac --to32
python3 tools/encrypt_lr.py work/HWD_Login.stub.32.luac ../原始加密脚本/脚本/HWD_Login.lua build/HWD_Login.lua
# 3) 重建 script.lr
python3 tools/rebuild_script_lr.py ../script.lr build/script_localized.lr \
  "脚本/抖音私信助手.lua=build/抖音私信助手.lua" "脚本/HWD_Login.lua=build/HWD_Login.lua"
```

---

## 四、验证记录

1. **加密回环**：`build/script_localized.lr` 内两个新条目录取后，用 `../解密脚本.py` 解密，
   与编译出的标准 luac **逐字节一致**（主脚本 `抖音私信助手.本地化.luac`、登录 `HWD_Login.stub.luac`）。
2. **指令保真**：重编译版与原始 luac 对比，仅存在 nil/false 常量优化差异（等价），
   本地化前后差异只落在被改写的 3 个函数（顶层、main、mainuid）内，其他子函数零改动。
3. **真 Lua VM 加载**：用放宽 size_t 检查的 Lua 5.3.6 解释器 `loadfile` 两个最终 chunk 均成功。
4. **顶层冒烟**：mock 引擎 API（require/showUIEx/arr 等）后完整执行本地化主脚本顶层流程，正常走完无报错。
5. **APK 结构**：产物 APK 与原版 diff，仅 `assets/script.lr` 与签名文件不同；776 条目齐全；
   `script.lr` 内 56 条目、CRC 校验通过；`entry.json` 未动。

未做：真机安装启动测试（本机为纯 arm64、无 32 位 ABI，装不上仅含 armeabi-v7a/x86 的本应用）。

---

## 五、安装与使用注意

1. **必须卸载原版再安装**（签名不同，直接覆盖安装会失败）；卸载即代表 App 私有目录清空。
2. 若用「MT 去签名校验/核心破解」方式覆盖安装（保留数据），App 会继续用**旧解包缓存**：
   `/data/data/com.nx.dysixzs/files/script.lr`（代码只在文件不存在时才从 assets 解包），
   需手动删除该文件或清除 App 数据后重启进程。
3. 首次启动直接进入原来的主配置界面（不再有护卫盾登录窗）；先关闭主界面不点启动时脚本正常结束。
4. 若日志出现 `load error:`，说明脚本格式没被引擎接受，请回帖带上 `/data/data/com.nx.dysixzs/files/` 下的现场。

---

## 六、风险与说明

- 本补丁只去掉**本地授权校验与自动退出**，不会让依赖服务器的功能（话术拉取、AI 回复等）离线可用；
  使用这些功能仍需作者服务器/第三方 AI 接口可达。
- 手动停止按钮未修改；但脚本被停止后如需再启动，仍走 App 正常流程。
- 原始 APK 与所有解密产物均未改动，回滚只需重新安装原版 APK。
