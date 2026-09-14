package com.cobbleverse.legendaryrule.mechanic;

import org.jetbrains.annotations.Nullable;

public enum MajorBattleMechanic {
    MEGA("mega"),
    TERA("terastallize", "terastal"),
    DYNAMAX("dynamax"),
    Z_MOVE("z-power", "zmove"),
    ULTRA_BURST("ultra");

    private final String[] identifiers;

    MajorBattleMechanic(String... identifiers) {
        this.identifiers = identifiers;
    }

    @Nullable
    public static MajorBattleMechanic fromGimmickId(@Nullable String gimmickId) {
        if (gimmickId == null) {
            return null;
        }
        String clean = gimmickId.trim().toLowerCase();
        for (MajorBattleMechanic m : values()) {
            for (String id : m.identifiers) {
                if (id.equalsIgnoreCase(clean)) {
                    return m;
                }
            }
        }
        return null;
    }
}
