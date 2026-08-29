package com.cobbleverse.legendaryrule;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class CompanionConfigTest {

    @TempDir
    Path tempDir;

    private Path testConfigPath;

    @BeforeEach
    public void setup() {
        testConfigPath = tempDir.resolve("test-companion.json");
        CompanionConfig.setConfigPath(testConfigPath);
        CompanionConfig.setMaxLegendaryMythical(CompanionConfig.DEFAULT_LIMIT);
    }

    @AfterEach
    public void teardown() {
        CompanionConfig.setConfigPath(null);
    }

    @Test
    public void testDefaultValueIsOne() {
        assertEquals(1, CompanionConfig.DEFAULT_LIMIT);
        assertEquals(1, CompanionConfig.getMaxLegendaryMythical());
    }

    @Test
    public void testValidRangeValues() {
        for (int i = 0; i <= 6; i++) {
            CompanionConfig.setMaxLegendaryMythical(i);
            assertEquals(i, CompanionConfig.getMaxLegendaryMythical());
        }
    }

    @Test
    public void testOutOfRangeThrows() {
        assertThrows(IllegalArgumentException.class, () -> CompanionConfig.setMaxLegendaryMythical(-1));
        assertThrows(IllegalArgumentException.class, () -> CompanionConfig.setMaxLegendaryMythical(7));
        assertThrows(IllegalArgumentException.class, () -> CompanionConfig.setMaxLegendaryMythical(100));
    }

    @Test
    public void testSaveAndLoadSurvives() {
        CompanionConfig.setMaxLegendaryMythical(3);
        CompanionConfig.save(testConfigPath);
        assertTrue(Files.exists(testConfigPath));

        // Reset in memory to default
        CompanionConfig.setMaxLegendaryMythical(1);
        assertEquals(1, CompanionConfig.getMaxLegendaryMythical());

        // Load back from file
        CompanionConfig.load(testConfigPath);
        assertEquals(3, CompanionConfig.getMaxLegendaryMythical());
    }

    @Test
    public void testMissingFileCreatesDefault() {
        assertFalse(Files.exists(testConfigPath));
        CompanionConfig.load(testConfigPath);
        assertTrue(Files.exists(testConfigPath));
        assertEquals(1, CompanionConfig.getMaxLegendaryMythical());
    }

    @Test
    public void testCorruptedJsonFallsBackToDefault() throws IOException {
        Files.writeString(testConfigPath, "{ not valid json ::::");
        CompanionConfig.load(testConfigPath);
        assertEquals(1, CompanionConfig.getMaxLegendaryMythical());
    }

    @Test
    public void testOutOfRangeInJsonFallsBackToDefault() throws IOException {
        Files.writeString(testConfigPath, "{\"maxLegendaryMythical\": 99}");
        CompanionConfig.load(testConfigPath);
        assertEquals(1, CompanionConfig.getMaxLegendaryMythical());
    }
}
