package org.almostcraft.graphics.culling;

import org.almostcraft.camera.Camera;
import org.almostcraft.world.chunk.Chunk;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Manages chunk culling operations to optimize rendering performance.
 * Applies distance culling, frustum culling, and tracks statistics.
 */
public class CullingManager {

    private static final Logger logger = LoggerFactory.getLogger(CullingManager.class);

    // Culling configuration
    private int renderDistance;           // In chunks (e.g., 8 = 8*16 = 128 blocks)
    private float renderDistanceSquared;  // Cached squared distance for faster checks

    // Culling components
    private final Frustum frustum;
    private final CullingStats stats;

    // Cached values (updated each frame)
    private Vector3f cameraPosition;
    private Vector3f playerChunkPosition;

    /**
     * Creates a new culling manager with the specified render distance.
     *
     * @param renderDistance Maximum render distance in chunks
     */
    public CullingManager(int renderDistance) {
        this.renderDistance = renderDistance;
        this.renderDistanceSquared = calculateRenderDistanceSquared();

        this.frustum = new Frustum();
        this.stats = new CullingStats();

        this.cameraPosition = new Vector3f();
        this.playerChunkPosition = new Vector3f();

        logger.info("CullingManager initialized with render distance: {} chunks ({} blocks)",
                renderDistance, renderDistance * 16);
    }

    /**
     * Updates the culling manager's internal state based on camera and player position.
     * Should be called once per frame before culling chunks.
     *
     * @param camera Current camera
     * @param playerPosition Current player world position
     */
    public void update(Camera camera, Vector3f playerPosition) {
        // Update cached camera position
        cameraPosition.set(camera.getPosition());

        // Calculate player's chunk position
        playerChunkPosition.set(
                (float) Math.floor(playerPosition.x / 16f),
                (float) Math.floor(playerPosition.y / 16f),
                (float) Math.floor(playerPosition.z / 16f)
        );

        // Update frustum from camera's view-projection matrix
        frustum.update(camera.getViewProjectionMatrix());

        // Reset stats for this frame
        stats.reset();

        logger.trace("CullingManager updated - Camera: {}, PlayerChunk: ({}, {}, {})",
                cameraPosition,
                (int) playerChunkPosition.x,
                (int) playerChunkPosition.y,
                (int) playerChunkPosition.z);
    }

    /**
     * Culls the provided chunks and returns only those that should be rendered.
     * Applies distance culling, frustum culling, and updates statistics.
     *
     * @param chunks Collection of chunks to cull
     * @return List of chunks that passed all culling tests
     */
    public List<Chunk> cullChunks(Collection<Chunk> chunks) {
        long startTime = System.nanoTime();

        List<Chunk> visibleChunks = new ArrayList<>();

        for (Chunk chunk : chunks) {
            stats.incrementTotalLoaded();

            // Skip chunks without geometry
            if (chunk.isEmpty()) {
                stats.incrementEmptyChunks();
                continue;
            }

            // Skip chunks without a mesh
            if (!chunk.hasMesh()) {
                stats.incrementPendingMeshGeneration();
                continue;
            }

            // Distance culling
            if (!isChunkInRenderDistance(chunk)) {
                stats.incrementDistanceCulled();
                continue;
            }

            // Frustum culling
            if (!isChunkInFrustum(chunk)) {
                stats.incrementFrustumCulled();
                continue;
            }

            // Chunk is visible
            stats.incrementRendered();
            visibleChunks.add(chunk);
        }

        long endTime = System.nanoTime();
        stats.addCullingTime(endTime - startTime);

        logger.trace("Culled {} chunks -> {} visible", chunks.size(), visibleChunks.size());

        return visibleChunks;
    }

    /**
     * Tests if a chunk is within the render distance.
     * Uses squared distance to avoid expensive sqrt() calls.
     *
     * @param chunk Chunk to test
     * @return true if the chunk is within render distance
     */
    private boolean isChunkInRenderDistance(Chunk chunk) {
        // Calculate chunk position
        float chunkX = chunk.getChunkX();
        float chunkZ = chunk.getChunkZ();

        // Calculate squared distance from player chunk position
        float dx = chunkX - playerChunkPosition.x;
        float dz = chunkZ - playerChunkPosition.z;
        float distanceSquared = dx * dx + dz * dz;

        return distanceSquared <= renderDistanceSquared;
    }

    /**
     * Tests if a chunk's bounding box intersects the view frustum.
     *
     * @param chunk Chunk to test
     * @return true if the chunk is visible in the frustum
     */
    private boolean isChunkInFrustum(Chunk chunk) {
        BoundingBox boundingBox = chunk.getBoundingBox();
        return frustum.intersects(boundingBox);
    }

    /**
     * Calculates the squared render distance for efficient distance checks.
     *
     * @return Squared render distance in chunks
     */
    private float calculateRenderDistanceSquared() {
        return renderDistance * renderDistance;
    }

