package org.almostcraft.graphics.culling;

import org.almostcraft.world.chunk.Chunk;
import org.joml.Vector3f;

/**
 * Axis-Aligned Bounding Box (AABB) for spatial culling and collision detection.
 * Immutable class representing a rectangular box aligned with world axes.
 */
public class BoundingBox {

    private final float minX, minY, minZ;
    private final float maxX, maxY, maxZ;

    /**
     * Creates a bounding box with specified bounds.
     *
     * @param minX Minimum X coordinate
     * @param minY Minimum Y coordinate
     * @param minZ Minimum Z coordinate
     * @param maxX Maximum X coordinate
     * @param maxY Maximum Y coordinate
     * @param maxZ Maximum Z coordinate
     */
    public BoundingBox(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        this.minX = Math.min(minX, maxX);
        this.minY = Math.min(minY, maxY);
        this.minZ = Math.min(minZ, maxZ);
        this.maxX = Math.max(minX, maxX);
        this.maxY = Math.max(minY, maxY);
        this.maxZ = Math.max(minZ, maxZ);
    }

    /**
     * Creates a bounding box from a chunk's world coordinates.
     *
     * @param chunk The chunk to create a bounding box for
     * @return BoundingBox encompassing the entire chunk
     */
    public static BoundingBox fromChunk(Chunk chunk) {
        int chunkX = chunk.getChunkX();
        int chunkZ = chunk.getChunkZ();

        float minX = chunkX * 16f;
        float minY = 0f;
        float minZ = chunkZ * 16f;
        float maxX = minX + 16f;
        float maxY = 256f;
        float maxZ = minZ + 16f;

        return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * Creates a bounding box from two corner points.
     *
     * @param corner1 First corner point
     * @param corner2 Opposite corner point
     * @return BoundingBox encompassing both points
     */
    public static BoundingBox fromCorners(Vector3f corner1, Vector3f corner2) {
        return new BoundingBox(
                corner1.x, corner1.y, corner1.z,
                corner2.x, corner2.y, corner2.z
        );
    }

    /**
     * Creates a bounding box centered at a point with given size.
     *
     * @param center Center point
     * @param width Width (X axis)
     * @param height Height (Y axis)
     * @param depth Depth (Z axis)
     * @return BoundingBox centered at the point
     */
    public static BoundingBox fromCenterAndSize(Vector3f center, float width, float height, float depth) {
        float halfWidth = width / 2f;
        float halfHeight = height / 2f;
        float halfDepth = depth / 2f;

        return new BoundingBox(
                center.x - halfWidth, center.y - halfHeight, center.z - halfDepth,
                center.x + halfWidth, center.y + halfHeight, center.z + halfDepth
        );
    }

    // ========== Getters ==========

    public float getMinX() { return minX; }
    public float getMinY() { return minY; }
    public float getMinZ() { return minZ; }
    public float getMaxX() { return maxX; }
    public float getMaxY() { return maxY; }
    public float getMaxZ() { return maxZ; }

    /**
     * @return Minimum corner as a new Vector3f
     */
    public Vector3f getMin() {
        return new Vector3f(minX, minY, minZ);
    }

    /**
     * @return Maximum corner as a new Vector3f
     */
    public Vector3f getMax() {
        return new Vector3f(maxX, maxY, maxZ);
    }

    /**
     * @return Center point of the bounding box
     */
    public Vector3f getCenter() {
        return new Vector3f(
                (minX + maxX) / 2f,
                (minY + maxY) / 2f,
                (minZ + maxZ) / 2f
        );
    }

    /**
     * @return Size of the bounding box (width, height, depth)
     */
    public Vector3f getSize() {
        return new Vector3f(
                maxX - minX,
                maxY - minY,
                maxZ - minZ
        );
    }

    /**
     * @return Width of the bounding box (X axis)
     */
    public float getWidth() {
        return maxX - minX;
    }

    /**
     * @return Height of the bounding box (Y axis)
     */
    public float getHeight() {
        return maxY - minY;
    }

    /**
     * @return Depth of the bounding box (Z axis)
     */
    public float getDepth() {
        return maxZ - minZ;
    }

    // ========== Intersection Tests ==========

    /**
     * Tests if a point is contained within this bounding box.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return true if the point is inside or on the boundary
     */
    public boolean containsPoint(float x, float y, float z) {
        return x >= minX && x <= maxX &&
                y >= minY && y <= maxY &&
                z >= minZ && z <= maxZ;
    }

    /**
     * Tests if a point is contained within this bounding box.
     *
     * @param point Point to test
     * @return true if the point is inside or on the boundary
     */
    public boolean containsPoint(Vector3f point) {
        return containsPoint(point.x, point.y, point.z);
    }

    /**
     * Tests if this bounding box intersects with another.
     * Uses the Separating Axis Theorem (SAT).
     *
     * @param other Other bounding box
     * @return true if the boxes overlap or touch
     */
    public boolean intersects(BoundingBox other) {
        return this.minX <= other.maxX && this.maxX >= other.minX &&
                this.minY <= other.maxY && this.maxY >= other.minY &&
                this.minZ <= other.maxZ && this.maxZ >= other.minZ;
    }

    /**
     * Tests if this bounding box completely contains another.
     *
     * @param other Other bounding box
     * @return true if other is completely inside this box
     */
    public boolean contains(BoundingBox other) {
        return other.minX >= this.minX && other.maxX <= this.maxX &&
                other.minY >= this.minY && other.maxY <= this.maxY &&
                other.minZ >= this.minZ && other.maxZ <= this.maxZ;
    }

    /**
     * Tests if a sphere intersects this bounding box.
     *
     * @param center Center of the sphere
     * @param radius Radius of the sphere
     * @return true if the sphere intersects the box
     */
    public boolean intersectsSphere(Vector3f center, float radius) {
        float distanceSquared = getSquaredDistanceToPoint(center);
        return distanceSquared <= radius * radius;
    }

    /**
     * Calculates the squared distance from a point to the nearest point on this box.
     * If the point is inside, returns 0.
     *
     * @param point Point to test
     * @return Squared distance to the box
     */
    public float getSquaredDistanceToPoint(Vector3f point) {
        float dx = Math.max(0, Math.max(minX - point.x, point.x - maxX));
        float dy = Math.max(0, Math.max(minY - point.y, point.y - maxY));
        float dz = Math.max(0, Math.max(minZ - point.z, point.z - maxZ));
        return dx * dx + dy * dy + dz * dz;
    }

    // ========== Transformation Methods ==========

    /**
     * Creates a new bounding box expanded by the given amount in all directions.
     *
     * @param amount Amount to expand (can be negative to shrink)
     * @return New expanded bounding box
     */
    public BoundingBox expand(float amount) {
        return new BoundingBox(
                minX - amount, minY - amount, minZ - amount,
                maxX + amount, maxY + amount, maxZ + amount
        );
    }

    /**
     * Creates a new bounding box expanded by different amounts per axis.
     *
     * @param x Amount to expand on X axis
     * @param y Amount to expand on Y axis
     * @param z Amount to expand on Z axis
     * @return New expanded bounding box
     */
    public BoundingBox expand(float x, float y, float z) {
        return new BoundingBox(
                minX - x, minY - y, minZ - z,
                maxX + x, maxY + y, maxZ + z
        );
    }

    /**
     * Creates a new bounding box translated by the given offset.
     *
     * @param dx X offset
     * @param dy Y offset
     * @param dz Z offset
     * @return New translated bounding box
     */
    public BoundingBox translate(float dx, float dy, float dz) {
        return new BoundingBox(
                minX + dx, minY + dy, minZ + dz,
                maxX + dx, maxY + dy, maxZ + dz
        );
    }

    /**
     * Creates a new bounding box translated by the given offset.
     *
     * @param offset Translation vector
     * @return New translated bounding box
     */
    public BoundingBox translate(Vector3f offset) {
        return translate(offset.x, offset.y, offset.z);
    }

    /**
     * Creates the union (smallest box containing both) of this box and another.
     *
     * @param other Other bounding box
     * @return New bounding box containing both
     */
    public BoundingBox union(BoundingBox other) {
        return new BoundingBox(
                Math.min(this.minX, other.minX),
                Math.min(this.minY, other.minY),
                Math.min(this.minZ, other.minZ),
                Math.max(this.maxX, other.maxX),
                Math.max(this.maxY, other.maxY),
                Math.max(this.maxZ, other.maxZ)
        );
    }

    /**
     * Creates the intersection of this box and another.
     * Returns null if the boxes don't intersect.
     *
     * @param other Other bounding box
     * @return Intersection bounding box, or null if no intersection
     */
    public BoundingBox intersection(BoundingBox other) {
        if (!this.intersects(other)) {
            return null;
        }

        return new BoundingBox(
                Math.max(this.minX, other.minX),
                Math.max(this.minY, other.minY),
                Math.max(this.minZ, other.minZ),
                Math.min(this.maxX, other.maxX),
                Math.min(this.maxY, other.maxY),
                Math.min(this.maxZ, other.maxZ)
        );
    }

    // ========== Utility Methods ==========

    /**
     * @return Volume of the bounding box
     */
    public float getVolume() {
        return getWidth() * getHeight() * getDepth();
    }

    /**
     * @return Surface area of the bounding box
     */
    public float getSurfaceArea() {
        float w = getWidth();
        float h = getHeight();
        float d = getDepth();
        return 2f * (w * h + w * d + h * d);
    }

    /**
     * Gets the corner point at the specified indices.
     *
     * @param xIndex 0 for min, 1 for max
     * @param yIndex 0 for min, 1 for max
     * @param zIndex 0 for min, 1 for max
     * @return Corner point
     */
    public Vector3f getCorner(int xIndex, int yIndex, int zIndex) {
        return new Vector3f(
                xIndex == 0 ? minX : maxX,
                yIndex == 0 ? minY : maxY,
                zIndex == 0 ? minZ : maxZ
        );
    }

    /**
     * Gets all 8 corner points of the bounding box.
     * Order: [minX minY minZ], [maxX minY minZ], [minX maxY minZ], [maxX maxY minZ],
     *        [minX minY maxZ], [maxX minY maxZ], [minX maxY maxZ], [maxX maxY maxZ]
     *
     * @return Array of 8 corner points
     */
    public Vector3f[] getCorners() {
        return new Vector3f[] {
                new Vector3f(minX, minY, minZ),
                new Vector3f(maxX, minY, minZ),
                new Vector3f(minX, maxY, minZ),
                new Vector3f(maxX, maxY, minZ),
                new Vector3f(minX, minY, maxZ),
                new Vector3f(maxX, minY, maxZ),
                new Vector3f(minX, maxY, maxZ),
                new Vector3f(maxX, maxY, maxZ)
        };
    }

    @Override
    public String toString() {
        return String.format("BoundingBox[min=(%.2f, %.2f, %.2f), max=(%.2f, %.2f, %.2f)]",
                minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        BoundingBox other = (BoundingBox) obj;
        return Float.compare(minX, other.minX) == 0 &&
                Float.compare(minY, other.minY) == 0 &&
                Float.compare(minZ, other.minZ) == 0 &&
                Float.compare(maxX, other.maxX) == 0 &&
                Float.compare(maxY, other.maxY) == 0 &&
                Float.compare(maxZ, other.maxZ) == 0;
    }

    @Override
    public int hashCode() {
        int result = Float.floatToIntBits(minX);
        result = 31 * result + Float.floatToIntBits(minY);
        result = 31 * result + Float.floatToIntBits(minZ);
        result = 31 * result + Float.floatToIntBits(maxX);
        result = 31 * result + Float.floatToIntBits(maxY);
        result = 31 * result + Float.floatToIntBits(maxZ);
        return result;
    }
}
