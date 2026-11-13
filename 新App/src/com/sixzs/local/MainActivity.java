package com.sixzs.local;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private EditText etName, etInterval, etStay1, etStay2, etPmSearch, etPmUid, etVideos;
    private EditText[] etC = new EditText[6];
    private EditText etScript;
    private EditText etGlmKey, etAiTask, etAiModel, etAiSteps, etAiKeyword, etRiskQQ;
    private EditText etAutoDmText, etAutoDmApps, etAutoDmMax, etGuardRounds;
    private CheckBox cbAutoDm, cbGuardScroll;
    private TextView tvDmResult;
    private TextView tvAiResult, tvGuardStat;
    private LinearLayout llRisk;
    private Button btnReportAll;
    private final java.util.List<CommentGuard.Comment> riskItems = new java.util.ArrayList<CommentGuard.Comment>();
    private CheckBox cbFenduan, cbFuhao, cbDanci, cbAi;
    private RadioButton rbSearch, rbUid;
    private TextView tvPermState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        GlmAgent.setContext(this);

        etName = (EditText) findViewById(R.id.etName);
        etInterval = (EditText) findViewById(R.id.etInterval);
        etStay1 = (EditText) findViewById(R.id.etStay1);
        etStay2 = (EditText) findViewById(R.id.etStay2);
        etPmSearch = (EditText) findViewById(R.id.etPmSearch);
        etPmUid = (EditText) findViewById(R.id.etPmUid);
        etVideos = (EditText) findViewById(R.id.etVideos);
        etC[0] = (EditText) findViewById(R.id.etC1);
        etC[1] = (EditText) findViewById(R.id.etC2);
        etC[2] = (EditText) findViewById(R.id.etC3);
        etC[3] = (EditText) findViewById(R.id.etC4);
        etC[4] = (EditText) findViewById(R.id.etC5);
        etC[5] = (EditText) findViewById(R.id.etC6);
        etScript = (EditText) findViewById(R.id.etScript);
        cbFenduan = (CheckBox) findViewById(R.id.cbFenduan);
        cbFuhao = (CheckBox) findViewById(R.id.cbFuhao);
        cbDanci = (CheckBox) findViewById(R.id.cbDanci);
        cbAi = (CheckBox) findViewById(R.id.cbAi);
        rbSearch = (RadioButton) findViewById(R.id.rbSearch);
        rbUid = (RadioButton) findViewById(R.id.rbUid);
        tvPermState = (TextView) findViewById(R.id.tvPermState);
        etGlmKey = (EditText) findViewById(R.id.etGlmKey);
        etAiTask = (EditText) findViewById(R.id.etAiTask);
        etAiModel = (EditText) findViewById(R.id.etAiModel);
        etAiSteps = (EditText) findViewById(R.id.etAiSteps);
        etAiKeyword = (EditText) findViewById(R.id.etAiKeyword);
        etRiskQQ = (EditText) findViewById(R.id.etRiskQQ);
        etAutoDmText = (EditText) findViewById(R.id.etAutoDmText);
        etAutoDmApps = (EditText) findViewById(R.id.etAutoDmApps);
        etAutoDmMax = (EditText) findViewById(R.id.etAutoDmMax);
        cbAutoDm = (CheckBox) findViewById(R.id.cbAutoDm);
        cbGuardScroll = (CheckBox) findViewById(R.id.cbGuardScroll);
        etGuardRounds = (EditText) findViewById(R.id.etGuardRounds);
        tvDmResult = (TextView) findViewById(R.id.tvDmResult);
        tvAiResult = (TextView) findViewById(R.id.tvAiResult);
        tvGuardStat = (TextView) findViewById(R.id.tvGuardStat);
        llRisk = (LinearLayout) findViewById(R.id.llRisk);
        btnReportAll = (Button) findViewById(R.id.btnReportAll);

        loadToViews();

        findViewById(R.id.btnOverlay).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(MainActivity.this)) {
                    startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName())));
                } else {
                    Toast.makeText(MainActivity.this, "悬浮窗权限已授予", Toast.LENGTH_SHORT).show();
                }
            }
        });

        findViewById(R.id.btnAccess).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            }
        });

        findViewById(R.id.btnBattery).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= 23) {
                    PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                    if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
                        try {
                            startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                    Uri.parse("package:" + getPackageName())));
                        } catch (Exception e) {
                            startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "已忽略电池优化", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        findViewById(R.id.btnIme).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS));
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "请到系统设置-语言与输入法 中启用并选中本输入法", Toast.LENGTH_LONG).show();
                }
            }
        });
        findViewById(R.id.btnIme).setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                try {
                    ((android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                            .showInputMethodPicker();
                } catch (Exception e) {
                }
                return true;
            }
        });

        findViewById(R.id.btnCap).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    android.media.projection.MediaProjectionManager mpm =
                            (android.media.projection.MediaProjectionManager)
                                    getSystemService(MEDIA_PROJECTION_SERVICE);
                    startActivityForResult(mpm.createScreenCaptureIntent(), 1001);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "截图授权失败: " + e, Toast.LENGTH_LONG).show();
                }
            }
        });

        findViewById(R.id.btnStorage).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                openStorage();
            }
        });

        findViewById(R.id.btnGuide).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(MainActivity.this)) {
                    startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName())));
                    Toast.makeText(MainActivity.this, "请授予悬浮窗权限", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!isAccessibilityOn()) {
                    startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                    Toast.makeText(MainActivity.this, "请开启无障碍服务", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!LocalImeService.isEnabled(MainActivity.this)) {
                    startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS));
                    Toast.makeText(MainActivity.this, "请启用本输入法", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!LocalImeService.isSelected(MainActivity.this)) {
                    try {
                        ((android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                                .showInputMethodPicker();
                    } catch (Exception e) {
                    }
                    Toast.makeText(MainActivity.this, "请选中「AI本地输入法」", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (Build.VERSION.SDK_INT >= 23) {
                    PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                    if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
                        try {
                            startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                    Uri.parse("package:" + getPackageName())));
                        } catch (Exception e) {
                        }
                        Toast.makeText(MainActivity.this, "请允许忽略电池优化", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                if (!storageOn()) {
                    openStorage();
                    Toast.makeText(MainActivity.this, "请授予存储权限", Toast.LENGTH_SHORT).show();
                    return;
                }
                Toast.makeText(MainActivity.this, "权限已全部就绪", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btnSave).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                saveFromViews();
                Toast.makeText(MainActivity.this, "配置已保存", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btnFloat).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                saveFromViews();
                if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(MainActivity.this)) {
                    Toast.makeText(MainActivity.this, "请先授予悬浮窗权限", Toast.LENGTH_SHORT).show();
                    return;
                }
                startService(new Intent(MainActivity.this, FloatService.class));
                Toast.makeText(MainActivity.this, "悬浮窗已启动", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btnAiTest).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                saveFromViews();
                final String key = etGlmKey.getText().toString().trim();
                final String model = etAiModel.getText().toString().trim();
                final String task = etAiTask.getText().toString().trim();
                if (key.isEmpty()) {
                    Toast.makeText(MainActivity.this, "先填 API Key", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (task.isEmpty()) {
                    Toast.makeText(MainActivity.this, "先填任务描述", Toast.LENGTH_SHORT).show();
                    return;
                }
                tvAiResult.setText("AI 思考中…");
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            final String r = GlmAgent.askOnce(key, model,
                                    "任务: " + task + "\n只描述你现在看到的界面和第一步该做什么(不要执行)");
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    tvAiResult.setText(r);
                                }
                            });
                        } catch (final Throwable e) {
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    tvAiResult.setText("失败: " + e.getMessage());
                                }
                            });
                        }
                    }
                }).start();
            }
        });

        findViewById(R.id.btnAiStart).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                saveFromViews();
                final String key = etGlmKey.getText().toString().trim();
                final String model = etAiModel.getText().toString().trim();
                final String task = etAiTask.getText().toString().trim();
                if (key.isEmpty() || task.isEmpty()) {
                    Toast.makeText(MainActivity.this, "先填 API Key 和任务描述", Toast.LENGTH_SHORT).show();
                    return;
                }
                int steps = num(etAiSteps, 15);
                GlmAgent.start(MainActivity.this, task, key, model, steps);
                Toast.makeText(MainActivity.this, "AI 已启动, 悬浮窗看进度", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btnGenTask).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String kw = etAiKeyword.getText().toString().trim();
                if (kw.isEmpty()) {
                    Toast.makeText(MainActivity.this, "先填搜索关键词", Toast.LENGTH_SHORT).show();
                    return;
                }
                String task = "打开抖音, 搜索\"" + kw + "\", 打开搜索结果里的第一个视频, "
                        + "进入该视频的评论区后停下, 等待我下一步指示";
                etAiTask.setText(task);
                Toast.makeText(MainActivity.this, "任务已生成, 点「立即启动AI执行」或悬浮窗「AI」", Toast.LENGTH_LONG).show();
            }
        });

        findViewById(R.id.btnDmTest).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                saveFromViews();
                final String r = DmAutoReply.fillOnly(Config.load(MainActivity.this));
                tvDmResult.setText(r);
                Toast.makeText(MainActivity.this, r, Toast.LENGTH_LONG).show();
            }
        });

        findViewById(R.id.btnScanOnly).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doScan(false);
            }
        });
        findViewById(R.id.btnScanAi).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doScan(true);
            }
        });

        btnReportAll.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (riskItems.isEmpty()) {
                    Toast.makeText(MainActivity.this, "没有可举报的高风险评论", Toast.LENGTH_SHORT).show();
                    return;
                }
                new Thread(new Runnable() {
                    public void run() {
                        for (final CommentGuard.Comment c : riskItems) {
                            if (!"high".equals(c.level)) continue;
                            final String r = CommentGuard.report(c);
                            Logx.i("举报[" + c.label + "]: " + r);
                            try {
                                Thread.sleep(1500);
                            } catch (Exception e) {
                            }
                        }
                        runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(MainActivity.this, "举报流程已执行完毕, 见悬浮窗日志", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }).start();
            }
        });
    }

    /** 扫描当前屏幕的评论(可选 AI 分类) */
    private void doScan(final boolean withAi) {
        final Config cfg = Config.load(this);
        tvGuardStat.setText("扫描中…");
        llRisk.removeAllViews();
        new Thread(new Runnable() {
            public void run() {
                try {
                    final java.util.List<CommentGuard.Comment> items;
                    if (cfg.guardScroll) {
                        items = CommentGuard.scanDeep(cfg.guardScrollRounds, 200);
                    } else {
                        items = CommentGuard.scan();
                    }
                    if (withAi && !items.isEmpty()) {
                        if (cfg.glmKey == null || cfg.glmKey.trim().isEmpty()) {
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    tvGuardStat.setText("未填 API Key, 仅扫描");
                                }
                            });
                        } else {
                            try {
                                CommentGuard.classify(items, cfg.glmKey, cfg.aiModel);
                            } catch (final Exception e) {
                                Logx.i("AI 分类失败: " + e.getMessage());
                            }
                        }
                    }
                    riskItems.clear();
                    riskItems.addAll(items);
                    runOnUiThread(new Runnable() {
                        public void run() {
                            renderRisk(items, withAi);
                        }
                    });
                } catch (Throwable t) {
                    Logx.e("扫描失败", t);
                    runOnUiThread(new Runnable() {
                        public void run() {
                            tvGuardStat.setText("扫描失败: " + t.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    /** 渲染风险列表 */
    private void renderRisk(final java.util.List<CommentGuard.Comment> items, boolean withAi) {
        llRisk.removeAllViews();
        int high = 0, mid = 0, leads = 0;
        for (final CommentGuard.Comment c : items) {
            if ("high".equals(c.level)) high++;
            else if ("mid".equals(c.level)) mid++;
            if (CommentGuard.hasIntent(c) && !"high".equals(c.level)) leads++;
        }
        tvGuardStat.setText("共 " + items.size() + " 条候选评论"
                + (withAi ? "，高风险 " + high + " 条 / 中风险 " + mid + " 条 / 意向 " + leads + " 条" : "（未做 AI 识别）")
                + (high + mid > 0 ? "　风险处理 QQ: " + Config.load(this).riskQQ : ""));

        for (final CommentGuard.Comment c : items) {
            final TextView tv = new TextView(this);
            int color = 0xFF333333;
            if ("high".equals(c.level)) color = 0xFFCC2222;
            else if ("mid".equals(c.level)) color = 0xFFB36B00;
            else if (CommentGuard.hasIntent(c)) color = 0xFF1F7A3D;
            else if ("normal".equals(c.label)) color = 0xFF555555;
            tv.setTextColor(color);
            tv.setTextSize(12);
            StringBuilder sb = new StringBuilder();
            sb.append(c.label.isEmpty() ? "未分类" : c.label);
            if (!c.level.isEmpty()) sb.append("[").append(c.level).append("]");
            if (!c.intent.isEmpty()) sb.append(" 意向:").append(c.intent);
            sb.append("\n").append(c.text);
            if (!c.reason.isEmpty()) sb.append("\n理由: ").append(c.reason);
            if (!c.reply.isEmpty()) sb.append("\n建议话术: ").append(c.reply);
            tv.setText(sb.toString());
            tv.setPadding(0, 8, 0, 0);
            llRisk.addView(tv);

            final LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            Button bLocate = new Button(this);
            bLocate.setText("定位");
            bLocate.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    android.view.accessibility.AccessibilityNodeInfo n = NodeUtil.get(c.nid);
                    if (n == null) {
                        Toast.makeText(MainActivity.this, "节点已失效, 请重新扫描", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    android.graphics.Rect r = new android.graphics.Rect();
                    n.getBoundsInScreen(r);
                    AccessService.tap(r.centerX(), r.centerY());
                }
            });
            Button bReport = new Button(this);
            bReport.setText("举报");
            bReport.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    new Thread(new Runnable() {
                        public void run() {
                            final String r = CommentGuard.report(c);
                            Logx.i("举报结果: " + r);
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    Toast.makeText(MainActivity.this, r, Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }).start();
                }
            });
            row.addView(bLocate, new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            row.addView(bReport, new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            if (!c.reply.isEmpty()) {
                Button bCopy = new Button(this);
                bCopy.setText("复制话术");
                bCopy.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        try {
                            android.content.ClipboardManager cm =
                                    (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                            cm.setPrimaryClip(android.content.ClipData.newPlainText("reply", c.reply));
                            Toast.makeText(MainActivity.this, "已复制, 粘贴到评论区/私信自行发送", Toast.LENGTH_SHORT).show();
                        } catch (Throwable e) {
                            Toast.makeText(MainActivity.this, "复制失败", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                row.addView(bCopy, new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            }
            llRisk.addView(row);
        }
        btnReportAll.setVisibility(high > 0 ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Logx.i("onActivityResult req=" + requestCode + " code=" + resultCode + " data=" + (data != null));
        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            Intent svc = new Intent(this, FloatService.class);
            svc.putExtra("capCode", resultCode);
            svc.putExtra("capData", data);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(svc);
            else startService(svc);
            Toast.makeText(this, "截图已授权（悬浮窗服务中生效）", Toast.LENGTH_LONG).show();
        } else if (requestCode == 1001) {
            Logx.i("截图授权被取消");
            Toast.makeText(this, "未授予截图权限, AI 无法看屏", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshPermState();
        // 配置可能被广播/脚本改过, 任务框以配置为准
        try {
            Config c = Config.load(this);
            if (c.aiTask != null && !c.aiTask.equals(etAiTask.getText().toString())) {
                etAiTask.setText(c.aiTask);
            }
        } catch (Exception e) {
        }
    }

    private boolean isAccessibilityOn() {
        if (AccessService.I != null) return true;
        try {
            String s = Settings.Secure.getString(getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            return s != null && s.contains(getPackageName() + "/" + AccessService.class.getName());
        } catch (Exception e) {
            return false;
        }
    }

    private boolean storageOn() {
        if (Build.VERSION.SDK_INT >= 30) {
            return android.os.Environment.isExternalStorageManager();
        }
        if (Build.VERSION.SDK_INT >= 23) {
            return checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void openStorage() {
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:" + getPackageName())));
            } else if (Build.VERSION.SDK_INT >= 23) {
                requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2002);
            }
        } catch (Exception e) {
            try {
                startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + getPackageName())));
            } catch (Exception e2) {
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        refreshPermState();
    }

    private void refreshPermState() {
        StringBuilder sb = new StringBuilder("权限状态: ");
        sb.append(Build.VERSION.SDK_INT >= 23 && Settings.canDrawOverlays(this) ? "悬浮窗✓ " : "悬浮窗✗ ");
        sb.append(isAccessibilityOn() ? "无障碍✓ " : "无障碍✗ ");
        sb.append(LocalImeService.isEnabled(this) ? (LocalImeService.isSelected(this) ? "输入法✓ " : "输入法已启用未选中 ") : "输入法✗ ");
        sb.append(storageOn() ? "存储✓ " : "存储✗ ");
        if (Build.VERSION.SDK_INT >= 23) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            sb.append(pm.isIgnoringBatteryOptimizations(getPackageName()) ? "电池✓" : "电池✗");
        }
        tvPermState.setText(sb.toString());
    }

    private void loadToViews() {
        Config cfg = Config.load(this);
        etName.setText(cfg.name);
        etInterval.setText(String.valueOf(cfg.interval));
        etStay1.setText(String.valueOf(cfg.stay1));
        etStay2.setText(String.valueOf(cfg.stay2));
        etPmSearch.setText(String.valueOf(cfg.pmSearch));
        etPmUid.setText(String.valueOf(cfg.pmUid));
        etVideos.setText(String.valueOf(cfg.videos));
        for (int i = 0; i < 6; i++) etC[i].setText(cfg.contacts[i]);
        cbFenduan.setChecked(cfg.fenduan);
        cbFuhao.setChecked(cfg.fuhao);
        cbDanci.setChecked(cfg.danci);
        cbAi.setChecked(cfg.ai);
        etScript.setText(cfg.script);
        etGlmKey.setText(cfg.glmKey);
        etAiTask.setText(cfg.aiTask);
        etAiModel.setText(cfg.aiModel);
        etAiSteps.setText(String.valueOf(cfg.aiMaxSteps));
        etRiskQQ.setText(cfg.riskQQ);
        cbAutoDm.setChecked(cfg.autoDmReply);
        etAutoDmText.setText(cfg.autoDmText);
        etAutoDmApps.setText(cfg.autoDmApps);
        etAutoDmMax.setText(String.valueOf(cfg.autoDmMaxPerHour));
        cbGuardScroll.setChecked(cfg.guardScroll);
        etGuardRounds.setText(String.valueOf(cfg.guardScrollRounds));
        if (cfg.mode == 1) rbUid.setChecked(true); else rbSearch.setChecked(true);
    }

    private int num(EditText e, int def) {
        try {
            return Integer.parseInt(e.getText().toString().trim());
        } catch (Exception ex) {
            return def;
        }
    }

    private void saveFromViews() {
        SharedPreferences.Editor ed = getSharedPreferences("cfg", MODE_PRIVATE).edit();
        ed.putString("name", etName.getText().toString());
        ed.putInt("mode", rbUid.isChecked() ? 1 : 0);
        ed.putInt("interval", num(etInterval, 1000));
        ed.putInt("stay1", num(etStay1, 3));
        ed.putInt("stay2", num(etStay2, 5));
        ed.putInt("pmSearch", num(etPmSearch, 20));
        ed.putInt("pmUid", num(etPmUid, 50));
        ed.putInt("videos", num(etVideos, 10));
        ed.putBoolean("fenduan", cbFenduan.isChecked());
        ed.putBoolean("fuhao", cbFuhao.isChecked());
        ed.putBoolean("danci", cbDanci.isChecked());
        for (int i = 0; i < 6; i++) ed.putString("c" + (i + 1), etC[i].getText().toString());
        ed.putBoolean("ai", cbAi.isChecked());
        ed.putString("script", etScript.getText().toString());
        ed.putString("glmKey", etGlmKey.getText().toString().trim());
        ed.putString("aiTask", etAiTask.getText().toString().trim());
        ed.putString("aiModel", etAiModel.getText().toString().trim());
        ed.putInt("aiMaxSteps", num(etAiSteps, 15));
        ed.putString("riskQQ", etRiskQQ.getText().toString().trim());
        ed.putBoolean("autoDmReply", cbAutoDm.isChecked());
        ed.putString("autoDmText", etAutoDmText.getText().toString());
        ed.putString("autoDmApps", etAutoDmApps.getText().toString().trim());
        ed.putInt("autoDmMaxPerHour", num(etAutoDmMax, 10));
        ed.putBoolean("guardScroll", cbGuardScroll.isChecked());
        ed.putInt("guardScrollRounds", num(etGuardRounds, 6));
        ed.apply();
    }
}
