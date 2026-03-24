package com.example.monitoring_automate_simple.api;

import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import com.example.monitoring_automate_simple.model.PointData;

import org.json.JSONObject;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Service de communication avec l'automate Distech Controls.
 *
 * L'API REST Distech retourne un JSON de la forme :
 *   { "value": 22.5 }
 * ou
 *   { "present-value": 22.5 }
 *
 * Adaptez parseValue() si votre firmware retourne un format différent.
 */
public class DistechApiService {

    private static final String TAG = "DistechApiService";

    public interface Callback {
        void onSuccess(PointData[] points);
        void onError(String message);
    }

    private final OkHttpClient httpClient;
    private final ExecutorService executor;
    private final Handler mainHandler;
    private final String authHeader;

    public DistechApiService() {
        httpClient  = buildUnsafeOkHttpClient();
        executor    = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        // authHeader est reconstruit dynamiquement dans fetchSinglePoint
        authHeader  = null;
    }

    private String buildAuthHeader() {
        com.example.monitoring_automate_simple.api.ConfigManager cfg =
                com.example.monitoring_automate_simple.api.ConfigManager.get();
        String credentials = cfg.getUsername() + ":" + cfg.getPassword();
        return "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
    }

    /**
     * Construit un OkHttpClient qui accepte les certificats auto-signés.
     * Nécessaire pour les automates Distech Controls sur réseau interne.
     */
    private OkHttpClient buildUnsafeOkHttpClient() {
        try {
            X509TrustManager trustAll = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                @Override
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{trustAll}, new java.security.SecureRandom());
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            return new OkHttpClient.Builder()
                    .sslSocketFactory(sslSocketFactory, trustAll)
                    .hostnameVerifier((hostname, session) -> true)
                    .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                    .build();

        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            Log.e(TAG, "SSL setup failed, falling back to default client", e);
            return new OkHttpClient.Builder()
                    .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                    .build();
        }
    }

    /**
     * Envoie un GET par point configuré, puis délivre les résultats sur le main thread.
     * URL : GET /api/rest/v1/protocols/bacnet/local/objects/{type}/{instance}/properties/{property}
     */
    public void fetchAllPoints(Callback callback) {
        executor.execute(() -> {
            try {
                ApiConfig.PointConfig[] points = com.example.monitoring_automate_simple.api.ConfigManager.get().getPoints();
                PointData[] results = new PointData[points.length];
                for (int i = 0; i < points.length; i++) {
                    ApiConfig.PointConfig cfg = points[i];
                    double value = fetchSinglePoint(cfg);
                    results[i] = new PointData(cfg.label, value, cfg.unit, cfg.min, cfg.max);
                }
                mainHandler.post(() -> callback.onSuccess(results));

            } catch (Exception e) {
                Log.e(TAG, "fetchAllPoints error: " + e.getMessage(), e);
                final String msg = e.getMessage();
                mainHandler.post(() -> callback.onError(msg));
            }
        });
    }

    private double fetchSinglePoint(ApiConfig.PointConfig cfg) throws IOException {
        String baseUrl = com.example.monitoring_automate_simple.api.ConfigManager.get().getBaseUrl();
        Request request = new Request.Builder()
                .url(baseUrl + cfg.path())
                .addHeader("Authorization", buildAuthHeader())
                .addHeader("Accept", "application/json")
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code() + " [" + cfg.label + "] — " + body);
            }
            Log.d(TAG, cfg.label + " → " + body);
            return parseValue(body);
        }
    }

    /**
     * Parse la réponse GET d'un point unique.
     * Formats supportés :
     *   { "value": 22.5 }
     *   { "present-value": 22.5 }
     *   { "value": "22.5" }
     */
    private double parseValue(String json) throws IOException {
        try {
            JSONObject obj = new JSONObject(json);
            for (String key : new String[]{"value", "present-value", "presentValue"}) {
                if (obj.has(key)) {
                    Object v = obj.get(key);
                    if (v instanceof Number) return ((Number) v).doubleValue();
                    if (v instanceof String) return Double.parseDouble((String) v);
                }
            }
            Log.w(TAG, "parseValue: aucune clé valeur trouvée dans " + json);
        } catch (Exception e) {
            throw new IOException("Erreur parsing valeur: " + e.getMessage() + " — body: " + json);
        }
        return 0;
    }

    public void shutdown() {
        executor.shutdown();
    }
}
