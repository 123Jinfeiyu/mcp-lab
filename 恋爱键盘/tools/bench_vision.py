# -*- coding: utf-8 -*-
"""
视觉模型基准测试: 同一张真实截图, 对比不同模型 / 不同图片尺寸的
耗时、token 消耗、输出质量。
"""
import base64, io, json, os, sys, time
import urllib.request

from PIL import Image

KEY = "09ddd26bbc974fc0823256236062a763.GiLlzTJyQaJDStk1"
URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions"
SRC = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "bench_feed.png")

SYSTEM = (
    "你是一个中文恋爱聊天助手。你要读懂对方发来的消息, 判断它的类型与意图, "
    "并写出可以直接发出去的回复。"
    "严格只输出 JSON 数组, 每项形如 "
    '{"type":"类型","intent":"对方意图","replies":["回复1","回复2","回复3"]}'
)

USER = (
    "这是对方发来消息的截图。请仔细看图, 找出对方最新说的那句话, "
    "判断类型与意图, 并给出 3 条可以直接发出去的回复。\n"
    "type 从这些里选: 幽默型,搭讪型,温柔型,高冷型,直球型,共情型,反撩型,暧昧试探型,冷淡敷衍,求助型\n"
    "严格只输出 JSON 数组。"
)


def encode(img, quality, max_edge=1280, crop=None):
    if crop:
        img = img.crop(crop)
    w, h = img.size
    if max_edge and max(w, h) > max_edge:
        s = max_edge / float(max(w, h))
        img = img.resize((int(w * s), int(h * s)), Image.LANCZOS)
    b = io.BytesIO()
    img.convert("RGB").save(b, "JPEG", quality=quality)
    return b.getvalue()


def call(model, jpeg, max_tokens=3000):
    body = {
        "model": model,
        "messages": [
            {"role": "system", "content": SYSTEM},
            {
                "role": "user",
                "content": [
                    {"type": "image_url",
                     "image_url": {"url": "data:image/jpeg;base64," + base64.b64encode(jpeg).decode()}},
                    {"type": "text", "text": USER},
                ],
            },
        ],
        "temperature": 0.85,
        "max_tokens": max_tokens,
    }
    req = urllib.request.Request(
        URL, data=json.dumps(body).encode("utf-8"),
        headers={"Content-Type": "application/json", "Authorization": "Bearer " + KEY})
    t0 = time.time()
    try:
        with urllib.request.urlopen(req, timeout=180) as r:
            raw = r.read().decode("utf-8")
    except Exception as e:
        return {"err": str(e), "sec": round(time.time() - t0, 1)}
    dt = round(time.time() - t0, 1)
    try:
        o = json.loads(raw)
    except Exception:
        return {"err": raw[:200], "sec": dt}
    if "error" in o:
        return {"err": str(o["error"])[:200], "sec": dt}
    ch = o["choices"][0]
    msg = ch["message"]
    u = o.get("usage", {})
    return {
        "sec": dt,
        "content": (msg.get("content") or "")[:400],
        "reasoning_len": len(msg.get("reasoning_content") or ""),
        "finish": ch.get("finish_reason"),
        "in_tok": u.get("prompt_tokens"), "out_tok": u.get("completion_tokens"),
        "total_tok": u.get("total_tokens"),
    }


def parse_ok(text):
    try:
        t = text.strip()
        if t.startswith("```"):
            t = t[t.index("\n") + 1:t.rindex("```")].strip()
        arr = json.loads(t[t.index("["):t.rindex("]") + 1])
        return "OK type=%s intent=%s replies=%d" % (
            arr[0].get("type"), arr[0].get("intent"), len(arr[0].get("replies", [])))
    except Exception:
        return "PARSE-FAIL"


def main():
    im = Image.open(SRC).convert("RGB")
    w, h = im.size
    variants = [
        ("原图 %dx%d q70" % (w, h), encode(im, 70, max_edge=0)),
        ("长边1280 q70", encode(im, 70, max_edge=1280)),
        ("长边1280 q60", encode(im, 60, max_edge=1280)),
        ("长边1280+裁剪 q70", encode(im, 70, max_edge=1280,
                                crop=(0, int(h * 0.10), w, int(h * 0.88)))),
    ]
    models = sys.argv[1:] or ["glm-4.5v"]
    print("=" * 74)
    for name, data in variants:
        print("\n### %s  -> %d KB (base64 %d KB)" % (
            name, len(data) // 1024, len(base64.b64encode(data)) // 1024))
        for m in models:
            r = call(m, data)
            if r.get("err"):
                print("  %-16s FAIL %ss  %s" % (m, r["sec"], r["err"][:120]))
                continue
            print("  %-16s %6ss  tok(in/out/total)=%s/%s/%s  finish=%s  reasoning=%d" % (
                m, r["sec"], r["in_tok"], r["out_tok"], r["total_tok"],
                r["finish"], r["reasoning_len"]))
            print("      parse: %s" % parse_ok(r["content"]))
            print("      out: %s" % r["content"][:160].replace("\n", " "))
    print("=" * 74)


if __name__ == "__main__":
    main()
