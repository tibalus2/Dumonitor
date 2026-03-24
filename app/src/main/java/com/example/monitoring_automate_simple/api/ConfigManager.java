package com.example.monitoring_automate_simple.api;

import android.content.Context;
import android.content.SharedPreferences;

public class ConfigManager {

    private static final String PREFS = "distech_config";
    private static ConfigManager instance;
    private final SharedPreferences prefs;

    private ConfigManager(Context ctx) {
        prefs = ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static void init(Context ctx) {
        if (instance == null) instance = new ConfigManager(ctx);
    }

    public static ConfigManager get() { return instance; }

    // ── Connexion ─────────────────────────────────────────────────────────────

    public String getBaseUrl()  { return prefs.getString("base_url",  ApiConfig.BASE_URL);       }
    public String getUsername() { return prefs.getString("username",  ApiConfig.USERNAME);        }
    public String getPassword() { return prefs.getString("password",  ApiConfig.PASSWORD);        }
    public String getAppTitle() { return prefs.getString("app_title", "Monitoring Automate");     }

    public void saveAppTitle(String title) { prefs.edit().putString("app_title", title).apply(); }

    public void saveConnection(String url, String user, String pass) {
        prefs.edit()
                .putString("base_url", url)
                .putString("username", user)
                .putString("password", pass)
                .apply();
    }

    // ── Points ────────────────────────────────────────────────────────────────

    public ApiConfig.PointConfig[] getPoints() {
        ApiConfig.PointConfig[] defaults = ApiConfig.POINTS;
        ApiConfig.PointConfig[] result   = new ApiConfig.PointConfig[defaults.length];
        for (int i = 0; i < defaults.length; i++) {
            ApiConfig.PointConfig d = defaults[i];
            result[i] = new ApiConfig.PointConfig(
                    prefs.getString("p" + i + "_label",    d.label),
                    prefs.getString("p" + i + "_type",     d.type),
                    prefs.getInt   ("p" + i + "_instance", d.instance),
                    prefs.getString("p" + i + "_property", d.property),
                    prefs.getString("p" + i + "_unit",     d.unit),
                    prefs.getFloat ("p" + i + "_min",      (float) d.min),
                    prefs.getFloat ("p" + i + "_max",      (float) d.max)
            );
        }
        return result;
    }

    public void savePoint(int i, String label, String type, int instance,
                          String property, String unit, float min, float max) {
        prefs.edit()
                .putString("p" + i + "_label",    label)
                .putString("p" + i + "_type",     type)
                .putInt   ("p" + i + "_instance", instance)
                .putString("p" + i + "_property", property)
                .putString("p" + i + "_unit",     unit)
                .putFloat ("p" + i + "_min",      min)
                .putFloat ("p" + i + "_max",      max)
                .apply();
    }
}
