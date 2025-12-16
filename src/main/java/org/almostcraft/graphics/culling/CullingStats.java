package org.almostcraft.graphics.culling;

/**
 * Statistics tracker for chunk culling operations.
 * Provides detailed metrics about why chunks are not being rendered.
 */
public class CullingStats {

    // Chunk counts by culling stage
    private int totalChunksLoaded;      // Total chunks in memory
    private int distanceCulled;         // Culled by distance from camera
    private int frustumCulled;          // Culled by frustum (outside view)
    private int occlusionCulled;        // Culled by occlusion (hidden behind others)
    private int rendered;               // Actually rendered

    // Additional metrics
    private int emptyChunks;            // Chunks with no visible blocks
    private int pendingMeshGeneration;  // Chunks waiting for mesh rebuild

    // Performance timing (optional)
    private long cullingTimeNanos;      // Time spent on culling calculations
    private long renderTimeNanos;       // Time spent on actual rendering

    /**
     * Creates a new statistics tracker with all counters at zero.
     */
    public CullingStats() {
        reset();
    }

    /**
     * Resets all statistics to zero.
     * Should be called at the start of each frame.
     */
    public void reset() {
        totalChunksLoaded = 0;
        distanceCulled = 0;
        frustumCulled = 0;
        occlusionCulled = 0;
        rendered = 0;
        emptyChunks = 0;
        pendingMeshGeneration = 0;
        cullingTimeNanos = 0;
        renderTimeNanos = 0;
    }

    // ========== Increment Methods ==========

    public void incrementTotalLoaded() {
        totalChunksLoaded++;
    }

    public void incrementDistanceCulled() {
        distanceCulled++;
    }

    public void incrementFrustumCulled() {
        frustumCulled++;
    }

    public void incrementOcclusionCulled() {
        occlusionCulled++;
    }

    public void incrementRendered() {
        rendered++;
    }

    public void incrementEmptyChunks() {
        emptyChunks++;
    }

    public void incrementPendingMeshGeneration() {
        pendingMeshGeneration++;
    }

    // ========== Setters for Direct Assignment ==========

    public void setTotalChunksLoaded(int count) {
        this.totalChunksLoaded = count;
    }

    public void addCullingTime(long nanos) {
        this.cullingTimeNanos += nanos;
    }

    public void addRenderTime(long nanos) {
        this.renderTimeNanos += nanos;
    }

    // ========== Getters ==========

    public int getTotalChunksLoaded() {
        return totalChunksLoaded;
    }

    public int getDistanceCulled() {
        return distanceCulled;
    }

    public int getFrustumCulled() {
        return frustumCulled;
    }

    public int getOcclusionCulled() {
        return occlusionCulled;
    }

    public int getRendered() {
        return rendered;
    }

    public int getEmptyChunks() {
        return emptyChunks;
    }

    public int getPendingMeshGeneration() {
        return pendingMeshGeneration;
    }

    public long getCullingTimeNanos() {
        return cullingTimeNanos;
    }

    public long getRenderTimeNanos() {
        return renderTimeNanos;
    }

    // ========== Calculated Metrics ==========

    /**
     * @return Total number of chunks culled by any method
     */
    public int getTotalCulled() {
        return distanceCulled + frustumCulled + occlusionCulled + emptyChunks + pendingMeshGeneration;
    }

    /**
     * @return Percentage of loaded chunks that were rendered
     */
    public float getRenderPercentage() {
        if (totalChunksLoaded == 0) {
            return 0f;
        }
        return (rendered * 100f) / totalChunksLoaded;
    }

    /**
     * @return Percentage of loaded chunks that were culled
     */
    public float getCullPercentage() {
        if (totalChunksLoaded == 0) {
            return 0f;
        }
        return (getTotalCulled() * 100f) / totalChunksLoaded;
    }

    /**
     * @return Average culling time per chunk in microseconds
     */
    public float getAverageCullingTimeMicros() {
        if (totalChunksLoaded == 0) {
            return 0f;
        }
        return (cullingTimeNanos / 1000f) / totalChunksLoaded;
    }

    /**
     * @return Average render time per rendered chunk in microseconds
     */
    public float getAverageRenderTimeMicros() {
        if (rendered == 0) {
            return 0f;
        }
        return (renderTimeNanos / 1000f) / rendered;
    }

