package org.almostcraft.graphics.culling;

import org.almostcraft.world.chunk.Chunk;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Simple software occlusion culling system for chunks.
 * <p>
 * Uses a conservative approach with directional occlusion testing:
 * a chunk is only culled if it's completely behind a solid occluder
 * in the direction from the camera.
 * </p>
 * <p>
 * <strong>Algorithm:</strong>
 * <ol>
 *   <li>Identify "occluder" chunks (fully opaque, close to camera)</li>
 *   <li>For each chunk, test if it's behind any occluder</li>
 *   <li>Use conservative bounding box tests to avoid false culling</li>
 * </ol>
 * </p>
 * <p>
 * <strong>Performance characteristics:</strong>
 * <ul>
 *   <li>O(n²) worst case, but typically O(n×k) where k is small</li>
 *   <li>Early exit optimizations reduce actual comparisons</li>
 *   <li>Works best with 30-70% occlusion in the scene</li>
 * </ul>
 * </p>
 *
 * @author Lucas Préaux
 * @version 1.0
 * @see CullingManager
 */
public class ChunkOcclusionCuller {

    private static final Logger logger = LoggerFactory.getLogger(ChunkOcclusionCuller.class);

    // ==================== Configuration ====================

    /**
     * Maximum distance (in chunks) for a chunk to be considered an occluder.
     * Chunks further than this are too far to reliably occlude others.
     */
    private static final int MAX_OCCLUDER_DISTANCE = 6;

    /**
     * Minimum opacity threshold for a chunk to be an occluder.
     * 1.0 = completely solid, 0.0 = completely empty.
     * We use 0.95 to be slightly conservative.
     */
    private static final float MIN_OCCLUDER_OPACITY = 0.95f;

    /**
     * Minimum size ratio for occlusion to occur.
     * The occluder must be at least this fraction of the occluded chunk's size.
     * This prevents small chunks from occluding large distant chunks.
     */
    private static final float MIN_SIZE_RATIO = 0.8f;

    /**
     * Margin for occlusion testing (in blocks).
     * A chunk must be this many blocks behind the occluder to be culled.
     * This prevents flickering at occlusion boundaries.
     */
    private static final float OCCLUSION_MARGIN = 2.0f;

    // ==================== State ====================

    /** Camera position (updated each frame) */
    private final Vector3f cameraPosition = new Vector3f();

    /** Cached list of potential occluders (regenerated when needed) */
    private final List<Chunk> potentialOccluders = new ArrayList<>();

    /** Enable/disable occlusion culling */
    private boolean enabled = true;

    /** Debug mode: log detailed occlusion decisions */
    private boolean debugMode = false;

    // ==================== Public API ====================

    /**
     * Creates a new occlusion culler.
     */
    public ChunkOcclusionCuller() {
        logger.info("ChunkOcclusionCuller initialized with MAX_OCCLUDER_DISTANCE={}, MIN_OCCLUDER_OPACITY={}",
                MAX_OCCLUDER_DISTANCE, MIN_OCCLUDER_OPACITY);
    }

    /**
     * Updates the culler's state for the current frame.
     * Should be called once per frame before culling.
     *
     * @param cameraPosition Current camera world position
     * @param visibleChunks Chunks that passed frustum culling (potential occluders)
     */
    public void update(Vector3f cameraPosition, Collection<Chunk> visibleChunks) {
        this.cameraPosition.set(cameraPosition);

        // Rebuild occluder list
        potentialOccluders.clear();

        for (Chunk chunk : visibleChunks) {
            if (isValidOccluder(chunk)) {
                potentialOccluders.add(chunk);
            }
        }

        // Sort occluders by distance (closest first)
        // Closest chunks are most likely to occlude others
        potentialOccluders.sort(Comparator.comparingDouble(
                chunk -> getSquaredDistanceToCamera(chunk)
        ));

        if (debugMode && !potentialOccluders.isEmpty()) {
            logger.debug("Found {} potential occluders", potentialOccluders.size());
        }
    }

