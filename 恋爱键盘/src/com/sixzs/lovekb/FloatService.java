package com.sixzs.lovekb;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * 悬浮窗面板:
 *  读取剪贴板 -> AI 分类+生成 -> 候选回复点一下就复制/填入对方聊天框
 */
public class FloatService extends android.app.Service {

    public static FloatService I;
    private WindowManager wm;
    private View view;
    private TextView tvStatus, tvType, tvSize;
    private LinearLayout llReplies, panelBody;
    private android.widget.Spinner spType;
    private TextView tvMem;
    private EditText etDraft;
    /** 当前选中候选的 TextView(用于取消高亮) */
    private TextView pickedCard = null;
    /** 上一次填入对方输入框的文本(撤回时用) */
    private String lastFilled = "";
    private java.util.Timer memTimer;
    private Button btnGen;
    private android.widget.ImageButton btnFold;
    private View resizeHandle;

    /** 对方消息 */
    private String peerMsg = "";
    /** 最近一次生成的候选回复 */
    private final List<String> replies = new ArrayList<String>();
    /** 已选中的回复 */
    private String picked = "";
    /** 面板是否折叠 */
    private boolean folded = false;
    /** 保存的窗口宽高(px) */
    private int savedW = 0, savedH = 0;

    private android.content.ClipboardManager clipMgr;
    private android.content.ClipboardManager.OnPrimaryClipChangedListener clipListener;
    /** 窗口参数(供折叠/缩放使用) */
    private WindowManager.LayoutParams lp;

    @Override
    public void onCreate() {
        super.onCreate();
        I = this;
        startForegroundCompat();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        Logx.sink = new Logx.Consumer() {
            public void accept(String s) {
                appendStatus(s);
            }
        };
        initOverlay();
    }

    /** 前台服务: 让广播/脚本在后台也能把悬浮窗拉起来 */
    private void startForegroundCompat() {
        try {
            String ch = "lovekb";
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= 26 && nm != null) {
                NotificationChannel c = new NotificationChannel(ch, "恋爱键盘", NotificationManager.IMPORTANCE_LOW);
                nm.createNotificationChannel(c);
                Notification n = new Notification.Builder(this, ch)
                        .setContentTitle("恋爱键盘")
                        .setContentText("悬浮窗运行中")
                        .setSmallIcon(android.R.drawable.ic_menu_edit)
                        .setOngoing(true)
                        .build();
                startForeground(1001, n);
            } else if (Build.VERSION.SDK_INT < 26) {
                startForeground(1001, new Notification.Builder(this)
                        .setContentTitle("恋爱键盘")
                        .setContentText("悬浮窗运行中")
                        .setSmallIcon(android.R.drawable.ic_menu_edit)
                        .build());
            }
        } catch (Throwable t) {
            Logx.e("前台通知失败(不影响悬浮窗)", t);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public android.os.IBinder onBind(Intent intent) {
        return null;
    }

    private void initOverlay() {
        view = LayoutInflater.from(this).inflate(R.layout.float_panel, null);
        int type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        final WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.START;
        p.x = 20;
        p.y = 150;
        lp = p;

        // 记住上次的大小
        try {
            android.content.SharedPreferences sp = getSharedPreferences("panel", Context.MODE_PRIVATE);
            savedW = sp.getInt("w", 0);
            savedH = sp.getInt("h", 0);
            folded = sp.getBoolean("folded", false);
        } catch (Throwable e) {
        }
        // 宽度始终显式指定: 内部布局才有确定宽度, 否则徽章会把按钮挤出面板
        if (savedW > 100) {
            p.width = savedW;
        } else {
            android.util.DisplayMetrics dm0 = getResources().getDisplayMetrics();
            p.width = (int) (340 * dm0.density);
            if (p.width > dm0.widthPixels - 20) p.width = dm0.widthPixels - 20;
            savedW = p.width;
        }
        if (savedH > 100) p.height = savedH;

        tvStatus = (TextView) view.findViewById(R.id.tvStatus);
        tvType = (TextView) view.findViewById(R.id.tvType);
        tvSize = (TextView) view.findViewById(R.id.tvSize);
        llReplies = (LinearLayout) view.findViewById(R.id.llReplies);
        spType = (android.widget.Spinner) view.findViewById(R.id.spType);
        tvMem = (TextView) view.findViewById(R.id.tvMem);
        etDraft = (EditText) view.findViewById(R.id.etDraft);
        panelBody = (LinearLayout) view.findViewById(R.id.panelBody);
        btnGen = (Button) view.findViewById(R.id.btnGen);
        btnFold = (android.widget.ImageButton) view.findViewById(R.id.btnFold);
        resizeHandle = view.findViewById(R.id.resizeHandle);
        initTypeSpinner();

        // 定时刷新内存占用, 让用户看得到清理效果
        try {
            memTimer = new java.util.Timer();
            memTimer.schedule(new java.util.TimerTask() {
                public void run() {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        public void run() {
                            refreshMemText();
                        }
                    });
                }
            }, 1000, 5000);
        } catch (Throwable t) {
        }

