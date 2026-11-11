package com.sixzs.local;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
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
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.START;
        p.x = 20;
        p.y = 150;
        tvStatus = (TextView) view.findViewById(R.id.tvStatus);
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
                LuaHost.start(FloatService.this, "main.lua");
                setRunning(true);
            }
        });

        view.findViewById(R.id.btnStop).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                LuaHost.stop();
                setRunning(false);
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

        wm.addView(view, p);
    }

    public static void setRunning(boolean run) {
        FloatService s = I;
        if (s != null && s.btnStart != null) s.btnStart.setText(run ? "运行中" : "开始");
    }

    public static void setStatus(final String txt) {
        final FloatService s = I;
        if (s == null) return;
        UI.post(new Runnable() {
            public void run() {
                if (s.tvStatus != null) s.tvStatus.setText(txt);
            }
        });
    }

    @Override
    public void onDestroy() {
        LuaHost.stop();
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