    /**
     * Tests if a chunk is occluded by any other chunk.
     * <p>
     * This is the main entry point for occlusion testing.
     * </p>
     *
     * @param chunk Chunk to test
     * @return true if the chunk is completely occluded and should not be rendered
     */
    public boolean isOccluded(Chunk chunk) {
        if (!enabled || potentialOccluders.isEmpty()) {
            return false;
        }

        BoundingBox chunkBounds = chunk.getBoundingBox();

        // Test against each occluder
        for (Chunk occluder : potentialOccluders) {
            // Don't test a chunk against itself
            if (occluder == chunk) {
                continue;
            }

            // Test if this occluder hides the chunk
            if (isOccludedBy(chunk, chunkBounds, occluder)) {
                if (debugMode) {
                    logger.debug("Chunk ({}, {}) occluded by ({}, {})",
                            chunk.getChunkX(), chunk.getChunkZ(),
                            occluder.getChunkX(), occluder.getChunkZ());
                }
                return true;
            }
        }

        return false;
    }

    // ==================== Internal Logic ====================

    /**
     * Determines if a chunk is a valid occluder.
     * <p>
     * A chunk can occlude others if it:
     * <ul>
     *   <li>Is close enough to the camera</li>
     *   <li>Is mostly opaque (few empty blocks)</li>
     *   <li>Has a valid mesh</li>
     * </ul>
     * </p>
     *
     * @param chunk Chunk to test
     * @return true if the chunk can occlude others
     */
    private boolean isValidOccluder(Chunk chunk) {
        // Must have geometry to occlude
        if (chunk.isEmpty() || !chunk.hasMesh()) {
            return false;
        }

        // Must be close enough to camera
        float distSquared = getSquaredDistanceToCamera(chunk);
        float maxDistSquared = MAX_OCCLUDER_DISTANCE * MAX_OCCLUDER_DISTANCE * 256f; // 256 = 16*16 (chunk size squared)

        if (distSquared > maxDistSquared) {
            return false;
        }

        // Must be mostly opaque
        float opacity = calculateChunkOpacity(chunk);
        return opacity >= MIN_OCCLUDER_OPACITY;
    }

    /**
     * Tests if a chunk is occluded by a specific occluder.
     * <p>
     * Uses conservative bounding box projection to determine occlusion.
     * </p>
     *
     * @param chunk Chunk to test
     * @param chunkBounds Precomputed bounding box of the chunk
     * @param occluder Potential occluder
     * @return true if chunk is completely hidden by occluder
     */
    private boolean isOccludedBy(Chunk chunk, BoundingBox chunkBounds, Chunk occluder) {
        BoundingBox occluderBounds = occluder.getBoundingBox();

        // 1. Check if chunk is behind occluder from camera perspective
        if (!isBehind(chunkBounds, occluderBounds)) {
            return false;
        }

        // 2. Check if occluder's projected shadow covers the chunk
        // Project both boxes onto a plane perpendicular to view direction
        Vector3f viewDir = getViewDirection(chunkBounds.getCenter());

        if (!doesShadowCover(occluderBounds, chunkBounds, viewDir)) {
            return false;
        }

        // 3. Verify size ratio (prevent small chunks occluding large ones)
        if (!meetsMinimumSizeRatio(occluderBounds, chunkBounds)) {
            return false;
        }

        return true;
    }

    /**
     * Checks if the target chunk is behind the occluder from camera perspective.
     *
     * @param targetBounds Target chunk bounds
     * @param occluderBounds Occluder bounds
     * @return true if target is behind occluder
     */
    private boolean isBehind(BoundingBox targetBounds, BoundingBox occluderBounds) {
        Vector3f targetCenter = targetBounds.getCenter();
        Vector3f occluderCenter = occluderBounds.getCenter();

        // Calculate distances from camera
        float targetDist = cameraPosition.distance(targetCenter);
        float occluderDist = cameraPosition.distance(occluderCenter);

        // Target must be further than occluder (with margin)
        return targetDist > occluderDist + OCCLUSION_MARGIN;
    }