    // ========== Summary Generation ==========

    /**
     * Generates a compact single-line summary for HUD display.
     * Format: "Chunks: 123/456 rendered (27%) | Culled: D:89 F:234 O:10"
     *
     * @return Compact summary string
     */
    public String getCompactSummary() {
        return String.format("Chunks: %d/%d rendered (%.1f%%) | Culled: D:%d F:%d O:%d E:%d",
                rendered, totalChunksLoaded, getRenderPercentage(),
                distanceCulled, frustumCulled, occlusionCulled, emptyChunks);
    }

    /**
     * Generates a detailed multi-line summary for debug overlay.
     *
     * @return Multi-line detailed summary
     */
    public String getDetailedSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Chunk Culling Statistics ===\n");
        sb.append(String.format("Total Loaded:      %d\n", totalChunksLoaded));
        sb.append(String.format("Rendered:          %d (%.1f%%)\n", rendered, getRenderPercentage()));
        sb.append(String.format("Total Culled:      %d (%.1f%%)\n", getTotalCulled(), getCullPercentage()));
        sb.append("\n--- Culling Breakdown ---\n");
        sb.append(String.format("Distance Culled:   %d\n", distanceCulled));
        sb.append(String.format("Frustum Culled:    %d\n", frustumCulled));
        sb.append(String.format("Occlusion Culled:  %d\n", occlusionCulled));
        sb.append(String.format("Empty Chunks:      %d\n", emptyChunks));
        sb.append(String.format("Pending Mesh Gen:  %d\n", pendingMeshGeneration));

        if (cullingTimeNanos > 0 || renderTimeNanos > 0) {
            sb.append("\n--- Performance ---\n");
            sb.append(String.format("Culling Time:      %.2f ms (%.2f µs/chunk)\n",
                    cullingTimeNanos / 1_000_000f, getAverageCullingTimeMicros()));
            sb.append(String.format("Render Time:       %.2f ms (%.2f µs/chunk)\n",
                    renderTimeNanos / 1_000_000f, getAverageRenderTimeMicros()));
        }

        return sb.toString();
    }

    /**
     * Generates a summary suitable for logging.
     *
     * @return Log-friendly summary string
     */
    public String getLogSummary() {
        return String.format(
                "Culling[loaded=%d, rendered=%d (%.1f%%), distance=%d, frustum=%d, occlusion=%d, empty=%d]",
                totalChunksLoaded, rendered, getRenderPercentage(),
                distanceCulled, frustumCulled, occlusionCulled, emptyChunks
        );
    }

    /**
     * Generates a summary for the F3 debug screen.
     *
     * @return F3-style summary with key metrics
     */
    public String getF3Summary() {
        return String.format(
                "C: %d/%d D: %d, E: %d, F: %d, O: %d",
                rendered, totalChunksLoaded,
                distanceCulled, emptyChunks, frustumCulled, occlusionCulled
        );
    }

    // ========== Efficiency Analysis ==========

    /**
     * Analyzes culling efficiency and provides recommendations.
     *
     * @return Analysis string with potential optimization suggestions
     */
    public String getEfficiencyAnalysis() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Culling Efficiency Analysis ===\n");

        float cullRate = getCullPercentage();
        if (cullRate < 50f) {
            sb.append("⚠ LOW CULLING RATE: Consider increasing render distance or implementing better culling.\n");
        } else if (cullRate > 90f) {
            sb.append("✓ EXCELLENT CULLING: Most chunks are being efficiently culled.\n");
        } else {
            sb.append("✓ GOOD CULLING: Culling is working effectively.\n");
        }

        if (emptyChunks > totalChunksLoaded * 0.3f) {
            sb.append("⚠ MANY EMPTY CHUNKS: Consider not loading/meshing empty chunks.\n");
        }

        if (pendingMeshGeneration > rendered) {
            sb.append("⚠ MESH GENERATION LAG: Mesh generation cannot keep up with camera movement.\n");
        }

        if (frustumCulled < distanceCulled * 0.5f) {
            sb.append("ℹ INFO: Distance culling is more effective than frustum culling.\n");
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return getCompactSummary();
    }
}
