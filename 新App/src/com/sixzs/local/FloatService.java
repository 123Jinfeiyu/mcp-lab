package com.sixzs.local;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class FloatService extends Service {

    private static FloatService I;
    private WindowManager wm;
    private View view;
    private TextView tvStatus;
    private TextView tvTitle;
    private Button btnStart;
    private static final Handler UI = new Handler(Looper.getMainLooper());

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("capCode")) {
            ScreenCap.init(this, intent.getIntExtra("capCode", 0),
                    (Intent) intent.getParcelableExtra("capData"));
        }
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        I = this;
        startForeground(1, buildNotification());
        initOverlay();
    }

    private Notification buildNotification() {
        String ch = "dm_local";
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel c = new NotificationChannel(ch, "运行状态", NotificationManager.IMPORTANCE_LOW);
            nm.createNotificationChannel(c);
            return new Notification.Builder(this, ch)
                    .setContentTitle("AI私信助手本地版")
                    .setContentText("悬浮窗运行中")
                    .setSmallIcon(android.R.drawable.stat_notify_sync)
                    .build();
        }
        return new Notification.Builder(this)
                .setContentTitle("AI私信助手本地版")
                .setContentText("悬浮窗运行中")
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .build();
    }

    private void initOverlay() {
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
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
        tvStatus = (TextView) view.findViewById(R.id.tvStatus);
        tvStatus.setMovementMethod(new android.text.method.ScrollingMovementMethod());
        tvTitle = (TextView) view.findViewById(R.id.tvTitle);
        btnStart = (Button) view.findViewById(R.id.btnStart);

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
                        wm.updateViewLayout(view, p);
                        return true;
                }
                return false;
            }
        });

        btnStart.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Logx.i("浮窗按钮点击: 开始");
                try {
                    if (LuaHost.isRunning()) {
                        // 运行中点击 = 热重启: 停止 -> 重新读取 sdcard 覆盖目录里的最新脚本 -> 启动
                        Logx.clear();
                        Logx.i("热重启...");
                        LuaHost.restart(FloatService.this, "main.lua");
                    } else {
                        Logx.clear();
                        LuaHost.start(FloatService.this, "main.lua");
                    }
                    setRunning(true);
                } catch (Throwable t) {
                    // 绝不能让脚本异常把整个 App 带崩
                    Logx.e("启动脚本失败", t);
                }
            }
        });

        view.findViewById(R.id.btnStop).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                LuaHost.stop();
                GlmAgent.stop();
                setRunning(false);
            }
        });

        view.findViewById(R.id.btnAi).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Config cfg = Config.load(FloatService.this);
                if (cfg.glmKey == null || cfg.glmKey.trim().isEmpty()) {
                    Logx.i("AI: 未配置 API Key, 去设置页填写");
                    return;
                }
                if (cfg.aiTask == null || cfg.aiTask.trim().isEmpty()) {
                    Logx.i("AI: 未填任务描述, 去设置页填写");
                    return;
                }
                if (GlmAgent.isRunning()) {
                    GlmAgent.stop();
                    return;
                }
                Logx.clear();
                GlmAgent.start(FloatService.this, cfg.aiTask, cfg.glmKey,
                        cfg.aiModel, cfg.aiMaxSteps);
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

        try {
            view.findViewById(R.id.btnGuard).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Logx.i("浮窗按钮点击: 风控扫描");
                doGuardScan();
            }
        });

        view.findViewById(R.id.btnReport).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Logx.i("浮窗按钮点击: 举报下一条");
                reportNext();
            }
        });

        view.findViewById(R.id.btnDmGuard).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Logx.i("浮窗按钮点击: 私信守候");
                toggleDmGuard();
            }
        });

        wm.addView(view, p);
        } catch (Throwable t) {
            // 没有悬浮窗权限时不能拖垮整个服务: 服务仍需存活(截图授权/AI执行都依赖它)
            Logx.e("悬浮窗添加失败(请授予悬浮窗权限)", t);
            try {
                if (view != null) view = null;
            } catch (Exception e) {
            }
        }
    }

    /** 评论区风控: 扫描当前前台界面并做 AI 风险识别 */
    private static final java.util.List<CommentGuard.Comment> guardItems =
            new java.util.ArrayList<CommentGuard.Comment>();
    private static int guardCursor = 0;

    /** 供悬浮窗按钮 / 广播 / 脚本调用 */
    public static void startScan(final Context ctx) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    final Config cfg = Config.load(ctx);
                    final java.util.List<CommentGuard.Comment> items;
                    if (cfg.guardScroll) {
                        items = CommentGuard.scanDeep(cfg.guardScrollRounds, 200);
                    } else {
                        items = CommentGuard.scan();
                    }
                    if (cfg.glmKey != null && !cfg.glmKey.trim().isEmpty() && !items.isEmpty()) {
                        try {
                            CommentGuard.classify(items, cfg.glmKey, cfg.aiModel);
                        } catch (Exception e) {
                            Logx.i("风控: AI 分类失败 " + e.getMessage());
                        }
                    }
                    final java.util.List<CommentGuard.Comment> risk = new java.util.ArrayList<CommentGuard.Comment>();
                    final java.util.List<CommentGuard.Comment> leads = new java.util.ArrayList<CommentGuard.Comment>();
                    for (CommentGuard.Comment c : items) {
                        if ("high".equals(c.level) || "mid".equals(c.level)) risk.add(c);
                        if (CommentGuard.hasIntent(c) && !"high".equals(c.level)) leads.add(c);
                    }
                    guardItems.clear();
                    guardItems.addAll(risk);
                    guardCursor = 0;
                    Logx.i("风控扫描: 候选 " + items.size() + " 条, 风险 " + risk.size() + " 条");
                    for (CommentGuard.Comment c : risk) {
                        Logx.i("[" + c.label + "/" + c.level + "] " + c.text
                                + (c.reason.isEmpty() ? "" : " — " + c.reason)
                                + (c.reply.isEmpty() ? "" : " | 话术: " + c.reply));
                    }
                    if (!leads.isEmpty()) {
                        Logx.i("—— 发现 " + leads.size() + " 条有互动态向的评论 ——");
                        for (CommentGuard.Comment c : leads) {
                            Logx.i("[意向:" + c.intent + "] " + c.text
                                    + (c.reply.isEmpty() ? "" : " | 建议话术: " + c.reply));
                        }
                    }
                    if (risk.isEmpty()) {
                        Logx.i("风控: 未发现可疑引流评论");
                    } else {
                        String qq = cfg.riskQQ == null ? "" : cfg.riskQQ.trim();
                        String tip = "⚠ 检出 " + risk.size() + " 条风险评论, 风险处理请加 QQ " + qq;
                        Logx.i(tip);
                        notifyRisk(tip);
                        Logx.i("点「举报下一条」逐条举报");
                    }
                } catch (Throwable t) {
                    Logx.e("风控扫描失败", t);
                }
            }
        }).start();
    }

    /** 举报扫描结果里的下一条高风险评论 */
    public static void reportNext() {
        new Thread(new Runnable() {
            public void run() {
                if (guardItems.isEmpty()) {
                    Logx.i("风控: 请先点「风控扫描」");
                    return;
                }
                if (guardCursor >= guardItems.size()) {
                    Logx.i("风控: 已全部举报完毕, 请重新扫描");
                    return;
                }
                CommentGuard.Comment c = guardItems.get(guardCursor++);
                Logx.i("举报中: " + c.text);
                Logx.i("举报结果: " + CommentGuard.report(c));
            }
        }).start();
    }

    private void doGuardScan() {
        startScan(this);
    }

    private void reportNext(Context ignored) {
        reportNext();
    }

    /** 风险预警通知(带处理QQ) */
    private static void notifyRisk(String tip) {
        try {
            FloatService s = I;
            if (s == null) return;
            NotificationManager nm = (NotificationManager) s.getSystemService(NOTIFICATION_SERVICE);
            if (nm == null) return;
            String ch = "dm_local";
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationChannel c = new NotificationChannel(ch, "运行状态", NotificationManager.IMPORTANCE_HIGH);
                nm.createNotificationChannel(c);
                nm.notify(1001, new Notification.Builder(s, ch)
                        .setContentTitle("评论区风控预警")
                        .setContentText(tip)
                        .setSmallIcon(android.R.drawable.stat_notify_sync)
                        .setAutoCancel(true)
                        .build());
            } else {
                nm.notify(1001, new Notification.Builder(s)
                        .setContentTitle("评论区风控预警")
                        .setContentText(tip)
                        .setSmallIcon(android.R.drawable.stat_notify_sync)
                        .setAutoCancel(true)
                        .build());
            }
        } catch (Throwable t) {
        }
    }

    /** 私信守候: 后台轮询, 对方发来私信时自动回复(内容由用户在设置页写死) */
    private static volatile boolean dmGuardOn = false;
    private static Thread dmGuardThread;

    private void toggleDmGuard() {
        if (dmGuardOn) {
            stopDmGuard();
            return;
        }
        startDmGuard(this);
    }

    /** 开启私信守候(网页/脚本/悬浮窗都能调) */
    public static void startDmGuard(final Context ctx) {
        final Config cfg = Config.load(ctx);
        if (!cfg.autoDmReply) {
            Logx.i("私信守候: 请先勾选「收到私信时自动回复」并保存");
            return;
        }
        if (cfg.autoDmText == null || cfg.autoDmText.trim().isEmpty()) {
            Logx.i("私信守候: 未设置自动回复内容");
            return;
        }
        if (dmGuardOn) {
            Logx.i("私信守候: 已在运行");
            return;
        }
        dmGuardOn = true;
        DmAutoReply.reset();
        dmGuardThread = new Thread(new Runnable() {
            public void run() {
                while (dmGuardOn) {
                    try {
                        String r = DmAutoReply.tick(Config.load(ctx));
                        if (r != null && !r.isEmpty()) Logx.i("私信守候: " + r);
                    } catch (Throwable t) {
                    }
                    try {
                        Thread.sleep(2500);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
        }, "dm-guard");
        dmGuardThread.start();
        Logx.i("私信守候: 已开启, 目标 " + cfg.autoDmApps + ", 回复: " + cfg.autoDmText);
    }

    /** 停止私信守候 */
    public static void stopDmGuard() {
        if (!dmGuardOn) {
            Logx.i("私信守候: 本来就没开");
            return;
        }
        dmGuardOn = false;
        if (dmGuardThread != null) dmGuardThread.interrupt();
        dmGuardThread = null;
        Logx.i("私信守候: 已停止");
    }

    /** AI 执行期间隐藏悬浮窗, 避免遮挡视线/吃掉点击; 结束后恢复 */
    public static void setOverlayVisible(boolean visible) {
        final FloatService s = I;
        if (s == null) return;
        UI.post(new Runnable() {
            public void run() {
                try {
                    if (s.view != null) s.view.setVisibility(visible ? View.VISIBLE : View.GONE);
                } catch (Throwable t) {
                }
            }
        });
    }

    public static void setRunning(boolean run) {        FloatService s = I;
        if (s != null && s.btnStart != null) s.btnStart.setText(run ? "重启" : "开始");
    }

    public static void setLog(final String txt) {
        final FloatService s = I;
        if (s == null) return;
        UI.post(new Runnable() {
            public void run() {
                if (s.tvStatus != null) {
                    s.tvStatus.setText(txt);
                    // 自动滚到底部
                    int off = s.tvStatus.getLayout() != null
                            ? s.tvStatus.getLayout().getLineTop(s.tvStatus.getLineCount()) - s.tvStatus.getHeight()
                            : 0;
                    if (off > 0) s.tvStatus.scrollTo(0, off);
                    else s.tvStatus.scrollTo(0, 0);
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        LuaHost.stop();
        GlmAgent.stop();
        if (wm != null && view != null) {
            try {
                wm.removeView(view);
            } catch (Exception e) {
            }
        }
        I = null;
        super.onDestroy();
    }
}