    // ========== Configuration ==========

    /**
     * Sets the render distance and updates cached values.
     *
     * @param renderDistance New render distance in chunks (must be positive)
     */
    public void setRenderDistance(int renderDistance) {
        if (renderDistance <= 0) {
            logger.warn("Invalid render distance: {}. Must be positive. Ignoring.", renderDistance);
            return;
        }

        int oldDistance = this.renderDistance;
        this.renderDistance = renderDistance;
        this.renderDistanceSquared = calculateRenderDistanceSquared();

        logger.info("Render distance changed: {} -> {} chunks ({} -> {} blocks)",
                oldDistance, renderDistance, oldDistance * 16, renderDistance * 16);
    }

    /**
     * Gets the current render distance in chunks.
     *
     * @return Render distance in chunks
     */
    public int getRenderDistance() {
        return renderDistance;
    }

    /**
     * Gets the current render distance in blocks.
     *
     * @return Render distance in blocks
     */
    public int getRenderDistanceBlocks() {
        return renderDistance * 16;
    }

    // ========== Statistics ==========

    /**
     * Gets the current culling statistics.
     * Statistics are reset each frame during update().
     *
     * @return Current frame's culling statistics
     */
    public CullingStats getStats() {
        return stats;
    }

    /**
     * Gets the frustum for external use (e.g., for occlusion culling).
     *
     * @return Current view frustum
     */
    public Frustum getFrustum() {
        return frustum;
    }

    // ========== Advanced Culling Methods ==========

    /**
     * Tests if a single chunk should be rendered.
     * Useful for on-demand checks without full collection culling.
     *
     * @param chunk Chunk to test
     * @return true if the chunk should be rendered
     */
    public boolean shouldRenderChunk(Chunk chunk) {
        if (chunk.isEmpty() || !chunk.hasMesh()) {
            return false;
        }

        if (!isChunkInRenderDistance(chunk)) {
            return false;
        }

        return isChunkInFrustum(chunk);
    }

    /**
     * Calculates the squared distance from the player to a chunk.
     *
     * @param chunk Chunk to calculate distance to
     * @return Squared distance in chunks
     */
    public float getChunkDistanceSquared(Chunk chunk) {
        float dx = chunk.getChunkX() - playerChunkPosition.x;
        float dz = chunk.getChunkZ() - playerChunkPosition.z;
        return dx * dx + dz * dz;
    }

    /**
     * Gets chunks sorted by distance from the player (nearest first).
     * Useful for prioritizing mesh generation or rendering order.
     *
     * @param chunks Chunks to sort
     * @return List of chunks sorted by distance
     */
    public List<Chunk> sortChunksByDistance(Collection<Chunk> chunks) {
        List<Chunk> sortedChunks = new ArrayList<>(chunks);

        sortedChunks.sort((c1, c2) -> {
            float dist1 = getChunkDistanceSquared(c1);
            float dist2 = getChunkDistanceSquared(c2);
            return Float.compare(dist1, dist2);
        });

        return sortedChunks;
    }

    /**
     * Gets chunks within a specific distance range.
     *
     * @param chunks Chunks to filter
     * @param minDistance Minimum distance in chunks (inclusive)
     * @param maxDistance Maximum distance in chunks (inclusive)
     * @return List of chunks within the distance range
     */
    public List<Chunk> getChunksInRange(Collection<Chunk> chunks, int minDistance, int maxDistance) {
        float minDistSquared = minDistance * minDistance;
        float maxDistSquared = maxDistance * maxDistance;

        List<Chunk> chunksInRange = new ArrayList<>();

        for (Chunk chunk : chunks) {
            float distSquared = getChunkDistanceSquared(chunk);
            if (distSquared >= minDistSquared && distSquared <= maxDistSquared) {
                chunksInRange.add(chunk);
            }
        }

        return chunksInRange;
    }

    // ========== Debug Methods ==========

    /**
     * Logs detailed culling statistics.
     */
    public void logStats() {
        logger.info(stats.getLogSummary());
    }

    /**
     * Logs detailed culling analysis with recommendations.
     */
    public void logAnalysis() {
        logger.info("\n{}", stats.getDetailedSummary());
        logger.info("\n{}", stats.getEfficiencyAnalysis());
    }

    /**
     * Gets debug information about the culling manager's state.
     *
     * @return Debug information string
     */
    public String getDebugInfo() {
        return String.format(
                "CullingManager[renderDist=%d chunks (%d blocks), cameraPos=%s, playerChunk=(%d,%d,%d)]",
                renderDistance,
                renderDistance * 16,
                cameraPosition,
                (int) playerChunkPosition.x,
                (int) playerChunkPosition.y,
                (int) playerChunkPosition.z
        );
    }

    @Override
    public String toString() {
        return String.format("CullingManager[renderDistance=%d, stats=%s]",
                renderDistance, stats.getCompactSummary());
    }
}
