package com.cobbleverse.legendaryrule.lead;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Pure immutable domain representation of the 18x18 directed type chart.
 * Zero external dependencies.
 */
public final class TypeChartData {
    private final Map<String, Map<String, Double>> table;

    public TypeChartData(Map<String, Map<String, Double>> table) {
        Objects.requireNonNull(table, "table must not be null");
        Map<String, Map<String, Double>> clean = new HashMap<>();
        for (Map.Entry<String, Map<String, Double>> e : table.entrySet()) {
            String att = e.getKey().toLowerCase(Locale.ROOT);
            Map<String, Double> inner = new HashMap<>();
            if (e.getValue() != null) {
                for (Map.Entry<String, Double> ie : e.getValue().entrySet()) {
                    inner.put(ie.getKey().toLowerCase(Locale.ROOT), ie.getValue());
                }
            }
            clean.put(att, Collections.unmodifiableMap(inner));
        }
        this.table = Collections.unmodifiableMap(clean);
    }

    public double getMultiplier(String attackType, String defenseType) {
        if (attackType == null || defenseType == null) {
            return 1.0;
        }
        String att = attackType.toLowerCase(Locale.ROOT);
        String def = defenseType.toLowerCase(Locale.ROOT);
        Map<String, Double> inner = table.get(att);
        if (inner == null) {
            return 1.0;
        }
        Double mult = inner.get(def);
        return mult != null ? mult : 1.0;
    }
}