    /**
     * Tests if the occluder's shadow covers the target chunk.
     * <p>
     * Projects both bounding boxes onto a plane perpendicular to the view direction
     * and checks if the occluder's projection contains the target's projection.
     * </p>
     *
     * @param occluderBounds Occluder bounding box
     * @param targetBounds Target bounding box
     * @param viewDir View direction (camera to target)
     * @return true if occluder's shadow covers target
     */
    private boolean doesShadowCover(BoundingBox occluderBounds, BoundingBox targetBounds, Vector3f viewDir) {
        // For simplicity, we'll use a 2D projection onto the XZ plane
        // This works well for voxel games where most occlusion happens horizontally

        // This is a simplified approach - a full implementation would project
        // along viewDir, but for chunks this approximation works well

        return occluderBounds.getMinX() <= targetBounds.getMinX() &&
                occluderBounds.getMaxX() >= targetBounds.getMaxX() &&
                occluderBounds.getMinZ() <= targetBounds.getMinZ() &&
                occluderBounds.getMaxZ() >= targetBounds.getMaxZ();
    }

    /**
     * Verifies that the occluder is large enough to occlude the target.
     *
     * @param occluderBounds Occluder bounding box
     * @param targetBounds Target bounding box
     * @return true if size ratio is acceptable
     */
    private boolean meetsMinimumSizeRatio(BoundingBox occluderBounds, BoundingBox targetBounds) {
        float occluderVolume = occluderBounds.getVolume();
        float targetVolume = targetBounds.getVolume();

        // Occluder should be at least MIN_SIZE_RATIO of target's size
        return occluderVolume >= targetVolume * MIN_SIZE_RATIO;
    }

    /**
     * Calculates the view direction from camera to a point.
     *
     * @param point Target point
     * @return Normalized direction vector
     */
    private Vector3f getViewDirection(Vector3f point) {
        return new Vector3f(point).sub(cameraPosition).normalize();
    }

    /**
     * Calculates squared distance from camera to chunk center.
     *
     * @param chunk Chunk to measure
     * @return Squared distance
     */
    private float getSquaredDistanceToCamera(Chunk chunk) {
        BoundingBox bounds = chunk.getBoundingBox();
        Vector3f center = bounds.getCenter();
        return cameraPosition.distanceSquared(center);
    }

    /**
     * Calculates the opacity of a chunk (how solid it is).
     * <p>
     * Returns the ratio of non-air blocks to total blocks.
     * </p>
     *
     * @param chunk Chunk to analyze
     * @return Opacity value from 0.0 (empty) to 1.0 (solid)
     */
    private float calculateChunkOpacity(Chunk chunk) {
        // If chunk is empty, opacity is 0
        if (chunk.isEmpty()) {
            return 0.0f;
        }

        // For now, use a simple heuristic based on whether the chunk has a mesh
        // A more sophisticated implementation would count non-air blocks
        // TODO: Add actual block counting if performance allows

        // Heuristic: chunks with meshes are considered fairly opaque
        // This is conservative but fast
        return chunk.hasMesh() ? 0.9f : 0.0f;
    }

    // ==================== Configuration ====================

    /**
     * Enables or disables occlusion culling.
     *
     * @param enabled true to enable, false to disable
     */
    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            logger.info("Occlusion culling {}", enabled ? "enabled" : "disabled");
        }
    }

    /**
     * Checks if occlusion culling is enabled.
     *
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Enables or disables debug logging.
     *
     * @param debug true to enable debug mode
     */
    public void setDebugMode(boolean debug) {
        this.debugMode = debug;
        logger.info("Occlusion culling debug mode {}", debug ? "enabled" : "disabled");
    }

    // ==================== Statistics ====================

    /**
     * Gets the number of potential occluders in the current frame.
     *
     * @return Number of chunks that can occlude others
     */
    public int getOccluderCount() {
        return potentialOccluders.size();
    }

    /**
     * Gets debug information about the occlusion culler.
     *
     * @return Debug string
     */
    public String getDebugInfo() {
        return String.format("ChunkOcclusionCuller[enabled=%b, occluders=%d, maxDist=%d]",
                enabled, potentialOccluders.size(), MAX_OCCLUDER_DISTANCE);
    }

    @Override
    public String toString() {
        return getDebugInfo();
    }
}
