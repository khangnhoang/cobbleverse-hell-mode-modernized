package com.cobbleverse.legendaryrule.lead;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RosterOrdererTest {

    @Test
    void testReorderStandardSixMemberRoster() {
        String[] original = {"indeedee", "farigiraf", "hatterene", "metagross", "armarouge", "alakazam"};
        String[] originalCopy = original.clone();

        // Preset 0: [0, 5] -> indeedee + alakazam lead, followed by farigiraf, hatterene, metagross, armarouge
        String[] reordered0 = RosterOrderer.reorder(original, new int[]{0, 5}, String[]::new);
        assertArrayEquals(new String[]{"indeedee", "alakazam", "farigiraf", "hatterene", "metagross", "armarouge"}, reordered0);

        // Preset 1: [1, 2] -> farigiraf + hatterene lead, followed by indeedee, metagross, armarouge, alakazam
        String[] reordered1 = RosterOrderer.reorder(original, new int[]{1, 2}, String[]::new);
        assertArrayEquals(new String[]{"farigiraf", "hatterene", "indeedee", "metagross", "armarouge", "alakazam"}, reordered1);

        // Preset 2: [0, 3] -> indeedee + metagross lead, followed by farigiraf, hatterene, armarouge, alakazam
        String[] reordered2 = RosterOrderer.reorder(original, new int[]{0, 3}, String[]::new);
        assertArrayEquals(new String[]{"indeedee", "metagross", "farigiraf", "hatterene", "armarouge", "alakazam"}, reordered2);

        // Reverse order slots: [3, 0]
        String[] reorderedReverse = RosterOrderer.reorder(original, new int[]{3, 0}, String[]::new);
        assertArrayEquals(new String[]{"metagross", "indeedee", "farigiraf", "hatterene", "armarouge", "alakazam"}, reorderedReverse);

        // Verify input array is completely unmodified
        assertArrayEquals(originalCopy, original, "Original array must never be mutated");
    }

    @Test
    void testReorderTwoMemberTeam() {
        Integer[] team = {10, 20};
        Integer[] result = RosterOrderer.reorder(team, new int[]{1, 0}, Integer[]::new);
        assertArrayEquals(new Integer[]{20, 10}, result);
    }

    @Test
    void testReorderPreservesBacklineRelativeOrder() {
        Character[] team = {'A', 'B', 'C', 'D', 'E', 'F'};
        // Select leads C (index 2) and E (index 4)
        Character[] result = RosterOrderer.reorder(team, new int[]{2, 4}, Character[]::new);
        assertArrayEquals(new Character[]{'C', 'E', 'A', 'B', 'D', 'F'}, result);
    }

    @Test
    void testThrowsOnInvalidArguments() {
        String[] team = {"A", "B", "C"};

        assertThrows(NullPointerException.class, () -> RosterOrderer.reorder(null, new int[]{0, 1}, String[]::new));
        assertThrows(NullPointerException.class, () -> RosterOrderer.reorder(team, null, String[]::new));
        assertThrows(NullPointerException.class, () -> RosterOrderer.reorder(team, new int[]{0, 1}, null));

        // Invalid length
        assertThrows(IllegalArgumentException.class, () -> RosterOrderer.reorder(team, new int[]{0}, String[]::new));
        assertThrows(IllegalArgumentException.class, () -> RosterOrderer.reorder(team, new int[]{0, 1, 2}, String[]::new));

        // Duplicate indices
        assertThrows(IllegalArgumentException.class, () -> RosterOrderer.reorder(team, new int[]{1, 1}, String[]::new));

        // Out of bounds
        assertThrows(IndexOutOfBoundsException.class, () -> RosterOrderer.reorder(team, new int[]{-1, 1}, String[]::new));
        assertThrows(IndexOutOfBoundsException.class, () -> RosterOrderer.reorder(team, new int[]{0, 3}, String[]::new));
    }
}
