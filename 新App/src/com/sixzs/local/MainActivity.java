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
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private EditText etName, etInterval, etStay1, etStay2, etPmSearch, etPmUid, etVideos;
    private EditText[] etC = new EditText[6];
    private EditText etScript;
    private CheckBox cbFenduan, cbFuhao, cbDanci, cbAi;
    private RadioButton rbSearch, rbUid;
    private TextView tvPermState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            Intent svc = new Intent(this, FloatService.class);
            svc.putExtra("capCode", resultCode);
            svc.putExtra("capData", data);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(svc);
            else startService(svc);
            Toast.makeText(this, "截图已授权（悬浮窗服务中生效）", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshPermState();
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
        ed.apply();
    }
}
