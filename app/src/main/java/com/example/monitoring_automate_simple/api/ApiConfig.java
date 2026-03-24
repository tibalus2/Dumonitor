package com.example.monitoring_automate_simple.api;

/**
 * Configuration pour l'automate Distech Controls.
 *
 * Endpoint GET par point :
 *   GET /api/rest/v1/protocols/bacnet/local/objects/{type}/{instance}/properties/{property}
 *
 * Types courants (kebab-case) :
 *   analog-input, analog-output, analog-value
 *   binary-input, binary-output, binary-value
 *   multi-state-input, multi-state-output, multi-state-value
 */
public class ApiConfig {

    // ── Connexion ────────────────────────────────────────────────────────────
    public static final String BASE_URL  = "https://192.168.1.12";
    public static final String OBJECTS   = "/api/rest/v1/protocols/bacnet/local/objects";
    public static final String USERNAME  = "admin";
    public static final String PASSWORD  = "Dumortier02*";

    // ── Intervalle de polling (ms) ───────────────────────────────────────────
    public static final int POLL_INTERVAL_MS = 10_000;

    // ── Définition des points à afficher ────────────────────────────────────
    // type     : kebab-case  (ex: "analog-input", "analog-value")
    // instance : numéro d'instance BACnet
    // property : kebab-case  (ex: "present-value")
    public static final PointConfig[] POINTS = {
        new PointConfig(
            "Température retour",   // label affiché
            "analog-value", 6,    // type + instance
            "present-value",        // propriété BACnet
            "°C",                   // unité
            0.0, 100.0               // min / max jauge
        ),
        new PointConfig(
            "Humidité relative",
            "analog-value", 6,
            "present-value",
            "%",
            0.0, 100.0
        ),
        new PointConfig(
            "Pression différentielle",
            "analog-value", 6,
            "present-value",
            "Pa",
            0.0, 500.0
        )
    };

    // ── Modèle ───────────────────────────────────────────────────────────────
    public static class PointConfig {
        public final String label;
        public final String type;
        public final int    instance;
        public final String property;
        public final String unit;
        public final double min;
        public final double max;

        public PointConfig(String label, String type, int instance,
                           String property, String unit, double min, double max) {
            this.label    = label;
            this.type     = type;
            this.instance = instance;
            this.property = property;
            this.unit     = unit;
            this.min      = min;
            this.max      = max;
        }

        /** URL complète du point : /objects/{type}/{instance}/properties/{property} */
        public String path() {
            return OBJECTS + "/" + type + "/" + instance + "/properties/" + property;
        }
    }
}
