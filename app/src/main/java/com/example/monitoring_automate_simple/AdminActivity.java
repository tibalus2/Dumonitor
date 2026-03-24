package com.example.monitoring_automate_simple;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.monitoring_automate_simple.api.ApiConfig;
import com.example.monitoring_automate_simple.api.ConfigManager;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AdminActivity extends AppCompatActivity {

    private static final Map<String, String> BACNET_UNITS = new HashMap<>();
    static {
        BACNET_UNITS.put("degreesCelsius",           "°C");
        BACNET_UNITS.put("degreesFahrenheit",         "°F");
        BACNET_UNITS.put("degreesKelvin",             "K");
        BACNET_UNITS.put("percentRelativeHumidity",   "%");
        BACNET_UNITS.put("percent",                   "%");
        BACNET_UNITS.put("pascals",                   "Pa");
        BACNET_UNITS.put("kilopascals",               "kPa");
        BACNET_UNITS.put("bars",                      "bar");
        BACNET_UNITS.put("millibars",                 "mbar");
        BACNET_UNITS.put("cubicMetersPerHour",        "m³/h");
        BACNET_UNITS.put("litersPerSecond",           "L/s");
        BACNET_UNITS.put("wattsPerSquareMeter",       "W/m²");
        BACNET_UNITS.put("watts",                     "W");
        BACNET_UNITS.put("kilowatts",                 "kW");
        BACNET_UNITS.put("amperes",                   "A");
        BACNET_UNITS.put("volts",                     "V");
        BACNET_UNITS.put("noUnits",                   "");
    }

    // Connection fields
    private TextInputEditText etAppTitle;
    private TextInputEditText etBaseUrl, etUsername, etPassword;
    private TextView tvConnectionStatus;

    private static final String FIXED_TYPE     = "analog-value";
    private static final String FIXED_PROPERTY = "present-value";

    // Gauge fields (3 sets)
    private final TextInputEditText[] etLabel    = new TextInputEditText[3];
    private final TextInputEditText[] etInstance = new TextInputEditText[3];
    private final TextInputEditText[] etUnit     = new TextInputEditText[3];
    private final TextInputEditText[] etMin      = new TextInputEditText[3];
    private final TextInputEditText[] etMax      = new TextInputEditText[3];

    private OkHttpClient httpClient;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        httpClient = buildUnsafeClient();
        bindViews();
        loadCurrentConfig();
    }

    private void bindViews() {
        etAppTitle = findViewById(R.id.etAppTitle);
        etBaseUrl  = findViewById(R.id.etBaseUrl);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus);

        int[] gaugeViewIds = { R.id.gauge1Fields, R.id.gauge2Fields, R.id.gauge3Fields };
        for (int i = 0; i < 3; i++) {
            View g = findViewById(gaugeViewIds[i]);
            etLabel[i]    = g.findViewById(R.id.etLabel);
            etInstance[i] = g.findViewById(R.id.etInstance);
            etUnit[i]     = g.findViewById(R.id.etUnit);
            etMin[i]      = g.findViewById(R.id.etMin);
            etMax[i]      = g.findViewById(R.id.etMax);

            final int idx = i;
            g.<Button>findViewById(R.id.btnFetchUnit).setOnClickListener(v -> fetchUnit(idx));
            g.<Button>findViewById(R.id.btnDiscover).setOnClickListener(v -> discoverObjects(idx));
        }

        findViewById(R.id.btnTestConnection).setOnClickListener(v -> testConnection());
        findViewById(R.id.btnSave).setOnClickListener(v -> saveConfig());
    }

    private void loadCurrentConfig() {
        ConfigManager cfg = ConfigManager.get();
        etAppTitle.setText(cfg.getAppTitle());
        etBaseUrl.setText(cfg.getBaseUrl());
        etUsername.setText(cfg.getUsername());
        etPassword.setText(cfg.getPassword());

        ApiConfig.PointConfig[] points = cfg.getPoints();
        for (int i = 0; i < 3 && i < points.length; i++) {
            ApiConfig.PointConfig p = points[i];
            etLabel[i].setText(p.label);
            etInstance[i].setText(String.valueOf(p.instance));
            etUnit[i].setText(p.unit);
            etMin[i].setText(String.valueOf((int) p.min == p.min ? String.valueOf((int) p.min) : String.valueOf(p.min)));
            etMax[i].setText(String.valueOf((int) p.max == p.max ? String.valueOf((int) p.max) : String.valueOf(p.max)));
        }
    }

    // ── Test connexion ─────────────────────────────────────────────────────────

    private void testConnection() {
        String url  = str(etBaseUrl);
        String user = str(etUsername);
        String pass = str(etPassword);
        if (url.isEmpty()) { toast("URL vide"); return; }

        tvConnectionStatus.setVisibility(View.VISIBLE);
        tvConnectionStatus.setTextColor(0xFFFFAA00);
        tvConnectionStatus.setText("● Test en cours...");

        String auth = "Basic " + Base64.encodeToString((user + ":" + pass).getBytes(), Base64.NO_WRAP);
        executor.execute(() -> {
            try {
                Request req = new Request.Builder()
                        .url(url + ApiConfig.OBJECTS)
                        .addHeader("Authorization", auth)
                        .addHeader("Accept", "application/json")
                        .get().build();
                try (Response resp = httpClient.newCall(req).execute()) {
                    String body = resp.body() != null ? resp.body().string() : "";
                    boolean ok = resp.isSuccessful();
                    mainHandler.post(() -> {
                        tvConnectionStatus.setTextColor(ok ? 0xFF4CAF50 : 0xFFFF5252);
                        tvConnectionStatus.setText(ok
                                ? "● Connecté — HTTP " + resp.code()
                                : "● Erreur HTTP " + resp.code() + " — " + body.substring(0, Math.min(80, body.length())));
                    });
                }
            } catch (Exception e) {
                mainHandler.post(() -> {
                    tvConnectionStatus.setTextColor(0xFFFF5252);
                    tvConnectionStatus.setText("● " + e.getMessage());
                });
            }
        });
    }

    // ── Discover objects ───────────────────────────────────────────────────────

    private void discoverObjects(int gaugeIndex) {
        String url  = str(etBaseUrl);
        String user = str(etUsername);
        String pass = str(etPassword);
        if (url.isEmpty()) { toast("Remplis l'URL d'abord"); return; }

        toast("Recherche en cours...");
        String auth = "Basic " + Base64.encodeToString((user + ":" + pass).getBytes(), Base64.NO_WRAP);

        executor.execute(() -> {
            try {
                Request req = new Request.Builder()
                        .url(url + ApiConfig.OBJECTS)
                        .addHeader("Authorization", auth)
                        .addHeader("Accept", "application/json")
                        .get().build();
                try (Response resp = httpClient.newCall(req).execute()) {
                    if (!resp.isSuccessful()) throw new IOException("HTTP " + resp.code());
                    String body = resp.body() != null ? resp.body().string() : "[]";
                    List<String> items = parseObjectList(body);
                    mainHandler.post(() -> showObjectDialog(gaugeIndex, items));
                }
            } catch (Exception e) {
                mainHandler.post(() -> toast("Erreur : " + e.getMessage()));
            }
        });
    }

    private List<String> parseObjectList(String json) {
        List<String> list = new ArrayList<>();
        try {
            JSONArray arr;
            try {
                arr = new JSONArray(json);
            } catch (Exception e) {
                JSONObject obj = new JSONObject(json);
                arr = obj.has("value") ? obj.getJSONArray("value") : new JSONArray();
            }
            for (int i = 0; i < arr.length(); i++) {
                JSONObject item = arr.getJSONObject(i);
                String type = item.optString("type", item.optString("objectType", ""));
                int instance = item.optInt("instance", item.optInt("objectInstance", -1));
                if (!type.isEmpty() && instance >= 0) {
                    list.add(type + " / " + instance);
                }
            }
        } catch (Exception ignored) {}
        if (list.isEmpty()) list.add("Aucun objet trouvé");
        return list;
    }

    private void showObjectDialog(int gaugeIndex, List<String> items) {
        String[] arr = items.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle("Objets disponibles")
                .setItems(arr, (dialog, which) -> {
                    String selected = arr[which];
                    if (selected.contains(" / ")) {
                        String[] parts = selected.split(" / ");
                        etInstance[gaugeIndex].setText(parts[1].trim());
                    }
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    // ── Fetch unit ─────────────────────────────────────────────────────────────

    private void fetchUnit(int gaugeIndex) {
        String url      = str(etBaseUrl);
        String user     = str(etUsername);
        String pass     = str(etPassword);
        String instance = str(etInstance[gaugeIndex]);
        if (url.isEmpty() || instance.isEmpty()) {
            toast("Remplis URL, type et instance d'abord");
            return;
        }

        String unitUrl = url + ApiConfig.OBJECTS + "/" + FIXED_TYPE + "/" + instance + "/properties/units";
        String auth = "Basic " + Base64.encodeToString((user + ":" + pass).getBytes(), Base64.NO_WRAP);

        executor.execute(() -> {
            try {
                Request req = new Request.Builder()
                        .url(unitUrl)
                        .addHeader("Authorization", auth)
                        .addHeader("Accept", "application/json")
                        .get().build();
                try (Response resp = httpClient.newCall(req).execute()) {
                    if (!resp.isSuccessful()) throw new IOException("HTTP " + resp.code());
                    String body = resp.body() != null ? resp.body().string() : "{}";
                    String unit = parseUnitValue(body);
                    mainHandler.post(() -> {
                        etUnit[gaugeIndex].setText(unit);
                        toast("Unité récupérée : " + (unit.isEmpty() ? "(sans unité)" : unit));
                    });
                }
            } catch (Exception e) {
                mainHandler.post(() -> toast("Erreur unité : " + e.getMessage()));
            }
        });
    }

    private String parseUnitValue(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            String raw = obj.optString("value", "");
            return BACNET_UNITS.getOrDefault(raw, raw);
        } catch (Exception e) {
            return "";
        }
    }

    // ── Save ───────────────────────────────────────────────────────────────────

    private void saveConfig() {
        ConfigManager cfg = ConfigManager.get();

        cfg.saveAppTitle(str(etAppTitle).isEmpty() ? "Monitoring Automate" : str(etAppTitle));
        cfg.saveConnection(str(etBaseUrl), str(etUsername), str(etPassword));

        for (int i = 0; i < 3; i++) {
            String instanceStr = str(etInstance[i]);
            int instance = instanceStr.isEmpty() ? 0 : Integer.parseInt(instanceStr);
            float min = parseFloat(str(etMin[i]), 0f);
            float max = parseFloat(str(etMax[i]), 100f);

            cfg.savePoint(i,
                    str(etLabel[i]),
                    FIXED_TYPE,
                    instance,
                    FIXED_PROPERTY,
                    str(etUnit[i]),
                    min, max);
        }

        Toast.makeText(this, "Configuration sauvegardée", Toast.LENGTH_SHORT).show();
        finish();
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private String str(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    private float parseFloat(String s, float def) {
        try { return Float.parseFloat(s); } catch (Exception e) { return def; }
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private OkHttpClient buildUnsafeClient() {
        try {
            X509TrustManager trustAll = new X509TrustManager() {
                @Override public void checkClientTrusted(X509Certificate[] c, String a) {}
                @Override public void checkServerTrusted(X509Certificate[] c, String a) {}
                @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            };
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, new TrustManager[]{trustAll}, new java.security.SecureRandom());
            return new OkHttpClient.Builder()
                    .sslSocketFactory(sc.getSocketFactory(), trustAll)
                    .hostnameVerifier((h, s) -> true)
                    .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                    .build();
        } catch (Exception e) {
            return new OkHttpClient();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
