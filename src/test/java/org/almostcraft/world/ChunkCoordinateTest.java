package org.almostcraft.world;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;

class ChunkCoordinateTest {

    @Test
    void testEquality() {
        ChunkCoordinate coord1 = new ChunkCoordinate(5, 10);
        ChunkCoordinate coord2 = new ChunkCoordinate(5, 10);
        ChunkCoordinate coord3 = new ChunkCoordinate(5, 11);

        // Même coordonnées → égaux
        assertEquals(coord1, coord2);

        // Coordonnées différentes → pas égaux
        assertNotEquals(coord1, coord3);
    }

    @Test
    void testHashCode() {
        ChunkCoordinate coord1 = new ChunkCoordinate(5, 10);
        ChunkCoordinate coord2 = new ChunkCoordinate(5, 10);

        // Même coordonnées → même hash
        assertEquals(coord1.hashCode(), coord2.hashCode());
    }

    @Test
    void testHashMapUsage() {
        HashMap<ChunkCoordinate, String> map = new HashMap<>();

        ChunkCoordinate coord = new ChunkCoordinate(5, 10);
        map.put(coord, "Test Chunk");

        // Récupérer avec une NOUVELLE instance ayant les mêmes coordonnées
        ChunkCoordinate lookupCoord = new ChunkCoordinate(5, 10);
        String result = map.get(lookupCoord);

        // ✅ Doit retrouver la valeur même avec une nouvelle instance
        assertEquals("Test Chunk", result);
    }

    @Test
    void testNegativeCoordinates() {
        ChunkCoordinate coord1 = new ChunkCoordinate(-5, -10);
        ChunkCoordinate coord2 = new ChunkCoordinate(-5, -10);

        assertEquals(coord1, coord2);
        assertEquals(coord1.hashCode(), coord2.hashCode());
    }
}