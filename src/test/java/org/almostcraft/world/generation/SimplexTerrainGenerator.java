package org.almostcraft.world.generation;

import org.almostcraft.world.block.BlockRegistry;
import org.almostcraft.world.block.Blocks;
import org.almostcraft.world.chunk.Chunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SimplexTerrainGeneratorTest {

    private BlockRegistry registry;
    private SimplexTerrainGenerator generator;

    @BeforeEach
    void setUp() {
        registry = new BlockRegistry();
        Blocks.register(registry);
        registry.freeze();
        generator = new SimplexTerrainGenerator(registry);
    }

    @Test
    void testGeneratorCreation() {
        assertNotNull(generator);
        assertEquals(0.01f, generator.getFrequency(), 0.001f);
        assertEquals(50, generator.getMinTerrainHeight());
        assertEquals(80, generator.getMaxTerrainHeight());
    }

    @Test
    void testGenerateChunk() {
        Chunk chunk = new Chunk(0, 0);
        generator.generate(chunk, 0, 0);

        // Le chunk ne doit pas être vide
        assertFalse(chunk.isEmpty());

        // Il doit y avoir un mélange de blocs (pas tous identiques)
        assertTrue(chunk.countNonAirBlocks() > 0);
    }

    @Test
    void testTerrainHeightRange() {
        Chunk chunk = new Chunk(0, 0);
        generator.generate(chunk, 0, 0);

        int grassId = registry.getNumericId(Blocks.GRASS.id());
        int airId = registry.getNumericId(Blocks.AIR.id());

        // Trouver la hauteur de surface à plusieurs positions
        for (int x = 0; x < 16; x += 4) {
            for (int z = 0; z < 16; z += 4) {
                // Chercher l'herbe (surface)
                int surfaceHeight = -1;
                for (int y = 0; y < 256; y++) {
                    if (chunk.getVoxel(x, y, z) == grassId) {
                        surfaceHeight = y;
                        break;
                    }
                }

                // Vérifier que la surface est dans la plage attendue
                assertTrue(surfaceHeight >= 50 && surfaceHeight <= 80,
                        String.format("Surface height %d at (%d,%d) outside range [50-80]",
                                surfaceHeight, x, z));

                // Vérifier qu'il y a de l'air au-dessus
                if (surfaceHeight < 255) {
                    assertEquals(airId, chunk.getVoxel(x, surfaceHeight + 1, z));
                }
            }
        }
    }

    @Test
    void testDeterministicGeneration() {
        long seed = 42;
        SimplexTerrainGenerator gen1 = new SimplexTerrainGenerator(registry, seed);
        SimplexTerrainGenerator gen2 = new SimplexTerrainGenerator(registry, seed);

        Chunk chunk1 = new Chunk(5, 5);
        Chunk chunk2 = new Chunk(5, 5);

        gen1.generate(chunk1, 5, 5);
        gen2.generate(chunk2, 5, 5);

        // Les deux chunks doivent être identiques
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 256; y++) {
                for (int z = 0; z < 16; z++) {
                    assertEquals(chunk1.getVoxel(x, y, z), chunk2.getVoxel(x, y, z),
                            String.format("Mismatch at (%d,%d,%d)", x, y, z));
                }
            }
        }
    }

    @Test
    void testAdjacentChunksConnect() {
        Chunk chunk1 = new Chunk(0, 0);
        Chunk chunk2 = new Chunk(1, 0);

        generator.generate(chunk1, 0, 0);
        generator.generate(chunk2, 1, 0);

        // Vérifier que les bords se raccordent (même hauteur)
        // Bord droit de chunk1 vs bord gauche de chunk2
        for (int z = 0; z < 16; z++) {
            int surfaceHeight1 = findSurfaceHeight(chunk1, 15, z);
            int surfaceHeight2 = findSurfaceHeight(chunk2, 0, z);

            // La différence ne devrait pas dépasser 1-2 blocs
            int diff = Math.abs(surfaceHeight1 - surfaceHeight2);
            assertTrue(diff <= 2,
                    String.format("Chunk seam too large at z=%d: diff=%d", z, diff));
        }
    }

    @Test
    void testInvalidParameters() {
        // Fréquence invalide
        assertThrows(IllegalArgumentException.class, () ->
                new SimplexTerrainGenerator(registry, 0, -0.1f, 50, 80, 3)
        );

        // Min > Max
        assertThrows(IllegalArgumentException.class, () ->
                new SimplexTerrainGenerator(registry, 0, 0.01f, 80, 50, 3)
        );

        // Dirt depth invalide
        assertThrows(IllegalArgumentException.class, () ->
                new SimplexTerrainGenerator(registry, 0, 0.01f, 50, 80, 0)
        );
    }

    // Helper pour trouver la hauteur de surface
    private int findSurfaceHeight(Chunk chunk, int x, int z) {
        int grassId = registry.getNumericId(Blocks.GRASS.id());
        for (int y = 255; y >= 0; y--) {
            if (chunk.getVoxel(x, y, z) == grassId) {
                return y;
            }
        }
        return -1;
    }
}