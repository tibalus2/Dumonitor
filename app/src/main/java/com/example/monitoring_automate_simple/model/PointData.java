package com.example.monitoring_automate_simple.model;

public class PointData {
    private final String label;
    private final double value;
    private final String unit;
    private final double minValue;
    private final double maxValue;

    public PointData(String label, double value, String unit, double minValue, double maxValue) {
        this.label = label;
        this.value = value;
        this.unit = unit;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    public String getLabel() { return label; }
    public double getValue() { return value; }
    public String getUnit() { return unit; }
    public double getMinValue() { return minValue; }
    public double getMaxValue() { return maxValue; }

    /** Returns a percentage (0–100) of value within [minValue, maxValue]. */
    public int getPercent() {
        if (maxValue <= minValue) return 0;
        double pct = (value - minValue) / (maxValue - minValue) * 100.0;
        return (int) Math.min(100, Math.max(0, pct));
    }
}