        // 拖动标题栏
        view.findViewById(R.id.header).setOnTouchListener(new View.OnTouchListener() {
            float dx, dy;
            int x0, y0;

            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        dx = e.getRawX();
                        dy = e.getRawY();
                        x0 = p.x;
                        y0 = p.y;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        p.x = x0 + (int) (e.getRawX() - dx);
                        p.y = y0 + (int) (e.getRawY() - dy);
                        try {
                            wm.updateViewLayout(view, p);
                        } catch (Throwable t) {
                        }
                        return true;
                }
                return false;
            }
        });

        view.findViewById(R.id.btnRead).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                readClip();
            }
        });
        btnGen.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                generate();
            }
        });
        view.findViewById(R.id.btnFill).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // 用编辑框里的内容(用户可能改过), 没有就用选中的候选
                String d = draftText();
                if (d.isEmpty()) {
                    toast("先点一条候选回复");
                    return;
                }
                picked = d;
                useReply(d);
            }
        });
        // 撤回: 取消选中 + 清编辑框 + 清掉已填入的内容
        view.findViewById(R.id.btnUndo).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                undoReply();
            }
        });
        view.findViewById(R.id.btnCtx).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                readContext();
            }
        });
        view.findViewById(R.id.btnShot).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                analyzeByShot();
            }
        });
        // 清上下文: 换人聊天时把上一个会话的痕迹清干净
        view.findViewById(R.id.btnClearCtx).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Cleaner.Report rep = Cleaner.clearContextOnly(FloatService.this);
                setStatus("已清空上下文与候选回复\n" + Cleaner.memoryText());
                toast(rep.text());
                Logx.quiet("清上下文: " + rep.text());
            }
        });
        // 清理内存: 截图 + 卡片 + 上下文 + 录屏会话
        view.findViewById(R.id.btnCleanup).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Cleaner.Report rep = Cleaner.cleanup(FloatService.this, true);
                setStatus("清理完成\n" + rep.text() + "\n" + Cleaner.memoryText());
                toast(rep.text());
                Logx.quiet("清理: " + rep.text() + " | " + Cleaner.memoryText());
            }
        });
        view.findViewById(R.id.btnSetting).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent it = new Intent(FloatService.this, MainActivity.class);
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(it);
            }
        });
        view.findViewById(R.id.btnClose).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopSelf();
            }
        });

        view.findViewById(R.id.btnResetSize).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                resetSize();
            }
        });

        // 折叠 / 展开
        btnFold.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                toggleFold();
            }
        });

        // 右下角拖动缩放
        resizeHandle.setOnTouchListener(new View.OnTouchListener() {
            float dx, dy;
            int w0, h0;

            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        dx = e.getRawX();
                        dy = e.getRawY();
                        w0 = p.width > 0 ? p.width : view.getWidth();
                        h0 = p.height > 0 ? p.height : view.getHeight();
                        return true;
                    case MotionEvent.ACTION_MOVE: {
                        int nw = w0 + (int) (e.getRawX() - dx);
                        int nh = h0 + (int) (e.getRawY() - dy);
                        android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
                        int minW = (int) (220 * dm.density);
                        int maxW = dm.widthPixels - p.x - 10;
                        int minH = (int) (160 * dm.density);
                        int maxH = dm.heightPixels - p.y - 10;
                        if (nw < minW) nw = minW;
                        if (nw > maxW) nw = maxW;
                        if (nh < minH) nh = minH;
                        if (nh > maxH) nh = maxH;
                        p.width = nw;
                        p.height = nh;
                        try {
                            wm.updateViewLayout(view, p);
                        } catch (Throwable t) {
                        }
                        showSize(nw, nh);
                        return true;
                    }
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        savedW = p.width;
                        savedH = p.height;
                        try {
                            getSharedPreferences("panel", Context.MODE_PRIVATE).edit()
                                    .putInt("w", savedW).putInt("h", savedH).apply();
                        } catch (Throwable ex) {
                        }
                        showSize(savedW, savedH);
                        return true;
                }
                return false;
            }
        });

        // 剪贴板变化自动读取(仅在本应用是前台或默认输入法时有效)
        try {
            clipMgr = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clipListener = new android.content.ClipboardManager.OnPrimaryClipChangedListener() {
                public void onPrimaryClipChanged() {
                    String t = LoveEngine.readClipboard(FloatService.this);
                    if (!t.isEmpty() && !t.equals(peerMsg)) {
                        Logx.i("检测到剪贴板变化, 自动读取");
                        setPeerMsg(t);
                        tvType.setText("");
                        clearReplies();
                    }
                }
            };
            clipMgr.addPrimaryClipChangedListener(clipListener);
        } catch (Throwable t) {
            Logx.e("剪贴板监听失败", t);
        }

        try {
            wm.addView(view, p);
        } catch (Throwable t) {
            Logx.e("悬浮窗添加失败(请授予悬浮窗权限)", t);
        }
    }

    // ------------------------------------------------------------ 业务动作

    /** 读取剪贴板里对方发来的消息(多策略) */
    public void readClip() {
        String t = LoveEngine.readClipboard(this);
        if (t.isEmpty()) {
            Logx.i("剪贴板读不到(Android10+ 需要把本应用设为默认输入法)");
            toast("读不到剪贴板: 请把「恋爱键盘」设为默认输入法\n(设置 → 系统 → 语言和输入法 → 默认输入法)");
            tvStatus.setText("读不到剪贴板。\nAndroid 10+ 只有默认输入法能读剪贴板，\n请在系统设置里把「恋爱键盘」设为默认输入法。");
            return;
        }
        setPeerMsg(t);
        Logx.i("已读取对方消息: " + brief(t));
        tvType.setText("");
        clearReplies();
        toast("已读取, 点「生成回复」");
    }

    private void setPeerMsg(String t) {
        peerMsg = t;
        LoveEngine.setPeerMsg(this, t);
    }

    /** 折叠 / 展开面板 */
    private void toggleFold() {
        folded = !folded;
        panelBody.setVisibility(folded ? View.GONE : View.VISIBLE);
        btnFold.setImageResource(folded ? R.drawable.ic_unfold : R.drawable.ic_fold);
        if (lp == null) return;
        if (folded) {
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        } else if (savedH > 100) {
            lp.height = savedH;
        }
        try {
            wm.updateViewLayout(view, lp);
        } catch (Throwable t) {
        }
        try {
            getSharedPreferences("panel", Context.MODE_PRIVATE).edit().putBoolean("folded", folded).apply();
        } catch (Throwable e) {
        }
        Logx.i("面板已" + (folded ? "折叠" : "展开"));
    }

    private void showSize(int w, int h) {
        if (tvSize == null) return;
        tvSize.setText(w + "×" + h);
    }

    /** 折叠 / 展开(供脚本与网页调用) */
    public void setFolded(boolean f) {
        if (folded == f) return;
        toggleFold();
    }

    /** 恢复默认尺寸 */
    public void resetSize() {
        android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
        int w = (int) (340 * dm.density);
        int h = (int) (520 * dm.density);
        if (w > dm.widthPixels - 20) w = dm.widthPixels - 20;
        if (h > dm.heightPixels - 20) h = dm.heightPixels - 20;
        setSize(w, h);
        Logx.i("面板尺寸已重置");
    }

    /** 截图/AI 执行时把自己藏起来(否则模型会把面板当成聊天内容) */
    public void hideSelf() {
        try {
            if (view != null) {
                view.setVisibility(View.INVISIBLE);
                wm.updateViewLayout(view, lp);
            }
        } catch (Throwable t) {
        }
    }

    /** 把自己显示回来 */
    public void showSelf() {
        try {
            if (view != null) {
                view.setVisibility(View.VISIBLE);
                wm.updateViewLayout(view, lp);
            }
        } catch (Throwable t) {
        }
    }

    /**
     * 初始化回复类型下拉框
     * 类型来自设置页的配置(可自定义), 选中后立即生效
     */
    /** 重新读取配置并刷新下拉框选中项 */
    public void refreshTypeSpinner() {
        initTypeSpinner();
    }

    private void initTypeSpinner() {
        if (spType == null) return;
        try {
            final Config cfg = Config.load(this);
            String raw = cfg.types == null ? "" : cfg.types.trim();
            List<String> types = new ArrayList<String>();
            if (!raw.isEmpty()) {
                for (String t : raw.split("[,，\\s]+")) {
                    if (!t.trim().isEmpty()) types.add(t.trim());
                }
            }
            // 第一个是"自动判断"(不限定类型), 其余是具体类型
            List<String> all = new ArrayList<String>();
            all.add("自动判断");
            all.addAll(types);

            final ArrayAdapter<String> ad = new ArrayAdapter<String>(this,
                    android.R.layout.simple_spinner_item, all);
            ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spType.setAdapter(ad);

            // 选中保存过的类型
            String cur = cfg.currentType == null ? "" : cfg.currentType.trim();
            int idx = cur.isEmpty() ? 0 : all.indexOf(cur);
            if (idx < 0) idx = 0;
            spType.setSelection(idx);

            spType.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    try {
                        Object o = parent.getItemAtPosition(position);
                        String pick = o == null ? "" : String.valueOf(o);
                        Config c = Config.load(FloatService.this);
                        // "自动判断" = 不限定类型, 用完整类型表
                        c.currentType = ("自动判断".equals(pick)) ? "" : pick;
                        c.save(FloatService.this);
                        Logx.quiet("回复类型切换为: " + (c.currentType.isEmpty() ? "自动判断" : c.currentType));
                    } catch (Throwable t) {
                    }
                }

                public void onNothingSelected(android.widget.AdapterView<?> parent) {
                }
            });
        } catch (Throwable t) {
            Logx.e("类型下拉框初始化失败", t);
        }
    }

    /** 供 Cleaner 拿候选区容器 */
    public LinearLayout repliesBox() {
        return llReplies;
    }

    /** 供 Cleaner 清空候选区 */
    public void clearRepliesView() {
        if (llReplies != null) llReplies.removeAllViews();
        replies.clear();
        picked = "";
    }

    /** 供 Cleaner 清掉对方消息 */
    public void clearPeerMessage() {
        peerMsg = "";
    }

    /** 刷新底部的内存占用显示 */
    private void refreshMemText() {
        if (tvMem == null) return;
        try {
            String t = Cleaner.memoryText();
            if (t != null && !t.isEmpty()) tvMem.setText(t);
        } catch (Throwable e) {
        }
    }

    /** 直接设置悬浮窗尺寸(px) */
    public void setSize(int w, int h) {
        if (lp == null) return;
        android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
        int minW = (int) (220 * dm.density);
        int maxW = dm.widthPixels - lp.x - 10;
        int minH = (int) (160 * dm.density);
        int maxH = dm.heightPixels - lp.y - 10;
        if (w < minW) w = minW;
        if (w > maxW) w = maxW;
        if (h < minH) h = minH;
        if (h > maxH) h = maxH;
        lp.width = w;
        lp.height = h;
        savedW = w;
        savedH = h;
        try {
            wm.updateViewLayout(view, lp);
        } catch (Throwable t) {
        }
        try {
            getSharedPreferences("panel", Context.MODE_PRIVATE).edit()
                    .putInt("w", savedW).putInt("h", savedH).apply();
        } catch (Throwable e) {
        }
        showSize(w, h);
        Logx.i("面板尺寸设为 " + w + "x" + h);
    }

    /**
     * 读取当前聊天窗口的上下文(用无障碍节点树, 比 OCR 快且准)
     */
    public void readContext() {
        List<String> ctx = AccessService.chatContext(12);
        if (ctx == null || ctx.isEmpty()) {
            Logx.i("没读到聊天上下文(对方App可能是自绘界面)");
            toast("没读到聊天内容, 试试「截图分析」");
            return;
        }
        LoveEngine.setContext(this, ctx);
        // 最后一条通常就是对方最新说的
        String last = ctx.get(ctx.size() - 1);
        setPeerMsg(last);
        Logx.i("读到 " + ctx.size() + " 条上下文, 最新: " + brief(last));
        for (String s : ctx) Logx.i("  · " + brief(s));
        tvStatus.setText("已读到 " + ctx.size() + " 条聊天记录:\n"
                + joinTail(ctx, 4) + "\n点「生成回复」按上下文回复");
        toast("已读到 " + ctx.size() + " 条上下文");
    }

    /**
     * 截图 + 视觉模型分析(节点树读不到时的兜底)
     *
     * 流程: 截图 → 裁剪+缩放+压缩 → 阶段1读图 → 阶段2写回复
     * 任一阶段失败都会自动降级, 不会让用户卡在空白
     */
    public void analyzeByShot() {
        final Config cfg = Config.load(this);
        if (cfg.glmKey == null || cfg.glmKey.trim().isEmpty()) {
            toast("请先在设置里填 API Key");
            return;
        }
        // 节点树上下文(读得到就用, 读不到就完全靠截图)
        final List<String> ctx = AccessService.chatContext(12);
        toast("截图中...");
        setStatus("① 截图中...");

        new Thread(new Runnable() {
            public void run() {
                long t0 = System.currentTimeMillis();
                try {
                    // 关键: 先把自己藏起来, 否则截图里会带上悬浮窗面板,
                    // 模型会把面板内容当成聊天消息来读
                    hideSelf();
                    android.graphics.Bitmap bmp = null;
                    try {
                        bmp = ScreenCap.capture();
                    } finally {
                        showSelf();
                    }
                    if (bmp == null) {
                        Logx.i("截图失败: " + ScreenCap.lastInfo);
                        postToast("截图失败: " + ScreenCap.lastInfo);
                        return;
                    }
                    long shotMs = System.currentTimeMillis() - t0;

                    // 图像预处理: 裁剪聊天区 + 长边压到 1280 + JPEG
                    // 原始图 1080x2400 ARGB 约 10MB, 用完立即回收, 不留在内存里
                    android.graphics.Rect focus = null;
                    if (cfg.shotCrop) focus = AccessService.chatListBounds();
                    ImgPrep.Out prep;
                    try {
                        prep = ImgPrep.prepare(bmp, focus, cfg.shotMaxEdge, cfg.shotQuality);
                    } finally {
                        try {
                            if (!bmp.isRecycled()) bmp.recycle();
                        } catch (Throwable t) {
                        }
                        Logx.quiet("原始截图已回收");
                    }
                    Logx.i("截图 " + shotMs + "ms, 预处理后 " + prep.detail()
                            + (focus != null ? " (已裁剪聊天区)" : " (未裁剪)"));
                    if (prep.jpeg == null || prep.jpeg.length < 1000) {
                        postToast("图像处理失败");
                        return;
                    }
                    final String imgInfo = prep.detail();
                    postStatus("② 读图中(" + imgInfo + ")...");

                    // 阶段1+2
                    String msg = peerMsg;
                    if (msg.isEmpty() && ctx != null && !ctx.isEmpty()) msg = ctx.get(ctx.size() - 1);
                    if (msg.isEmpty()) msg = "(请从截图里识别对方说了什么)";
                    final String m2 = msg;
                    LoveAI.Result r = LoveAI.analyze(cfg, m2, ctx, prep.jpeg);

                    // 裁剪裁歪了(读出来是界面文案) -> 自动用整屏重试一次
                    if (r.noContent && focus != null) {
                        Logx.i("裁剪版没读到内容, 回退整屏重试");
                        postStatus("② 裁剪未命中, 整屏重试...");
                        ImgPrep.Out full = ImgPrep.prepare(ScreenCap.capture(), null,
                                cfg.shotMaxEdge, cfg.shotQuality);
                        if (full.jpeg != null && full.jpeg.length >= 1000) {
                            r = LoveAI.analyze(cfg, m2, ctx, full.jpeg);
                        }
                    }
                    r.imageInfo = imgInfo;
                    Logx.i("分析完成: 读图 " + (r.msVision / 1000.0f) + "s + 生成 "
                            + (r.msText / 1000.0f) + "s, 共 " + ((System.currentTimeMillis() - t0) / 1000.0f) + "s");

                    final LoveAI.Result rf = r;
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        public void run() {
                            showResult(rf);
                        }
                    });
                } catch (final Throwable t) {
                    Logx.e("截图分析失败", t);
                    postToast("分析失败: " + t.getMessage());
                }
            }
        }).start();
    }

    /** 在主线程更新状态区 */
    private void postStatus(final String s) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            public void run() {
                setStatus(s);
            }
        });
    }

    /** 在主线程弹提示 */
    private void postToast(final String s) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            public void run() {
                toast(s);
            }
        });
    }

    private static String joinTail(List<String> list, int n) {
        StringBuilder sb = new StringBuilder();
        int from = Math.max(0, list.size() - n);
        for (int i = from; i < list.size(); i++) {
            sb.append("· ").append(brief(list.get(i))).append("\n");
        }
        return sb.toString();
    }

    /** AI 分类 + 生成候选回复 */
    public void generate() {
        if (peerMsg.isEmpty()) {
            readClip();
            if (peerMsg.isEmpty()) return;
        }
        final Config cfg = Config.load(this);
        if (cfg.glmKey == null || cfg.glmKey.trim().isEmpty()) {
            toast("请先在设置里填 API Key");
            Logx.i("未配置 API Key");
            return;
        }
        btnGen.setEnabled(false);
        tvType.setText("分析中...");
        new Thread(new Runnable() {
            public void run() {
                try {
                    final LoveAI.Result r = LoveEngine.analyze(FloatService.this, peerMsg);
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        public void run() {
                            showResult(r);
                            btnGen.setEnabled(true);
                        }
                    });
                } catch (final Throwable t) {
                    Logx.e("生成失败", t);
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        public void run() {
                            tvType.setText("生成失败");
                            btnGen.setEnabled(true);
                            toast("生成失败: " + t.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    private void showResult(LoveAI.Result r) {
        // 截图里没找到聊天内容: 明确提示, 不硬编回复
        if (r.noContent && r.replies.isEmpty()) {
            tvType.setText("未读到");
            tvType.setBackgroundResource(R.drawable.bg_badge);
            tvType.setVisibility(View.VISIBLE);
            setStatus("截图里没找到聊天内容\n请先切到聊天窗口再点「截图分析」");
            toast("截图里没找到聊天内容");
            return;
        }

        // 徽章只放类型(宽度有限), 意图放到状态区完整显示
        tvType.setText(r.type);
        tvType.setBackgroundResource(R.drawable.bg_badge);
        tvType.setVisibility(View.VISIBLE);

        // 状态区: 意图 + 读到的原话 + 耗时/体积(让优化效果可见)
        StringBuilder st = new StringBuilder();
        if (!r.intent.isEmpty()) st.append("【").append(r.type).append("】").append(r.intent);
        if (!r.readMsg.isEmpty()) {
            if (st.length() > 0) st.append("\n");
            st.append("读到: ").append(r.readMsg.length() > 60 ? r.readMsg.substring(0, 60) + "…" : r.readMsg);
        }
        String timing = r.timing();
        if (!timing.isEmpty() || !r.imageInfo.isEmpty()) {
            if (st.length() > 0) st.append("\n");
            st.append(timing);
            if (!r.imageInfo.isEmpty()) st.append(" · 图 ").append(r.imageInfo);
        }
        setStatus(st.toString());
        clearReplies();
        replies.clear();
        replies.addAll(r.replies);
        LoveEngine.setReplies(this, replies);
        LoveImeService.SHARED.clear();
        LoveImeService.SHARED.addAll(r.replies);
        // 有结果时自动展开
        if (folded) {
            folded = false;
            panelBody.setVisibility(View.VISIBLE);
            btnFold.setImageResource(R.drawable.ic_fold);
            if (savedH > 100 && lp != null) lp.height = savedH;
            try {
                wm.updateViewLayout(view, lp);
            } catch (Throwable t) {
            }
        }
        for (int idx = 0; idx < r.replies.size(); idx++) {
            final String rep = r.replies.get(idx);
            final TextView tv = new TextView(this);
            tv.setText((idx + 1) + ". " + rep);
            tv.setTextSize(12);
            tv.setTextColor(0xFF3A3A3A);
            tv.setBackgroundResource(R.drawable.bg_reply);
            tv.setPadding(10, 8, 10, 8);
            tv.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    // 选中: 取消上一个高亮, 本条高亮, 文本进编辑框
                    selectCard(tv, rep);
                    // 点一下就发: 复制备用 + 填入输入框 + 点发送
                    Clip.write(FloatService.this, rep);
                    useReply(rep);
                }
            });
            // 长按: 只进编辑框不发送(给用户先改再发的机会)
            tv.setOnLongClickListener(new View.OnLongClickListener() {
                public boolean onLongClick(View v) {
                    selectCard(tv, rep);
                    Clip.write(FloatService.this, rep);
                    toast("已放入编辑框, 可修改后点「填入发送」");
                    Logx.quiet("长按选中(不发送): " + brief(rep));
                    return true;
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = 6;
            llReplies.addView(tv, lp);
        }
        if (r.replies.isEmpty()) {
            Logx.i("没有解析出候选回复, 原始输出: " + brief(r.raw));
            tvType.setText("【" + r.type + "】(未解析出回复)");
        } else {
            Logx.quiet("生成 " + r.replies.size() + " 条候选回复");
        }
    }

    /**
     * 选中一条候选: 取消上一个高亮, 高亮当前, 文本进编辑框
     * 编辑框里的内容可人工修改, 改完点「填入发送」发出修改后的版本
     */
    private void selectCard(TextView card, String text) {
        if (pickedCard != null && pickedCard != card) {
            try {
                pickedCard.setBackgroundResource(R.drawable.bg_reply);
                pickedCard.setTextColor(0xFF3A3A3A);
            } catch (Throwable t) {
            }
        }
        pickedCard = card;
        picked = text;
        if (card != null) {
            try {
                card.setBackgroundResource(R.drawable.bg_reply_sel);
                card.setTextColor(0xFF7A5B00);
            } catch (Throwable t) {
            }
        }
        // 文本进编辑框(用户可以人工改)
        try {
            if (etDraft != null) etDraft.setText(text);
        } catch (Throwable t) {
        }
    }

    /** 撤回: 取消选中 + 清编辑框 + 清掉已填入对方输入框的内容 + 清剪贴板 */
    public void undoReply() {
        // 取消高亮
        if (pickedCard != null) {
            try {
                pickedCard.setBackgroundResource(R.drawable.bg_reply);
                pickedCard.setTextColor(0xFF3A3A3A);
            } catch (Throwable t) {
            }
            pickedCard = null;
        }
        // 清编辑框
        try {
            if (etDraft != null) etDraft.setText("");
        } catch (Throwable t) {
        }
        picked = "";
        lastFilled = "";

        // 清掉对方输入框里我们填进去的文本
        boolean cleared = false;
        try {
            cleared = AccessService.clearChatInput();
        } catch (Throwable t) {
        }
        // 清剪贴板里的备份
        try {
            Clip.write(this, "");
        } catch (Throwable t) {
        }

        String msg = cleared ? "已撤回: 选中已取消, 输入框内容已清空"
                : "已撤回: 选中已取消" + (lastFilled.isEmpty() ? "" : " (对方App输入框未找到, 需手动删除)");
        setStatus(msg);
        toast(cleared ? "已撤回" : "已撤回(未找到输入框)");
        Logx.quiet("撤回: " + msg);
    }

    /** 取当前编辑框里的话术(用户可能改过); 空则回退到选中的候选 */
    public String draftText() {
        try {
            if (etDraft != null) {
                String t = etDraft.getText().toString().trim();
                if (!t.isEmpty()) return t;
            }
        } catch (Throwable t) {
        }
        return picked == null ? "" : picked;
    }

    /**
     * 选中第 n 条候选回复(脚本/网页可调用)
     * 只选中并放进编辑框, **不发送** —— 这样可以先改再发
     * 要连发用 pickAndSend()
     */
    public void pick(int index) {
        if (replies.isEmpty()) {
            Logx.i("还没有候选回复, 先执行生成");
            return;
        }
        int i = Math.max(0, Math.min(replies.size() - 1, index));
        String text = replies.get(i);
        selectCard(pickedCard, text);
        Clip.write(this, text);
        Logx.quiet("选中第" + (i + 1) + "条(待发送): " + brief(text));
    }

    /** 选中并立即发送(保持旧的脚本行为) */
    public void pickAndSend(int index) {
        pick(index);
        String text = draftText();
        if (!text.isEmpty()) useReply(text);
    }

    /** 设置编辑框里的话术(人工改完的版本) */
    public void setDraft(String text) {
        if (text == null) return;
        try {
            if (etDraft != null) etDraft.setText(text);
        } catch (Throwable t) {
        }
        picked = text;
        Logx.quiet("话术已改为: " + brief(text));
    }

    /** 发送编辑框里的话术(用户改过的版本) */
    public void sendDraft() {
        String text = draftText();
        if (text.isEmpty()) {
            toast("先点一条候选回复, 或在编辑框里写内容");
            Logx.i("编辑框是空的, 无法发送");
            return;
        }
        useReply(text);
    }

    /**
     * 使用一条回复: 填进当前输入框, 并(可选)直接点发送
     */
    public void useReply(String text) {
        if (text == null || text.trim().isEmpty()) {
            toast("回复内容为空");
            return;
        }
        Config cfg = Config.load(this);
        String pkg = AccessService.foregroundPkg();
        if (cfg.targetApps != null && !cfg.targetApps.trim().isEmpty()) {
            boolean ok = false;
            for (String a : cfg.targetApps.split("[,，\\s]+")) {
                if (!a.isEmpty() && a.equals(pkg)) ok = true;
            }
            if (!ok) {
                toast("当前不是目标App(" + pkg + "), 已改为复制");
                Clip.write(this, text);
                return;
            }
        }
        // ① 若本应用正是当前输入法, 走 IME 通道写入+发送(最可靠, 不受键盘遮挡影响)
        if (cfg.autoSend) {
            try {
                String def = android.provider.Settings.Secure.getString(getContentResolver(),
                        "default_input_method");
                if (def != null && def.contains("lovekb") && LoveImeService.I != null) {
                    if (LoveImeService.I.commitAndSend(text)) {
                        toast("已发送");
                        Logx.i("已发送(输入法通道): " + brief(text));
                        return;
                    }
                }
            } catch (Throwable e) {
            }
        }
        // ② 无障碍通道: 填进输入框
        if (!AccessService.fillChatInput(text)) {
            Clip.write(this, text);
            toast("没找到输入框, 已复制请手动粘贴");
            Logx.i("未找到聊天输入框, 已复制");
            return;
        }
        // 记下填进去的内容, 撤回时按这个清掉
        lastFilled = text;
        Logx.i("已填入输入框: " + brief(text));
        if (!cfg.autoSend) {
            toast("已填入, 点发送键发出");
            return;
        }
        // ③ 在界面上找「发送」按钮点它
        boolean sent = false;
        for (int i = 0; i < 3 && !sent; i++) {
            try {
                Thread.sleep(i == 0 ? 450 : 700);
            } catch (InterruptedException ie) {
            }
            sent = AccessService.clickSend();
        }
        if (sent) {
            toast("已发送");
            Logx.i("已发送: " + brief(text));
        } else {
            toast("已填入, 没找到发送键, 请手动点发送");
            Logx.i("已填入但未找到发送按钮");
        }
    }

    /** 把选中的回复直接填进对方 App 的聊天输入框 */
    public void fillIntoChat() {
        // 优先用编辑框里的内容(用户可能改过)
        String text = draftText();
        if (text.isEmpty()) {
            // 没生成候选时, 直接用剪贴板里的内容(手动复制好回复后也能用)
            text = Clip.read(this);
            if (text.isEmpty()) {
                toast("先点一条候选回复, 或先复制要发的内容");
                return;
            }
            Logx.i("未选中候选, 使用剪贴板内容填入");
        }
        final String reply = text;
        Config cfg = Config.load(this);
        String pkg = AccessService.foregroundPkg();
        if (cfg.targetApps != null && !cfg.targetApps.trim().isEmpty()) {
            boolean ok = false;
            for (String a : cfg.targetApps.split("[,，\\s]+")) {
                if (!a.isEmpty() && a.equals(pkg)) ok = true;
            }
            if (!ok) {
                toast("当前不是目标App(" + pkg + "), 已改为复制");
                Clip.write(this, reply);
                return;
            }
        }
        if (AccessService.fillChatInput(reply)) {
            toast("已填入聊天输入框");
            Logx.i("已填入输入框: " + brief(reply));
        } else {
            Clip.write(this, reply);
            toast("没找到输入框, 已复制请手动粘贴");
            Logx.i("未找到聊天输入框, 已复制");
        }
    }

    // ------------------------------------------------------------ 杂项

    /** 覆盖状态区文字(完整显示, 不追加) */
    private void setStatus(String s) {
        if (tvStatus == null) return;
        tvStatus.setText(s);
    }

    private void clearReplies() {
        if (llReplies != null) llReplies.removeAllViews();
    }

    private void appendStatus(final String s) {
        if (tvStatus == null) return;
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            public void run() {
                try {
                    tvStatus.append(s + "\n");
                    int max = tvStatus.getLayout() == null ? 0 : tvStatus.getLayout().getLineCount();
                    if (max > 60) {
                        CharSequence cs = tvStatus.getText();
                        int cut = cs.length() / 2;
                        tvStatus.setText(cs.subSequence(cut, cs.length()));
                    }
                } catch (Throwable t) {
                }
            }
        });
    }

    private void toast(final String s) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            public void run() {
                try {
                    Toast.makeText(FloatService.this, s, Toast.LENGTH_SHORT).show();
                } catch (Throwable t) {
                }
            }
        });
    }

    private static String brief(String s) {
        if (s == null) return "";
        return s.length() <= 24 ? s : s.substring(0, 24) + "…";
    }

    @Override
    public void onDestroy() {
        try {
            if (memTimer != null) memTimer.cancel();
        } catch (Throwable t) {
        }
        Logx.sink = null;
        try {
            if (view != null) wm.removeView(view);
        } catch (Throwable t) {
        }
        super.onDestroy();
    }
}
