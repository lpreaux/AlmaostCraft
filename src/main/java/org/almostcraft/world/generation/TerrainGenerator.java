package org.almostcraft.world.generation;

import org.almostcraft.world.chunk.Chunk;

public interface TerrainGenerator {

    void generate(Chunk chunk, int chunkX, int chunkZ);
}
