package org.almostcraft.world.utils;

import org.almostcraft.world.ChunkCoordinate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour {@link CoordinateUtil}.
 *
 * @author Lucas Préaux
 * @version 1.0
 */
class CoordinateUtilTest {

    // ==================== Tests World → Chunk ====================

    @Test
    void testWorldToChunkPositive() {
        assertEquals(0, CoordinateUtil.worldToChunkX(0));
        assertEquals(0, CoordinateUtil.worldToChunkX(15));
        assertEquals(1, CoordinateUtil.worldToChunkX(16));
        assertEquals(1, CoordinateUtil.worldToChunkX(31));
        assertEquals(2, CoordinateUtil.worldToChunkX(32));
    }

    @Test
    void testWorldToChunkNegative() {
        assertEquals(-1, CoordinateUtil.worldToChunkX(-1));
        assertEquals(-1, CoordinateUtil.worldToChunkX(-15));
        assertEquals(-1, CoordinateUtil.worldToChunkX(-16));
        assertEquals(-2, CoordinateUtil.worldToChunkX(-17));
        assertEquals(-2, CoordinateUtil.worldToChunkX(-32));
    }

    @Test
    void testWorldToChunkZ() {
        // Test similaires pour Z
        assertEquals(0, CoordinateUtil.worldToChunkZ(0));
        assertEquals(1, CoordinateUtil.worldToChunkZ(16));
        assertEquals(-1, CoordinateUtil.worldToChunkZ(-1));
        assertEquals(-2, CoordinateUtil.worldToChunkZ(-17));
    }

    @Test
    void testWorldToChunkCoordinate() {
        ChunkCoordinate coord = CoordinateUtil.worldToChunk(47, -33);
        assertEquals(2, coord.x());
        assertEquals(-3, coord.z());
    }

    // ==================== Tests World → Local ====================

    @Test
    void testWorldToLocalPositive() {
        assertEquals(0, CoordinateUtil.worldToLocalX(0));
        assertEquals(15, CoordinateUtil.worldToLocalX(15));
        assertEquals(0, CoordinateUtil.worldToLocalX(16));
        assertEquals(1, CoordinateUtil.worldToLocalX(17));
    }

    @Test
    void testWorldToLocalNegative() {
        // Tests critiques pour les coordonnées négatives !
        assertEquals(15, CoordinateUtil.worldToLocalX(-1));
        assertEquals(1, CoordinateUtil.worldToLocalX(-15));
        assertEquals(0, CoordinateUtil.worldToLocalX(-16));
        assertEquals(15, CoordinateUtil.worldToLocalX(-17));
    }

    @Test
    void testWorldToLocalY() {
        assertEquals(0, CoordinateUtil.worldToLocalY(0));
        assertEquals(64, CoordinateUtil.worldToLocalY(64));
        assertEquals(255, CoordinateUtil.worldToLocalY(255));
    }

    @Test
    void testWorldToLocalYInvalid() {
        // Y négatif doit lancer une exception
        assertThrows(IllegalArgumentException.class, () -> {
            CoordinateUtil.worldToLocalY(-1);
        });

        // Y >= 256 doit lancer une exception
        assertThrows(IllegalArgumentException.class, () -> {
            CoordinateUtil.worldToLocalY(256);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            CoordinateUtil.worldToLocalY(300);
        });
    }

    // ==================== Tests Chunk → World ====================

    @Test
    void testChunkToWorld() {
        assertEquals(0, CoordinateUtil.chunkToWorldX(0));
        assertEquals(16, CoordinateUtil.chunkToWorldX(1));
        assertEquals(-16, CoordinateUtil.chunkToWorldX(-1));
        assertEquals(-32, CoordinateUtil.chunkToWorldX(-2));
    }

    // ==================== Tests aller-retour ====================

    @Test
    void testRoundTripConversionPositive() {
        int worldX = 47;
        int chunkX = CoordinateUtil.worldToChunkX(worldX);
        int localX = CoordinateUtil.worldToLocalX(worldX);
        int reconstructed = CoordinateUtil.chunkToWorldX(chunkX) + localX;

        assertEquals(worldX, reconstructed);
    }

    @Test
    void testRoundTripConversionNegative() {
        int worldX = -25;
        int chunkX = CoordinateUtil.worldToChunkX(worldX);
        int localX = CoordinateUtil.worldToLocalX(worldX);
        int reconstructed = CoordinateUtil.chunkToWorldX(chunkX) + localX;

        assertEquals(worldX, reconstructed);
    }

    @Test
    void testRoundTripMultipleCoordinates() {
        // Tester plusieurs coordonnées pour être sûr
        int[] testCases = {-100, -50, -17, -16, -1, 0, 1, 16, 17, 50, 100};

        for (int worldX : testCases) {
            int chunkX = CoordinateUtil.worldToChunkX(worldX);
            int localX = CoordinateUtil.worldToLocalX(worldX);
            int reconstructed = CoordinateUtil.chunkToWorldX(chunkX) + localX;

            assertEquals(worldX, reconstructed,
                    String.format("Round-trip failed for worldX=%d", worldX));
        }
    }

    // ==================== Tests validation ====================

    @Test
    void testIsValidWorldY() {
        assertTrue(CoordinateUtil.isValidWorldY(0));
        assertTrue(CoordinateUtil.isValidWorldY(128));
        assertTrue(CoordinateUtil.isValidWorldY(255));

        assertFalse(CoordinateUtil.isValidWorldY(-1));
        assertFalse(CoordinateUtil.isValidWorldY(256));
        assertFalse(CoordinateUtil.isValidWorldY(300));
    }

    @Test
    void testIsValidLocal() {
        assertTrue(CoordinateUtil.isValidLocal(0, 0, 0));
        assertTrue(CoordinateUtil.isValidLocal(15, 255, 15));
        assertTrue(CoordinateUtil.isValidLocal(8, 128, 8));

        assertFalse(CoordinateUtil.isValidLocal(-1, 0, 0));
        assertFalse(CoordinateUtil.isValidLocal(16, 0, 0));
        assertFalse(CoordinateUtil.isValidLocal(0, 256, 0));
        assertFalse(CoordinateUtil.isValidLocal(0, 0, 16));
    }

    // ==================== Tests des cas limites ====================

    @Test
    void testChunkBoundaries() {
        // Tester les transitions entre chunks
        // worldX = 15 et worldX = 16 doivent être dans des chunks différents
        int chunk15 = CoordinateUtil.worldToChunkX(15);
        int chunk16 = CoordinateUtil.worldToChunkX(16);
        assertEquals(0, chunk15);
        assertEquals(1, chunk16);

        // Même chose pour les négatifs
        int chunkNeg16 = CoordinateUtil.worldToChunkX(-16);
        int chunkNeg17 = CoordinateUtil.worldToChunkX(-17);
        assertEquals(-1, chunkNeg16);
        assertEquals(-2, chunkNeg17);
    }

    @Test
    void testLocalBoundaries() {
        // Local doit être 15 juste avant la limite du chunk
        assertEquals(15, CoordinateUtil.worldToLocalX(15));
        assertEquals(0, CoordinateUtil.worldToLocalX(16));

        assertEquals(15, CoordinateUtil.worldToLocalX(-1));
        assertEquals(0, CoordinateUtil.worldToLocalX(-16));
    }
}