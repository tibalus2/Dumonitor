package com.example.monitoring_automate_simple;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.material.button.MaterialButton;

import androidx.appcompat.app.AppCompatActivity;

import com.example.monitoring_automate_simple.api.ApiConfig;
import com.example.monitoring_automate_simple.api.ConfigManager;
import com.example.monitoring_automate_simple.api.DistechApiService;
import com.example.monitoring_automate_simple.model.PointData;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private ConstraintLayout rootLayout;
    private MaterialButton btnNightMode;
    private View divider, dividerV;
    private TextView tvTitle;
    private TextView tvValue1, tvValue2, tvValue3;
    private TextView tvUnit1,  tvUnit2,  tvUnit3;
    private TextView tvLabel1, tvLabel2, tvLabel3;
    private TextView tvStatus, tvLastUpdate;

    private DistechApiService apiService;
    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private final Runnable pollRunnable = this::fetchData;

    // Secret admin : 5 taps rapides sur le titre
    private int tapCount = 0;
    private long lastTapTime = 0;

    // Night / Day mode
    private boolean isNightMode = true;
    private static final String PREFS_UI = "ui_prefs";
    private static final String KEY_NIGHT = "night_mode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ConfigManager.init(this);
        setContentView(R.layout.activity_main);

        bindViews();
        initLabels();
        setupSecretAdmin();

        isNightMode = getSharedPreferences(PREFS_UI, MODE_PRIVATE).getBoolean(KEY_NIGHT, true);
        applyMode();

        apiService = new DistechApiService();
        fetchData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initLabels();
        enableKioskMode();
    }

    private void enableKioskMode() {
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        if (dpm == null) return;
        ComponentName admin = new ComponentName(this, MonitoringDeviceAdminReceiver.class);
        if (dpm.isDeviceOwnerApp(getPackageName())) {
            dpm.setLockTaskPackages(admin, new String[]{ getPackageName() });
        }
        // Ne démarre le lock task que si pas déjà actif
        android.app.ActivityManager am = (android.app.ActivityManager) getSystemService(ACTIVITY_SERVICE);
        if (am != null && am.getLockTaskModeState() == android.app.ActivityManager.LOCK_TASK_MODE_NONE) {
            try { startLockTask(); } catch (Exception ignored) {}
        }
    }

    private void bindViews() {
        rootLayout   = findViewById(R.id.main);
        btnNightMode = findViewById(R.id.btnNightMode);
        divider      = findViewById(R.id.divider);
        dividerV     = findViewById(R.id.dividerV);
        tvTitle      = findViewById(R.id.tvTitle);
        tvValue1     = findViewById(R.id.tvValue1);
        tvValue2     = findViewById(R.id.tvValue2);
        tvValue3     = findViewById(R.id.tvValue3);
        tvUnit1      = findViewById(R.id.tvUnit1);
        tvUnit2      = findViewById(R.id.tvUnit2);
        tvUnit3      = findViewById(R.id.tvUnit3);
        tvLabel1     = findViewById(R.id.tvLabel1);
        tvLabel2     = findViewById(R.id.tvLabel2);
        tvLabel3     = findViewById(R.id.tvLabel3);
        tvStatus     = findViewById(R.id.tvStatus);
        tvLastUpdate = findViewById(R.id.tvLastUpdate);
    }

    private void initLabels() {
        tvTitle.setText(ConfigManager.get().getAppTitle());
        ApiConfig.PointConfig[] points = ConfigManager.get().getPoints();
        if (points.length >= 1) tvLabel1.setText(points[0].label);
        if (points.length >= 2) tvLabel2.setText(points[1].label);
        if (points.length >= 3) tvLabel3.setText(points[2].label);
    }

    private void applyMode() {
        if (isNightMode) {
            rootLayout.setBackgroundColor(0xFF121212);
            int white  = 0xFFFFFFFF;
            int label  = 0xFF666666;
            int last   = 0xFF333333;
            int divCol = 0xFF2A2A2A;
            btnNightMode.setText("☀");
            btnNightMode.setTextColor(0xFFAAAAAA);
            tvTitle.setTextColor(white);
            tvValue1.setTextColor(white);  tvValue2.setTextColor(white);  tvValue3.setTextColor(white);
            tvLabel1.setTextColor(label);  tvLabel2.setTextColor(label);  tvLabel3.setTextColor(label);
            tvLastUpdate.setTextColor(last);
            divider.setBackgroundColor(divCol);
            dividerV.setBackgroundColor(divCol);
        } else {
            rootLayout.setBackgroundColor(0xFFF2F2F2);
            int dark   = 0xFF1A1A1A;
            int label  = 0xFF999999;
            int last   = 0xFFBBBBBB;
            int divCol = 0xFFDDDDDD;
            btnNightMode.setText("🌙");
            btnNightMode.setTextColor(0xFF555555);
            tvTitle.setTextColor(dark);
            tvValue1.setTextColor(dark);   tvValue2.setTextColor(dark);   tvValue3.setTextColor(dark);
            tvLabel1.setTextColor(label);  tvLabel2.setTextColor(label);  tvLabel3.setTextColor(label);
            tvLastUpdate.setTextColor(last);
            divider.setBackgroundColor(divCol);
            dividerV.setBackgroundColor(divCol);
        }
        getSharedPreferences(PREFS_UI, MODE_PRIVATE).edit().putBoolean(KEY_NIGHT, isNightMode).apply();
    }

    private void setupSecretAdmin() {
        btnNightMode.setOnClickListener(v -> {
            isNightMode = !isNightMode;
            applyMode();
        });

        // Titre — 5 taps en moins de 3 secondes
        if (tvTitle == null) return;
        tvTitle.setOnClickListener(v -> {
            long now = System.currentTimeMillis();
            if (now - lastTapTime > 3000) tapCount = 0;
            lastTapTime = now;
            tapCount++;
            if (tapCount >= 5) {
                tapCount = 0;
                startActivity(new Intent(this, AdminActivity.class));
            }
        });
    }

    private void fetchData() {
        tvStatus.setText("● Actualisation...");
        tvStatus.setTextColor(0xFFFFAA00);

        apiService.fetchAllPoints(new DistechApiService.Callback() {
            @Override
            public void onSuccess(PointData[] points) {
                updatePoint(tvValue1, tvUnit1, points.length > 0 ? points[0] : null);
                updatePoint(tvValue2, tvUnit2, points.length > 1 ? points[1] : null);
                updatePoint(tvValue3, tvUnit3, points.length > 2 ? points[2] : null);

                tvStatus.setText("● Connecté  —  " + ConfigManager.get().getBaseUrl());
                tvStatus.setTextColor(0xFF4CAF50);

                String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
                tvLastUpdate.setText("Dernière mise à jour : " + time);

                schedulePoll();
            }

            @Override
            public void onError(String message) {
                tvStatus.setText("● Erreur : " + message);
                tvStatus.setTextColor(0xFFFF5252);
                schedulePoll();
            }
        });
    }

    private void updatePoint(TextView tvValue, TextView tvUnit, PointData point) {
        if (point == null) return;
        tvValue.setText(formatValue(point.getValue()));
        tvUnit.setText(point.getUnit());
    }

    private String formatValue(double v) {
        if (v == Math.floor(v) && !Double.isInfinite(v)) {
            return String.valueOf((int) v);
        }
        return String.format(Locale.getDefault(), "%.1f", v);
    }

    private void schedulePoll() {
        pollHandler.removeCallbacks(pollRunnable);
        pollHandler.postDelayed(pollRunnable, ApiConfig.POLL_INTERVAL_MS);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pollHandler.removeCallbacks(pollRunnable);
        if (apiService != null) apiService.shutdown();
    }
}