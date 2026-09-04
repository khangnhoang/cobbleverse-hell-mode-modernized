package com.cobbleverse.legendaryrule.lead;

import java.util.Objects;
import java.util.function.IntFunction;

/**
 * Pure domain utility for reordering Pokémon rosters given lead slots.
 * Guarantees slotA -> index 0, slotB -> index 1, and original relative order for all backline members.
 * The original input array is never modified.
 */
public final class RosterOrderer {
    private RosterOrderer() {}

    public static <T> T[] reorder(T[] original, int[] leadSlots, IntFunction<T[]> generator) {
        Objects.requireNonNull(original, "original array must not be null");
        Objects.requireNonNull(leadSlots, "leadSlots must not be null");
        Objects.requireNonNull(generator, "generator must not be null");

        if (leadSlots.length != 2) {
            throw new IllegalArgumentException("leadSlots must have length 2, got: " + leadSlots.length);
        }
        int slotA = leadSlots[0];
        int slotB = leadSlots[1];
        if (slotA < 0 || slotA >= original.length || slotB < 0 || slotB >= original.length) {
            throw new IndexOutOfBoundsException("Lead slot out of bounds for array of size " + original.length + ": [" + slotA + ", " + slotB + "]");
        }
        if (slotA == slotB) {
            throw new IllegalArgumentException("Lead slots must be distinct: [" + slotA + ", " + slotB + "]");
        }

        T[] result = generator.apply(original.length);
        result[0] = original[slotA];
        result[1] = original[slotB];

        int outIdx = 2;
        for (int i = 0; i < original.length; i++) {
            if (i != slotA && i != slotB) {
                result[outIdx++] = original[i];
            }
        }
        return result;
    }
}
