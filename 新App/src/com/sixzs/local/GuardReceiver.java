package com.sixzs.local;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

/**
 * 广播入口: 让自动化脚本/adb 也能触发扫描与配置
 *   adb shell am broadcast -n com.sixzs.local/.GuardReceiver -a com.sixzs.local.SCAN_GUARD
 *   adb shell am broadcast -n com.sixzs.local/.GuardReceiver -a com.sixzs.local.SET_CONFIG \
 *        --ez autoDmReply true --es autoDmText "你好呀" --es autoDmApps "com.ss.android.ugc.aweme"
 */
public class GuardReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String act = intent == null ? "" : String.valueOf(intent.getAction());
        if (act.contains("SET_CONFIG")) {
            applyConfig(context, intent);
            return;
        }
        if (act.contains("START_AI")) {
            startAi(context);
            return;
        }
        if (act.contains("STOP_AI")) {
            GlmAgent.stop();
            return;
        }
        if (act.contains("REPORT_NEXT")) {
            FloatService.reportNext();
            return;
        }
        if (act.contains("START_SCRIPT")) {
            ensureService(context);
            LuaHost.start(context, "main.lua");
            return;
        }
        if (act.contains("STOP_SCRIPT")) {
            LuaHost.stop();
            return;
        }
        if (act.contains("DM_GUARD_ON")) {
            ensureService(context);
            FloatService.startDmGuard(context);
            return;
        }
        if (act.contains("DM_GUARD_OFF")) {
            FloatService.stopDmGuard();
            return;
        }
        if (!act.contains("SCAN_GUARD")) return;
        ensureService(context);
        FloatService.startScan(context);
    }

    private void ensureService(Context context) {
        try {
            Intent svc = new Intent(context, FloatService.class);
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(svc);
            else context.startService(svc);
        } catch (Throwable t) {
        }
    }

    /** 用当前配置启动 AI 任务 */
    private void startAi(Context context) {
        try {
            Config cfg = Config.load(context);
            String task = cfg.aiTask == null ? "" : cfg.aiTask.trim();
            if (task.isEmpty()) {
                Logx.i("AI: 未设置任务, 请先在网页填写任务后保存");
                return;
            }
            if (cfg.glmKey == null || cfg.glmKey.trim().isEmpty()) {
                Logx.i("AI: 未设置 API Key");
                return;
            }
            ensureService(context);
            GlmAgent.start(context, task, cfg.glmKey.trim(), cfg.aiModel, cfg.aiMaxSteps);
        } catch (Throwable t) {
            Logx.e("AI 启动失败", t);
        }
    }

    /** 通过广播改配置(自动回复内容等) */
    private void applyConfig(Context context, Intent intent) {
        try {
            SharedPreferences.Editor ed = context.getSharedPreferences("cfg", Context.MODE_PRIVATE).edit();
            boolean changed = false;
            if (intent.hasExtra("autoDmReply")) {
                ed.putBoolean("autoDmReply", intent.getBooleanExtra("autoDmReply", false));
                changed = true;
            }
            if (intent.hasExtra("autoDmText")) {
                ed.putString("autoDmText", String.valueOf(intent.getStringExtra("autoDmText")));
                changed = true;
            }
            if (intent.hasExtra("autoDmApps")) {
                ed.putString("autoDmApps", String.valueOf(intent.getStringExtra("autoDmApps")));
                changed = true;
            }
            if (intent.hasExtra("autoDmMaxPerHour")) {
                ed.putInt("autoDmMaxPerHour", intent.getIntExtra("autoDmMaxPerHour", 10));
                changed = true;
            }
            if (intent.hasExtra("riskQQ")) {
                ed.putString("riskQQ", String.valueOf(intent.getStringExtra("riskQQ")));
                changed = true;
            }
            if (intent.hasExtra("aiTask")) {
                ed.putString("aiTask", String.valueOf(intent.getStringExtra("aiTask")));
                changed = true;
            }
            if (intent.hasExtra("aiModel")) {
                ed.putString("aiModel", String.valueOf(intent.getStringExtra("aiModel")));
                changed = true;
            }
            if (intent.hasExtra("aiMaxSteps")) {
                ed.putInt("aiMaxSteps", intent.getIntExtra("aiMaxSteps", 15));
                changed = true;
            }
            if (intent.hasExtra("glmKey")) {
                String k = String.valueOf(intent.getStringExtra("glmKey")).trim();
                // 空值不清掉已保存的 Key
                if (!k.isEmpty()) {
                    ed.putString("glmKey", k);
                    changed = true;
                }
            }
            if (changed) {
                ed.apply();
                Logx.i("配置已通过广播更新");
            }
        } catch (Throwable t) {
            Logx.e("广播改配置失败", t);
        }
    }
}
