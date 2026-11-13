#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
联调控制台 (纯标准库, 无第三方依赖)
浏览器打开 http://127.0.0.1:8123 :
  - 手机实时画面 (adb screencap 轮询, 约2帧/秒)
  - 脚本日志实时滚动 (adb logcat -s DMSX)
  - 基础按钮: 推送脚本 / 安装APK / 截一张图 / 清空日志
  - AI 控制台: 任务/模型/Key/步数 -> 启动/停止 AI, 直接调 GLM 看屏操作
  - 评论区风控: 扫描/AI 识别/自动下滑采集/举报下一条, 结果分区展示
  - 私信守候: 开关 + 你自己写死的回复内容, 一键下发到手机
  - 权限体检: 悬浮窗/无障碍/截图授权 状态, 一键补授
  - AI 看到的画面: 直接拉 ai_last.jpg 核对
用法: python dashboard.py [--port 8123]
停止: Ctrl+C
"""
import argparse
import json
import os
import subprocess
import threading
import time
import sys
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

ROOT = os.path.dirname(os.path.abspath(__file__))
PROJECT = os.path.dirname(ROOT)
OVERLAY = "/sdcard/AI私信助手数据/lua"
PKG = "com.sixzs.local"
GUARD = PKG + "/.GuardReceiver"
LOVE_PKG = "com.sixzs.lovekb"
LOVE_REC = LOVE_PKG + "/.LoveReceiver"
CFG_FILE = os.path.join(ROOT, ".dash_cfg.json")

ADB_CANDIDATES = [
    os.environ.get("ADB", ""),
    os.path.join(ROOT, "tools", "scrcpy", "adb.exe"),
    r"C:\Users\Administrator\AppData\Local\Android\Sdk\platform-tools\adb.exe",
    "adb",
]
ADB = next((p for p in ADB_CANDIDATES if p and (os.path.isfile(p) or p == "adb")), "adb")

state_lock = threading.Lock()
state = {"screen": b"", "screen_ts": 0, "logs": [], "status": "idle", "perm": {}}
SERIAL = os.environ.get("ASERIAL", "")


def pick_serial():
    """多设备时自动选一台在线的(优先 emulator-*, 其次任意 device 状态)"""
    global SERIAL
    if SERIAL:
        return SERIAL
    try:
        out = subprocess.run([ADB, "devices"], capture_output=True, timeout=10).stdout.decode("utf-8", "replace")
        online = []
        for line in out.splitlines()[1:]:
            line = line.strip()
            if not line:
                continue
            parts = line.split()
            if len(parts) >= 2 and parts[1] == "device":
                online.append(parts[0])
        if not online:
            return ""
        for s in online:
            if s.startswith("emulator-"):
                SERIAL = s
                break
        else:
            SERIAL = online[0]
        return SERIAL
    except Exception:
        return ""

DEFAULT_CFG = {
    "aiTask": "",
    "aiModel": "glm-4.5v",
    "aiMaxSteps": 15,
    "glmKey": "",
    "keyword": "",
    "autoDmReply": True,
    "autoDmText": "",
    "autoDmApps": "com.ss.android.ugc.aweme,com.xingin.xhs",
    "autoDmMaxPerHour": 10,
    "guardScroll": True,
    "guardScrollRounds": 6,
    "riskQQ": "286622079",
    "loveKey": "",
    "loveStyle": "温柔一点, 口语化, 别太油",
    "loveText": "",
    "loveIndex": 0,
    "loveFolded": False,
    "loveW": 330,
    "loveH": 420,
    "loveVisionModel": "glm-4.6v",
    "loveMaxEdge": 1280,
    "loveQuality": 70,
    "loveShotCrop": True,
    "loveTypeIdx": 0,
    "loveTypes": "幽默型,搭讪型,温柔型,高冷型,直球型,共情型,反撩型,暧昧试探型,冷淡敷衍,求助型",
    "loveDraft": "",
}


def load_cfg():
    try:
        with open(CFG_FILE, "r", encoding="utf-8") as f:
            d = json.load(f)
        c = dict(DEFAULT_CFG)
        c.update({k: v for k, v in d.items() if k in DEFAULT_CFG})
        return c
    except Exception:
        return dict(DEFAULT_CFG)


def save_cfg(cfg):
    try:
        with open(CFG_FILE, "w", encoding="utf-8") as f:
            json.dump(cfg, f, ensure_ascii=False, indent=2)
    except Exception as e:
        print("保存配置失败:", e)


CFG = load_cfg()


def adb(*args, binary=False, timeout=15):
    cmd = [ADB]
    s = pick_serial()
    if s:
        cmd += ["-s", s]
    cmd += list(args)
    r = subprocess.run(cmd, capture_output=True, timeout=timeout)
    if binary:
        return r.stdout
    return r.stdout.decode("utf-8", "replace")


def broadcast(action, **extras):
    """调用 App 的广播接口"""
    args = ["shell", "am", "broadcast", "-n", GUARD, "-a", PKG + "." + action]
    for k, v in extras.items():
        if isinstance(v, bool):
            args += ["--ez", k, "true" if v else "false"]
        elif isinstance(v, int):
            args += ["--ei", k, str(v)]
        else:
            args += ["--es", k, str(v)]
    out = adb(*args, timeout=20).strip()
    return out.splitlines()[-1] if out else "已发送"


def push_config(cfg):
    """把网页上的配置下发到手机(空值不覆盖, 尤其别把已存的 Key 清掉)"""
    extras = {
        "aiTask": cfg.get("aiTask", ""),
        "aiModel": cfg.get("aiModel", "glm-4.5v"),
        "aiMaxSteps": int(cfg.get("aiMaxSteps", 15) or 15),
        "autoDmReply": bool(cfg.get("autoDmReply")),
        "autoDmText": cfg.get("autoDmText", ""),
        "autoDmApps": cfg.get("autoDmApps", ""),
        "autoDmMaxPerHour": int(cfg.get("autoDmMaxPerHour", 10) or 10),
        "visionModel": cfg.get("loveVisionModel", "glm-4.6v"),
        "shotMaxEdge": int(cfg.get("loveMaxEdge", 1280) or 1280),
        "shotQuality": int(cfg.get("loveQuality", 70) or 70),
        "shotCrop": bool(cfg.get("loveShotCrop")),
        "types": cfg.get("loveTypes", ""),
        "currentType": cfg.get("loveCurrentType", ""),
        "guardScroll": bool(cfg.get("guardScroll")),
        "guardScrollRounds": int(cfg.get("guardScrollRounds", 6) or 6),
        "riskQQ": cfg.get("riskQQ", ""),
    }
    key = (cfg.get("glmKey") or "").strip()
    if key:
        extras["glmKey"] = key
    else:
        extras.pop("glmKey", None)
    return broadcast("SET_CONFIG", **extras)


def perm_status():
    """查设备上的权限状态"""
    st = {}
    try:
        ov = adb("shell", "appops", "get", PKG, "SYSTEM_ALERT_WINDOW").strip()
        st["悬浮窗"] = "allow" in ov
    except Exception:
        st["悬浮窗"] = False
    try:
        acc = adb("shell", "settings", "get", "secure", "enabled_accessibility_services").strip()
        st["无障碍"] = PKG in acc
    except Exception:
        st["无障碍"] = False
    try:
        mp = adb("shell", "dumpsys", "media_projection").strip()
        st["截图授权"] = PKG in mp
        if not st["截图授权"]:
            # 回退: 看最近日志里有没有投影就绪的记录
            with state_lock:
                logs = state["logs"]
            st["截图授权"] = any("截图(MediaProjection)就绪" in l for l in logs)
    except Exception:
        st["截图授权"] = False
    try:
        svc = adb("shell", "dumpsys", "activity", "services").strip()
        st["悬浮窗服务"] = ("%s/.FloatService" % PKG) in svc
    except Exception:
        st["悬浮窗服务"] = False
    return st


def fix_perms():
    """一键补授悬浮窗与无障碍(截图授权需要人工点系统弹窗)"""
    out = []
    try:
        adb("shell", "appops", "set", PKG, "SYSTEM_ALERT_WINDOW", "allow")
        out.append("悬浮窗 ✓")
    except Exception as e:
        out.append("悬浮窗失败:%s" % e)
    try:
        adb("shell", "settings", "put", "secure", "enabled_accessibility_services",
            "%s/%s.AccessService" % (PKG, PKG))
        adb("shell", "settings", "put", "secure", "accessibility_enabled", "1")
        out.append("无障碍 ✓")
    except Exception as e:
        out.append("无障碍失败:%s" % e)
    return " ".join(out) + " （截图授权仍需在 App 里点「截图授权」并选「立即开始」）"


def pull_ai_image():
    """拉取 AI 实际看到的画面"""
    remote = "/storage/emulated/0/Android/data/%s/files/ai_last.jpg" % PKG
    try:
        data = adb("exec-out", "cat", remote, binary=True, timeout=20)
        if data and len(data) > 500:
            return data
    except Exception:
        pass
    return None


def parse_risk(logs):
    """从日志里抽出风控/意向结果, 供网页分区展示"""
    out = []
    for ln in logs:
        body = ln.strip()
        if body.startswith("[") and "]" in body:
            head = body[1:body.index("]")]
            rest = body[body.index("]") + 1:].strip()
            if head.startswith("意向:"):
                out.append({"cls": "lead", "tag": head, "text": rest})
            elif "/" in head:
                out.append({"cls": "risk", "tag": head, "text": rest})
        elif body.startswith("风控扫描:"):
            out.append({"cls": "", "tag": "扫描结果", "text": body})
        elif body.startswith("私信守候:"):
            out.append({"cls": "lead", "tag": "私信", "text": body})
    return out[-40:]


def perm_loop():
    """定期刷新权限状态, 供网页展示"""
    while True:
        try:
            p = perm_status()
            with state_lock:
                state["perm"] = p
        except Exception:
            pass
        time.sleep(8)


def love_broadcast(action, **extras):
    """调用恋爱键盘的广播接口"""
    args = ["shell", "am", "broadcast", "-n", LOVE_REC, "-a", LOVE_PKG + "." + action]
    for k, v in extras.items():
        if isinstance(v, bool):
            args += ["--ez", k, "true" if v else "false"]
        elif isinstance(v, int):
            args += ["--ei", k, str(v)]
        else:
            args += ["--es", k, str(v)]
    out = adb(*args, timeout=20).strip()
    return out.splitlines()[-1] if out else "已发送"


def love_log_tail():
    """抓恋爱键盘最近的分析日志(读图结果/耗时/体积)"""
    try:
        out = adb("logcat", "-d")
        out = "\n".join([l for l in out.splitlines() if "LOVEKB" in l])
    except Exception:
        return ""
    keys = ("阶段1", "分析完成", "截图 ", "预处理后", "生成 ", "未读到", "LOVEKB")
    lines = [l for l in out.splitlines() if any(k in l for k in keys)]
    return "\n".join(lines[-25:])


def screen_loop():
    """持续截屏 (PNG), 约2帧/秒"""
    while True:
        try:
            data = adb("exec-out", "screencap", "-p", binary=True, timeout=10)
            if data and len(data) > 100:
                with state_lock:
                    state["screen"] = data
                    state["screen_ts"] = time.time()
        except Exception:
            pass
        time.sleep(0.5)


def log_loop():
    """持续读 logcat -s DMSX"""
    cmd = [ADB]
    s = pick_serial()
    if s:
        cmd += ["-s", s]
    cmd += ["logcat", "-s", "DMSX:V", "-T", "1"]
    try:
        p = subprocess.Popen(cmd, stdout=subprocess.PIPE)
    except Exception as e:
        with state_lock:
            state["status"] = "logcat 启动失败: %s" % e
        return
    while True:
        line = p.stdout.readline()
        if not line:
            time.sleep(1)
            try:
                p = subprocess.Popen(cmd, stdout=subprocess.PIPE)
            except Exception:
                time.sleep(2)
            continue
        text = line.decode("utf-8", "replace").rstrip()
        # logcat 格式: "11-12 11:30:00.123 D/...: msg" 截掉前缀时间重复
        if ": " in text:
            text = text.split(": ", 1)[1]
        with state_lock:
            state["logs"].append(text)
            state["logs"] = state["logs"][-400:]


PAGE = """<!DOCTYPE html>
<html><head><meta charset="utf-8"><title>AI私信助手 联调控制台</title>
<style>
 body{font-family:Consolas,'Microsoft YaHei',monospace;background:#1e1e1e;color:#ddd;margin:0;padding:12px}
 h3{margin:6px 0;color:#9cdcfe;font-weight:normal}
 .wrap{display:flex;gap:14px;flex-wrap:wrap}
 .panel{background:#252526;border:1px solid #333;border-radius:8px;padding:10px;margin-bottom:12px}
 .cols{display:flex;gap:14px;flex-wrap:wrap}
 .col{flex:1;min-width:330px}
 #screen{width:330px;border:1px solid #444;border-radius:4px;background:#000;min-height:560px}
 #log{width:100%;height:420px;overflow-y:auto;background:#1a1a1a;padding:8px;font-size:12px;line-height:1.5;white-space:pre-wrap;word-break:break-all}
 button{background:#0e639c;color:#fff;border:0;border-radius:4px;padding:7px 12px;margin:2px;cursor:pointer;font-size:13px}
 button:hover{background:#1177bb}
 button.g{background:#2d6a4f}button.g:hover{background:#3b8f66}
 button.r{background:#a13b3b}button.r:hover{background:#c74f4f}
 button.s{background:#6b5b95;padding:5px 9px;font-size:12px}
 input,textarea,select{background:#1a1a1a;color:#ddd;border:1px solid #444;border-radius:4px;padding:6px;font-size:12px;font-family:inherit;width:100%;box-sizing:border-box}
 textarea{resize:vertical}
 .row{display:flex;gap:8px;align-items:center;margin:4px 0}
 .row>label{white-space:nowrap;font-size:12px;color:#9cdcfe;width:96px}
 .row>input,.row>textarea,.row>select{flex:1}
 .tag{display:inline-block;padding:2px 8px;border-radius:10px;font-size:11px;margin:2px 4px 2px 0}
 .ok{background:#2d6a4f;color:#c9f5d8}.bad{background:#a13b3b;color:#ffd7d7}.mid{background:#8a6d1f;color:#ffe9a8}
 #stat{color:#ce9178;font-size:12px;margin:4px 0}
 .ts{color:#6a9955}
 .sub{color:#8b949e;font-size:11px;margin:2px 0 6px}
 .sec{border-top:1px solid #333;margin:10px 0 8px;padding-top:8px;color:#9cdcfe;font-size:13px}
 #aiimg{max-width:100%;border:1px solid #444;border-radius:4px;margin-top:6px}
 .risk{border-left:3px solid #a13b3b;padding-left:8px;margin:6px 0;font-size:12px}
 .lead{border-left:3px solid #2d6a4f;padding-left:8px;margin:6px 0;font-size:12px}
</style></head>
<body>
<h3>AI私信助手 联调控制台 <span id="stat"></span></h3>
<div class="wrap">
 <div class="panel">
   <div class="ts">手机画面 (约2帧/秒, 如黑屏请检查USB调试/授权)</div><br>
   <img id="screen" src="/screen">
   <div style="margin-top:8px">
     <button onclick="act('push')">推送脚本</button>
     <button onclick="act('install')">安装APK</button>
     <button onclick="act('snap')">截一张图</button>
     <button onclick="clearLog()">清空日志</button>
   </div>
   <div id="stat2"></div>
 </div>

 <div style="flex:1;min-width:620px">
  <div class="panel">
   <div class="sec">① AI 智能控制 (GLM 看屏操作)</div>
   <div class="row"><label>任务</label><textarea id="aiTask" rows="2" placeholder="例: 打开抖音, 搜索\"美食\", 进入第一个视频评论区"></textarea></div>
   <div class="row"><label>关键词</label><input id="keyword" placeholder="填了自动生成搜索任务"><button class="s" onclick="genTask()">生成任务</button></div>
   <div class="row"><label>模型</label><input id="aiModel" value="glm-4.5v"><label style="width:auto">步数</label><input id="aiMaxSteps" value="15" style="width:60px"><label style="width:auto">Key</label><input id="glmKey" type="password" placeholder="智谱 API Key"></div>
   <div>
     <button class="g" onclick="act('aistart')">启动 AI 执行</button>
     <button class="r" onclick="act('aistop')">停止 AI</button>
     <button onclick="act('savecfg')">保存配置到手机</button>
   </div>
   <div class="sub">启动后 AI 会自己看屏 → 给动作 → 真实点击; 每一步都出现在下面的日志里。</div>

   <div class="sec">② 评论区风控 / 意向线索</div>
   <div class="row"><label>自动下滑</label><input type="checkbox" id="guardScroll" checked><label style="width:auto">轮数</label><input id="guardScrollRounds" value="6" style="width:60px"><label style="width:auto">风险QQ</label><input id="riskQQ" value="286622079"></div>
   <div>
     <button onclick="act('scan')">扫描当前页评论</button>
     <button class="g" onclick="act('scanai')">扫描 + AI 风险识别</button>
     <button class="r" onclick="act('reportnext')">举报下一条风险</button>
   </div>
   <div id="riskbox"></div>

   <div class="sec">③ 私信守候 (回复内容你自己写死)</div>
   <div class="row"><label>自动回复</label><input type="checkbox" id="autoDmReply"></div>
   <div class="row"><label>回复内容</label><textarea id="autoDmText" rows="2" placeholder="例: 你好呀, 看到你的评论了, 很高兴认识你"></textarea></div>
   <div class="row"><label>目标包名</label><input id="autoDmApps" value="com.ss.android.ugc.aweme,com.xingin.xhs"><label style="width:auto">每小时</label><input id="autoDmMaxPerHour" value="10" style="width:60px"></div>
   <div>
     <button class="g" onclick="act('dmguard_on')">开启守候</button>
     <button class="r" onclick="act('dmguard_off')">停止守候</button>
   </div>

   <div class="sec">④ 权限体检</div>
   <div id="perms"></div>
   <div style="margin-top:6px">
     <button onclick="act('fixperms')">一键补授悬浮窗/无障碍</button>
     <button onclick="act('permrefresh')">刷新状态</button>
   </div>

   <div class="sec">⑤ AI 看到的画面</div>
   <button class="s" onclick="loadAiImg()">拉取 ai_last.jpg</button>
   <img id="aiimg" style="display:none">

   <div class="sec">⑥ 恋爱键盘 (悬浮窗 / 智谱分类回复)</div>
   <div class="row"><label>API Key</label><input id="loveKey" type="password" placeholder="智谱 Key"></div>
   <div class="row"><label>风格</label><input id="loveStyle" value="温柔一点, 口语化, 别太油"></div>
   <div class="row"><label>要复制的内容</label><input id="loveText" placeholder="例: 想你的第37天"></div>
   <div class="row"><label>话术内容(改完再发)</label><input id="loveDraft" placeholder="选中候选后可在这里改成自己的话"></div>
   <div class="row"><label>选第几条</label><input id="loveIndex" type="number" value="0">
     <label style="width:auto">宽</label><input id="loveW" type="number" value="330" style="width:64px">
     <label style="width:auto">高</label><input id="loveH" type="number" value="420" style="width:64px"></div>
   <div class="row"><label>折叠</label><input type="checkbox" id="loveFolded">
     <label style="width:auto">视觉模型</label><input id="loveVisionModel" value="glm-4.6v" style="width:110px">
     <label style="width:auto">长边</label><input id="loveMaxEdge" type="number" value="1280" style="width:64px">
     <label style="width:auto">质量</label><input id="loveQuality" type="number" value="70" style="width:52px"></div>
   <div class="row"><label>裁剪聊天区</label><input type="checkbox" id="loveShotCrop" checked>
     <label style="width:auto">类型序号</label><input id="loveTypeIdx" type="number" value="0" style="width:56px"></div>
   <div class="row"><label>类型列表</label><input id="loveTypes" value="幽默型,搭讪型,温柔型,高冷型,直球型,共情型,反撩型,暧昧试探型,冷淡敷衍,求助型"></div>
   <div>
     <button onclick="act('love_install')">安装恋爱键盘</button>
     <button onclick="act('love_config')">下发 Key/风格</button>
     <button class="g" onclick="act('love_panel')">启动悬浮窗</button>
     <button onclick="act('love_read')">读取剪贴板</button>
     <button onclick="act('love_gen')">生成回复</button>
     <button onclick="act('love_pick')">选中第N条</button>
     <button onclick="act('love_fill')">填入聊天框</button>
     <button class="s" onclick="act('love_copy')">复制指定内容</button>
     <button onclick="act('love_fold')">折叠/展开</button>
     <button onclick="act('love_size')">设置大小</button>
     <button onclick="act('love_ime_set')">设为默认输入法</button>
     <button onclick="act('love_ctx')">读当前聊天上下文</button>
     <button onclick="act('love_shot')">截图分析(视觉模型)</button>
     <button onclick="act('love_testshot')">截图自检</button>
     <button onclick="act('love_result')">查看分析结果</button>
     <button onclick="act('love_cleanup')">清理内存</button>
     <button onclick="act('love_undo')">撤回</button>
     <button onclick="act('love_draft')">设置话术</button>
     <button onclick="act('love_send')">发送话术</button>
     <button onclick="act('love_ime_read')">输入法读剪贴板+生成</button>
     <button onclick="act('love_ime_commit')">输入法写入第N条</button>
   </div>
   <div class="sub">流程: 复制对方消息 → 点「读取剪贴板」→ App 里点「生成回复」→ 点候选 → 「填入聊天框」。</div>
  </div>

  <div class="panel">
   <div class="ts">实时日志 (adb logcat -s DMSX)</div>
   <div id="log"></div>
  </div>
 </div>
</div>
<script>
let lastN=0;
function esc(s){return s.replace(/&/g,'&amp;').replace(/</g,'&lt;')}
function fmt(t){return '<span class="ts">'+esc(t.slice(0,8))+'</span> '+esc(t.slice(9))}
function $(id){return document.getElementById(id)}
function collect(){
  return {
    aiTask:$('aiTask').value, keyword:$('keyword').value, aiModel:$('aiModel').value,
    aiMaxSteps:parseInt($('aiMaxSteps').value)||15, glmKey:$('glmKey').value,
    autoDmReply:$('autoDmReply').checked, autoDmText:$('autoDmText').value,
    autoDmApps:$('autoDmApps').value, autoDmMaxPerHour:parseInt($('autoDmMaxPerHour').value)||10,
    guardScroll:$('guardScroll').checked, guardScrollRounds:parseInt($('guardScrollRounds').value)||6,
    riskQQ:$('riskQQ').value,
    loveKey:$('loveKey').value, loveStyle:$('loveStyle').value, loveText:$('loveText').value,
    loveIndex:parseInt($('loveIndex').value)||0,
    loveFolded:$('loveFolded').checked, loveW:parseInt($('loveW').value)||330,
    loveH:parseInt($('loveH').value)||420,
    loveVisionModel:$('loveVisionModel').value||'glm-4.6v',
    loveMaxEdge:parseInt($('loveMaxEdge').value)||1280,
    loveQuality:parseInt($('loveQuality').value)||70,
    loveShotCrop:$('loveShotCrop').checked,
    loveTypeIdx:parseInt($('loveTypeIdx').value)||0
    loveTypes:$('loveTypes').value||'',
    loveDraft:$('loveDraft').value||''
  };
}
function fill(c){
  for(const k in c){ const el=$(k); if(el && c[k]!==undefined){
     if(el.type==='checkbox') el.checked=!!c[k]; else el.value=c[k]; } }
}
async function poll(){
  try{
    let r=await fetch('/data'); let j=await r.json();
    $('stat').textContent=j.status;
    let el=$('log');
    if(j.logs.length>lastN || lastN>j.logs.length){
      el.innerHTML=j.logs.map(fmt).join('<br>');
      lastN=j.logs.length; el.scrollTop=el.scrollHeight;
    }
    if(j.risk && j.risk.length) renderRisk(j.risk);
    if(j.perm) renderPerm(j.perm);
    $('screen').src='/screen?t='+Date.now();
  }catch(e){$('stat').textContent='连接失败: '+e}
}
function renderRisk(list){
  try{
    $('riskbox').innerHTML=list.map(r=>
      '<div class="'+(r.cls||'')+'"><b>'+esc(r.tag)+'</b> '+esc(r.text)+
      (r.extra?'<br>'+esc(r.extra):'')+'</div>').join('');
  }catch(e){}
}
function renderPerm(p){
  try{
    const keys=['悬浮窗','无障碍','截图授权','悬浮窗服务'];
    $('perms').innerHTML=keys.map(k=>
      '<span class="tag '+(p[k]?'ok':'bad')+'">'+(p[k]?'✓':'✗')+' '+k+'</span>').join('');
  }catch(e){}
}
async function act(a){
  $('stat2').textContent='执行中: '+a+' ...';
  if(a==='savecfg'||a==='aistart'||a==='dmguard_on'||a==='love_config'){
    let c=collect();
    await fetch('/cfg',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(c)});
  }
  fetch('/act?a='+encodeURIComponent(a)).then(r=>r.text()).then(t=>{
    $('stat2').textContent=t;
    if(a==='snap') window.open('/screen?t='+Date.now());
  }).catch(e=>$('stat2').textContent='失败: '+e);
}
function genTask(){
  const kw=$('keyword').value.trim();
  if(!kw){alert('先填关键词');return;}
  $('aiTask').value='打开抖音, 搜索"'+kw+'", 打开搜索结果里的第一个视频, 进入该视频的评论区后停下, 等待我下一步指示';
}
function clearLog(){fetch('/act?a=clearlog');$('log').innerHTML='';lastN=0}
function loadAiImg(){fetch('/aiimg').then(r=>{if(r.ok){$('aiimg').src='/aiimg?t='+Date.now();$('aiimg').style.display='block'}else alert('还没有 ai_last.jpg, 先跑一次 AI')})}
fetch('/cfg').then(r=>r.json()).then(j=>{
  try{ fill(j.cfg); }catch(e){}
  try{ renderPerm(j.perm||{}); }catch(e){}
}).catch(()=>{});
setInterval(poll,700); poll();
</script></body></html>"""


class Handler(BaseHTTPRequestHandler):
    def log_message(self, *a):
        pass

    def _send(self, code, body, ctype="text/html; charset=utf-8"):
        self.send_response(code)
        self.send_header("Content-Type", ctype)
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        if self.path == "/":
            self._send(200, PAGE.encode("utf-8"))
        elif self.path == "/data":
            with state_lock:
                logs = state["logs"][-200:]
                body = json.dumps({
                    "logs": logs,
                    "status": state["status"],
                    "risk": parse_risk(logs),
                    "perm": state.get("perm", {}),
                }).encode("utf-8")
            self._send(200, body, "application/json")
        elif self.path == "/cfg":
            body = json.dumps({"cfg": CFG, "perm": perm_status()},
                              ensure_ascii=False).encode("utf-8")
            self._send(200, body, "application/json")
        elif self.path == "/aiimg":
            img = pull_ai_image()
            if not img:
                self._send(404, b"no ai_last.jpg")
                return
            self.send_response(200)
            self.send_header("Content-Type", "image/jpeg")
            self.send_header("Content-Length", str(len(img)))
            self.send_header("Cache-Control", "no-store")
            self.end_headers()
            self.wfile.write(img)
        elif self.path.startswith("/screen"):
            with state_lock:
                img = state["screen"]
            if not img:
                img = b"\x89PNG\r\n\x1a\n"  # 空占位
            self.send_response(200)
            self.send_header("Content-Type", "image/png")
            self.send_header("Content-Length", str(len(img)))
            self.send_header("Cache-Control", "no-store")
            self.end_headers()
            self.wfile.write(img)
        elif self.path.startswith("/act"):
            from urllib.parse import urlparse, parse_qs
            a = parse_qs(urlparse(self.path).query).get("a", [""])[0]
            self._send(200, self.do_act(a).encode("utf-8"), "text/plain; charset=utf-8")
        else:
            self._send(404, b"not found")

    def do_POST(self):
        from urllib.parse import urlparse
        if urlparse(self.path).path == "/cfg":
            try:
                n = int(self.headers.get("Content-Length", 0))
                data = json.loads(self.rfile.read(n).decode("utf-8")) if n else {}
                cfg = dict(CFG)
                for k in DEFAULT_CFG:
                    if k in data:
                        cfg[k] = data[k]
                CFG.clear()
                CFG.update(cfg)
                save_cfg(CFG)
                msg = push_config(CFG)
                self._send(200, ("配置已保存并下发到手机: " + msg).encode("utf-8"),
                           "text/plain; charset=utf-8")
            except Exception as e:
                self._send(500, ("保存失败: %s" % e).encode("utf-8"), "text/plain; charset=utf-8")
        else:
            self._send(404, b"not found")

    def do_act(self, a):
        try:
            if a == "push":
                os.makedirs(os.path.join(PROJECT, "assets", "lua"), exist_ok=True)
                adb("shell", "mkdir", "-p", OVERLAY)
                n = 0
                src = os.path.join(PROJECT, "assets", "lua")
                for f in os.listdir(src):
                    adb("push", os.path.join(src, f), OVERLAY + "/")
                    n += 1
                return "已推送 %d 个脚本, 悬浮窗点「重启」生效" % n
            if a == "install":
                apk = os.path.join(PROJECT, "dist", "新App-debug.apk")
                out = adb("install", "-r", apk, timeout=120)
                return out.strip()[-200:]
            if a == "snap":
                return "已在画面区刷新最新截图 (右键图片可另存)"
            if a == "clearlog":
                with state_lock:
                    state["logs"] = []
                return "日志已清空"
            # ---- AI 控制 ----
            if a == "savecfg":
                return "配置已下发: " + push_config(CFG)
            if a == "aistart":
                return "已下发并启动: " + broadcast("START_AI")
            if a == "aistop":
                return "已下发: " + broadcast("STOP_AI")
            # ---- 风控 ----
            if a == "scan":
                return "已下发扫描: " + broadcast("SCAN_GUARD")
            if a == "scanai":
                return "已下发扫描(AI识别): " + broadcast("SCAN_GUARD")
            if a == "reportnext":
                return "已下发: " + broadcast("REPORT_NEXT")
            # ---- 私信守候 ----
            if a == "dmguard_on":
                return "已下发: " + broadcast("DM_GUARD_ON")
            if a == "dmguard_off":
                return "已下发: " + broadcast("DM_GUARD_OFF")
            # ---- 权限 ----
            if a == "permrefresh":
                p = perm_status()
                return " / ".join("%s:%s" % (k, "✓" if v else "✗") for k, v in p.items())
            if a == "fixperms":
                return fix_perms()
            # ---- 恋爱键盘 ----
            if a == "love_install":
                apk = os.path.join(os.path.dirname(PROJECT), "LoveKeyboard", "dist", "恋爱键盘-debug.apk")
                if not os.path.isfile(apk):
                    return "找不到 APK: " + apk
                out = adb("install", "-r", apk, timeout=120)
                adb("shell", "appops", "set", LOVE_PKG, "SYSTEM_ALERT_WINDOW", "allow")
                adb("shell", "settings", "put", "secure", "enabled_accessibility_services",
                    "%s/%s.AccessService" % (LOVE_PKG, LOVE_PKG))
                adb("shell", "settings", "put", "secure", "accessibility_enabled", "1")
                return (out.strip()[-120:] + " (悬浮窗/无障碍已授权)")
            if a == "love_panel":
                return "已下发: " + love_broadcast("START_PANEL")
            if a == "love_read":
                return "已下发: " + love_broadcast("READ_CLIP")
            if a == "love_gen":
                return "已下发: " + love_broadcast("GENERATE")
            if a == "love_pick":
                return "已下发: " + love_broadcast("PICK", index=int(CFG.get("loveIndex", 0) or 0))
            if a == "love_fill":
                # 发编辑框里的话术(用户改过的), 没有就发选中的候选
                return "已下发: " + love_broadcast("SEND_DRAFT")
            if a == "love_fold":
                return "已下发: " + love_broadcast("FOLD", folded=bool(CFG.get("loveFolded", True)))
            if a == "love_size":
                return "已下发: " + love_broadcast("SET_SIZE",
                                                 w=int(CFG.get("loveW", 330) or 330),
                                                 h=int(CFG.get("loveH", 420) or 420))
            if a == "love_ctx":
                return "已下发: " + love_broadcast("READ_CONTEXT")
            if a == "love_send":
                return "已下发: " + love_broadcast("SEND_DRAFT")
            if a == "love_draft":
                t = CFG.get("loveDraft", "")
                if not t.strip():
                    return "先在「话术内容」里填要发的话"
                return "已下发: " + love_broadcast("SET_DRAFT", text=t)
            if a == "love_undo":
                return "已下发: " + love_broadcast("UNDO")
            if a == "love_cleanup":
                return "已下发: " + love_broadcast("CLEANUP")
            if a == "love_type":
                idx = int(CFG.get("loveTypeIdx", 0) or 0)
                types = ["自动判断"] + [t.strip() for t in
                        str(CFG.get("loveTypes", "")).split(",") if t.strip()]
                pick = types[idx] if 0 <= idx < len(types) else "自动判断"
                return "已下发: " + love_broadcast("SET_CONFIG", currentType=("" if pick == "自动判断" else pick))
            if a == "love_result":
                out = love_log_tail()
                return out if out else "(暂无分析日志, 先点「截图分析」)"
            if a == "love_testshot":
                return "已下发: " + love_broadcast("TEST_SHOT")
            if a == "love_shot":
                return "已下发: " + love_broadcast("SHOT_ANALYZE")
            if a == "love_ime_read":
                return "已下发: " + love_broadcast("IME_READ")
            if a == "love_ime_commit":
                return "已下发: " + love_broadcast("IME_COMMIT",
                                                  index=int(CFG.get("loveIndex", 0) or 0))
            if a == "love_ime_set":
                # 设为默认输入法(剪贴板读取必需)
                out = adb("shell", "ime", "enable", LOVE_PKG + "/.LoveImeService").strip()
                out2 = adb("shell", "ime", "set", LOVE_PKG + "/.LoveImeService").strip()
                return (out + " / " + out2)
            if a == "love_copy":
                t = CFG.get("loveText", "")
                if not t:
                    return "先在网页填写要复制的内容"
                return "已下发: " + love_broadcast("SEND_CLIP", text=t)
            if a == "love_config":
                key = (CFG.get("loveKey") or "").strip()
                style = CFG.get("loveStyle", "")
                extras = {}
                if key:
                    extras["glmKey"] = key
                if style:
                    extras["style"] = style
                if not extras:
                    return "先填写 Key 或风格"
                return "已下发: " + love_broadcast("SET_CONFIG", **extras)
            return "未知操作: " + a
        except Exception as e:
            return "出错: %s" % e


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--port", type=int, default=8123)
    args = ap.parse_args()
    for job in (screen_loop, log_loop, perm_loop):
        threading.Thread(target=job, daemon=True).start()
    srv = ThreadingHTTPServer(("127.0.0.1", args.port), Handler)
    print("联调控制台: http://127.0.0.1:%d  (Ctrl+C 退出)" % args.port)
    print("adb: %s  覆盖目录: %s" % (ADB, OVERLAY))
    srv.serve_forever()


if __name__ == "__main__":
    main()
