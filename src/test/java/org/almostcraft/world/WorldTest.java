package org.almostcraft.world;

import org.almostcraft.world.block.BlockRegistry;
import org.almostcraft.world.block.Blocks;
import org.almostcraft.world.chunk.Chunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorldTest {

    private BlockRegistry registry;
    private World world;

    @BeforeEach
    void setUp() {
        registry = new BlockRegistry();
        Blocks.register(registry);
        registry.freeze();
        world = new World(registry);
    }

    @Test
    void testGetChunkCreatesChunkIfNotExists() {
        assertFalse(world.hasChunk(0, 0));

        Chunk chunk = world.getChunk(0, 0);
        assertNotNull(chunk);
        assertTrue(world.hasChunk(0, 0));
    }

    @Test
    void testSetAndGetBlockAt() {
        int stoneId = registry.getNumericId(Blocks.STONE.id());

        // Placer un bloc de pierre à (47, 64, 33)
        world.setBlockAt(47, 64, 33, stoneId);

        // Lire le bloc
        int blockId = world.getBlockAt(47, 64, 33);
        assertEquals(stoneId, blockId);
    }

    @Test
    void testSetAndGetBlockAtNegativeCoordinates() {
        int stoneId = registry.getNumericId(Blocks.STONE.id());

        // Placer un bloc à des coordonnées négatives
        world.setBlockAt(-1, 64, -1, stoneId);

        // Lire le bloc
        int blockId = world.getBlockAt(-1, 64, -1);
        assertEquals(stoneId, blockId);

        // Vérifier que le chunk (-1, -1) a été créé
        assertTrue(world.hasChunk(-1, -1));
    }

    @Test
    void testGetBlockTypeAt() {
        int stoneId = registry.getNumericId(Blocks.STONE.id());
        world.setBlockAt(10, 50, 10, stoneId);

        var blockType = world.getBlockTypeAt(10, 50, 10);
        assertNotNull(blockType);
        assertEquals(Blocks.STONE.id(), blockType.id());
    }

    @Test
    void testDefaultBlockIsAir() {
        // Un chunk nouvellement créé doit être rempli d'air (0)
        int blockId = world.getBlockAt(0, 0, 0);
        assertEquals(0, blockId);
    }

    @Test
    void testInvalidYThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            world.getBlockAt(0, -1, 0);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            world.getBlockAt(0, 256, 0);
        });
    }

    @Test
    void testMultipleChunksCreated() {
        // Placer des blocs dans différents chunks
        world.setBlockAt(0, 64, 0, 1);    // Chunk (0, 0)
        world.setBlockAt(16, 64, 0, 1);   // Chunk (1, 0)
        world.setBlockAt(0, 64, 16, 1);   // Chunk (0, 1)
        world.setBlockAt(-1, 64, -1, 1);  // Chunk (-1, -1)

        assertEquals(4, world.getLoadedChunkCount());
    }

    @Test
    void testChunkBoundaries() {
        int stoneId = registry.getNumericId(Blocks.STONE.id());

        // Placer un bloc à la frontière entre chunks
        world.setBlockAt(15, 64, 15, stoneId);  // Chunk (0, 0)
        world.setBlockAt(16, 64, 16, stoneId);  // Chunk (1, 1)

        assertEquals(stoneId, world.getBlockAt(15, 64, 15));
        assertEquals(stoneId, world.getBlockAt(16, 64, 16));

        // Vérifier que les bons chunks ont été créés
        assertTrue(world.hasChunk(0, 0));
        assertTrue(world.hasChunk(1, 1));
    }

    @Test
    void testRemoveChunk() {
        world.getChunk(5, 5);
        assertTrue(world.hasChunk(5, 5));

        world.removeChunk(5, 5);
        assertFalse(world.hasChunk(5, 5));
    }
}