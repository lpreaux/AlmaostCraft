package org.almostcraft.world;

import org.almostcraft.world.block.BlockRegistry;
import org.almostcraft.world.block.Blocks;
import org.almostcraft.world.generation.FlatTerrainGenerator;
import org.joml.Vector3f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChunkLoaderTest {

    private World world;
    private ChunkLoader chunkLoader;

    @BeforeEach
    void setUp() {
        BlockRegistry registry = new BlockRegistry();
        Blocks.register(registry);
        registry.freeze();

        FlatTerrainGenerator generator = new FlatTerrainGenerator(registry);
        world = new World(generator, registry);
        chunkLoader = new ChunkLoader(world, 4); // Render distance = 4
    }

    @Test
    void testInitialChunkLoading() {
        Vector3f spawnPos = new Vector3f(0, 64, 0);
        chunkLoader.loadInitialChunks(spawnPos);

        // Avec render distance = 4, on devrait avoir (2*4+1)² = 81 chunks
        assertEquals(81, world.getLoadedChunkCount());

        // Le chunk central doit être chargé
        assertTrue(world.hasChunk(0, 0));
    }

    @Test
    void testChunkLoadingOnMovement() {
        Vector3f pos = new Vector3f(0, 64, 0);
        chunkLoader.loadInitialChunks(pos);

        int initialCount = world.getLoadedChunkCount();

        // Déplacer le joueur vers (100, 64, 0) = chunk (6, 0)
        pos.set(100, 64, 0);

        // Simuler plusieurs frames d'update
        for (int i = 0; i < 50; i++) {
            chunkLoader.update(pos);
        }

        // Des chunks devraient avoir été chargés/déchargés
        int finalCount = world.getLoadedChunkCount();

        // Le nombre total devrait rester similaire
        assertTrue(Math.abs(finalCount - initialCount) < 20);

        // Le nouveau chunk central devrait être chargé
        assertTrue(world.hasChunk(6, 0));
    }

    @Test
    void testChunkUnloading() {
        Vector3f pos = new Vector3f(0, 64, 0);
        chunkLoader.loadInitialChunks(pos);

        // Se téléporter très loin
        pos.set(1000, 64, 1000);

        // Forcer l'update plusieurs fois
        for (int i = 0; i < 100; i++) {
            chunkLoader.update(pos);
        }

        // Les anciens chunks devraient être déchargés
        assertFalse(world.hasChunk(0, 0));

        // Les nouveaux chunks devraient être chargés
        assertTrue(world.hasChunk(62, 62)); // 1000/16 ≈ 62
    }

    @Test
    void testRenderDistanceChange() {
        chunkLoader.setRenderDistance(2);
        assertEquals(2, chunkLoader.getRenderDistance());

        Vector3f pos = new Vector3f(0, 64, 0);
        chunkLoader.loadInitialChunks(pos);

        // Avec render distance = 2, on devrait avoir (2*2+1)² = 25 chunks
        assertEquals(25, world.getLoadedChunkCount());
    }

    @Test
    void testInvalidRenderDistance() {
        assertThrows(IllegalArgumentException.class, () ->
                new ChunkLoader(world, 0)
        );

        assertThrows(IllegalArgumentException.class, () ->
                new ChunkLoader(world, 33)
        );
    }
}
