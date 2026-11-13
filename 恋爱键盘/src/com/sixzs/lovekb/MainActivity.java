package com.sixzs.lovekb;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends android.app.Activity {

    private EditText etKey, etModel, etStyle, etTypes, etCount, etApps;
    private CheckBox cbAutoSend, cbShotCrop;
    private EditText etVision, etMaxEdge, etQuality;
    private TextView tvShot;
    private TextView tvPerm, tvResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etKey = (EditText) findViewById(R.id.etKey);
        etModel = (EditText) findViewById(R.id.etModel);
        etStyle = (EditText) findViewById(R.id.etStyle);
        etTypes = (EditText) findViewById(R.id.etTypes);
        etCount = (EditText) findViewById(R.id.etCount);
        etApps = (EditText) findViewById(R.id.etApps);
        cbAutoSend = (CheckBox) findViewById(R.id.cbAutoSend);
        cbShotCrop = (CheckBox) findViewById(R.id.cbShotCrop);
        etVision = (EditText) findViewById(R.id.etVision);
        etMaxEdge = (EditText) findViewById(R.id.etMaxEdge);
        etQuality = (EditText) findViewById(R.id.etQuality);
        tvShot = (TextView) findViewById(R.id.tvShot);
        tvShot = (TextView) findViewById(R.id.tvShot);
        cbAutoSend = (CheckBox) findViewById(R.id.cbAutoSend);
        tvShot = (TextView) findViewById(R.id.tvShot);
        tvShot = (TextView) findViewById(R.id.tvShot);
        tvPerm = (TextView) findViewById(R.id.tvPerm);
        tvResult = (TextView) findViewById(R.id.tvResult);

        Config cfg = Config.load(this);
        etKey.setText(cfg.glmKey);
        etKey.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        etModel.setText(cfg.model);
        etStyle.setText(cfg.style);
        etTypes.setText(cfg.types);
        etCount.setText(String.valueOf(cfg.replyCount));
        etApps.setText(cfg.targetApps);
        cbAutoSend.setChecked(cfg.autoSend);
        cbShotCrop.setChecked(cfg.shotCrop);
        etVision.setText(cfg.visionModel);
        etMaxEdge.setText(String.valueOf(cfg.shotMaxEdge));
        etQuality.setText(String.valueOf(cfg.shotQuality));
        cbAutoSend.setChecked(cfg.autoSend);

        findViewById(R.id.btnSave).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                save();
                toast("已保存");
            }
        });
        findViewById(R.id.btnFloat).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                save();
                try {
                    Intent it = new Intent(MainActivity.this, FloatService.class);
                    startService(it);
                    toast("悬浮窗已启动");
                } catch (Throwable t) {
                    toast("启动失败: " + t.getMessage());
                }
            }
        });
        findViewById(R.id.btnCap).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                requestCapture();
            }
        });
        findViewById(R.id.btnTestShot).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                testShot();
            }
        });
        findViewById(R.id.btnSaveShot).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                saveShot();
            }
        });
        findViewById(R.id.btnTestShot).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                testShot();
            }
        });
        findViewById(R.id.btnSaveShot).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                saveShot();
            }
        });
        findViewById(R.id.btnIme).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // 打开系统输入法设置, 让用户把「恋爱键盘」设为默认输入法
                try {
                    Intent it = new Intent("android.settings.INPUT_METHOD_SETTINGS");
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(it);
                    toast("在「默认输入法」里选择「恋爱键盘」");
                } catch (Throwable t) {
                    toast("请到: 设置 → 系统 → 语言和输入法 → 默认输入法");
                }
            }
        });
        findViewById(R.id.btnImePick).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    android.view.inputmethod.InputMethodManager imm =
                            (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    if (imm != null) imm.showInputMethodPicker();
                } catch (Throwable t) {
                }
            }
        });
        findViewById(R.id.btnSendTest).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                toast("收到「发送」点击 —— 自动发送链路正常");
                Logx.i("测试按钮被点击");
            }
        });
        findViewById(R.id.btnSendTest).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                toast("收到「发送」点击 —— 自动发送链路正常");
                Logx.i("测试按钮被点击");
            }
        });
        findViewById(R.id.btnTest).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                save();
                testAnalyze();
            }
        });
    }



    /** 测试无障碍截图(Android 11+ 免授权) */
    private void testShot() {
        tvShot.setText("截图方式: " + ScreenCap.mode() + " → 测试中...");
        new Thread(new Runnable() {
            public void run() {
                final android.graphics.Bitmap bmp = ScreenCap.capture();
                final String info = ScreenCap.lastInfo;
                runOnUiThread(new Runnable() {
                    public void run() {
                        tvShot.setText("截图方式: " + ScreenCap.mode() + "\n结果: " + info
                                + (bmp != null ? " (尺寸 " + bmp.getWidth() + "x" + bmp.getHeight() + ")" : ""));
                    }
                });
            }
        }).start();
    }

    /** 保存截图到相册 */
    private void saveShot() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    android.graphics.Bitmap bmp = ScreenCap.capture();
                    if (bmp == null) {
                        final String info = ScreenCap.lastInfo;
                        runOnUiThread(new Runnable() {
                            public void run() {
                                tvShot.setText("保存失败: " + info);
                            }
                        });
                        return;
                    }
                    java.io.File dir = android.os.Environment.getExternalStoragePublicDirectory(
                            android.os.Environment.DIRECTORY_PICTURES);
                    java.io.File f = new java.io.File(dir, "lovekb_" + System.currentTimeMillis() + ".jpg");
                    java.io.FileOutputStream fo = new java.io.FileOutputStream(f);
                    bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, fo);
                    fo.close();
                    final String path = f.getAbsolutePath();
                    runOnUiThread(new Runnable() {
                        public void run() {
                            tvShot.setText("已保存: " + path);
                            toast("已保存到相册");
                        }
                    });
                } catch (final Throwable t) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            tvShot.setText("保存失败: " + t.getMessage());
                        }
                    });
                }
            }
        }).start();
    }



    /** 请求系统截图授权 */
    private void requestCapture() {
        try {
            android.media.projection.MediaProjectionManager mpm =
                    (android.media.projection.MediaProjectionManager)
                            getSystemService(MEDIA_PROJECTION_SERVICE);
            startActivityForResult(mpm.createScreenCaptureIntent(), 1001);
        } catch (Throwable t) {
            toast("授权失败: " + t.getMessage());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001) {
            // 授权票是一次性的: 当场转交给 CaptureService,
            // 由它在前台服务上下文里换出 MediaProjection 并建虚拟屏
            boolean ok = ScreenCap.init(this, resultCode, data);
            Logx.i(ok ? "授权票已交给后台录屏服务" : "截图授权失败");
            toast(ok ? "已授权, 后台录屏服务已启动" : "截图授权失败");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshPerm();
        try {
            if (getIntent() != null && getIntent().getBooleanExtra("capture", false)
                    && !ScreenCap.ready()) {
                requestCapture();
            }
        } catch (Throwable t) {
        }
    }

    /** 从输入框读整数, 空/非法则用默认值 */
    private int num(EditText et, int def) {
        try {
            String t = et.getText().toString().trim();
            if (t.isEmpty()) return def;
            return Integer.parseInt(t);
        } catch (Throwable e) {
            return def;
        }
    }

    private void save() {
        Config cfg = new Config();
        cfg.glmKey = etKey.getText().toString().trim();
        cfg.model = etModel.getText().toString().trim();
        cfg.style = etStyle.getText().toString().trim();
        cfg.types = etTypes.getText().toString().trim();
        try {
            cfg.replyCount = Integer.parseInt(etCount.getText().toString().trim());
        } catch (Exception e) {
            cfg.replyCount = 3;
        }
        cfg.targetApps = etApps.getText().toString().trim();
        cfg.autoSend = cbAutoSend.isChecked();
        cfg.shotCrop = cbShotCrop.isChecked();
        String vm = etVision.getText().toString().trim();
        cfg.visionModel = vm.isEmpty() ? "glm-4.6v" : vm;
        cfg.shotMaxEdge = num(etMaxEdge, 1280);
        cfg.shotQuality = num(etQuality, 70);
        cfg.save(this);
    }

    /** 权限体检 */
    private void refreshPerm() {
        StringBuilder sb = new StringBuilder("权限状态: ");
        sb.append("悬浮窗").append(overlayAllowed() ? "✓" : "✗(需在设置里开启)");
        boolean acc = AccessService.I != null;
        if (!acc) {
            // 服务已注册但还没连上, 也算开启中
            try {
                String en = android.provider.Settings.Secure.getString(getContentResolver(),
                        "enabled_accessibility_services");
                acc = en != null && en.contains("com.sixzs.lovekb");
            } catch (Throwable t) {
            }
        }
        sb.append("  无障碍").append(acc ? "✓" : "✗(需在设置里开启)");
        try {
            String def = android.provider.Settings.Secure.getString(getContentResolver(),
                    "default_input_method");
            sb.append("  默认输入法").append(def != null && def.contains("lovekb") ? "✓" : "✗(读剪贴板需要)");
        } catch (Throwable t) {
        }
        tvPerm.setText(sb.toString());
    }

    private boolean overlayAllowed() {
        try {
            return android.provider.Settings.canDrawOverlays(this);
        } catch (Throwable t) {
            return false;
        }
    }

    /** 用剪贴板里的内容测试一次分类 */
    private void testAnalyze() {
        final String msg = Clip.read(this);
        if (msg.isEmpty()) {
            tvResult.setText("剪贴板是空的, 先复制一条消息");
            return;
        }
        final Config cfg = Config.load(this);
        if (cfg.glmKey.trim().isEmpty()) {
            tvResult.setText("请先填 API Key");
            return;
        }
        tvResult.setText("分析中...");
        new Thread(new Runnable() {
            public void run() {
                try {
                    final LoveAI.Result r = LoveAI.analyze(cfg, msg);
                    runOnUiThread(new Runnable() {
                        public void run() {
                            StringBuilder sb = new StringBuilder();
                            sb.append("【").append(r.type).append("】").append(r.intent).append("\n");
                            for (String s : r.replies) sb.append("· ").append(s).append("\n");
                            tvResult.setText(sb.toString());
                        }
                    });
                } catch (final Throwable t) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            tvResult.setText("失败: " + t.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
