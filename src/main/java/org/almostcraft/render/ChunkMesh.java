package org.almostcraft.render;

import org.almostcraft.world.World;
import org.almostcraft.world.block.BlockRegistry;
import org.almostcraft.world.block.BlockType;
import org.almostcraft.world.chunk.Chunk;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Générateur de mesh pour un chunk avec Greedy Meshing et TextureArray.
 *
 * @author Lucas Préaux
 * @version 5.0 (avec TextureArray)
 */
public class ChunkMesh {

    private static final Logger logger = LoggerFactory.getLogger(ChunkMesh.class);

    // ==================== Constantes ====================

    private static final float BLOCK_SIZE = 1.0f;

    // ==================== Attributs ====================

    private final Chunk chunk;
    private final World world;
    private final BlockRegistry blockRegistry;
    private final TextureArray textureArray;

    private final MeshData meshData;

    // Classe interne pour stocker les données de mesh
    private static class MeshData {
        List<Float> vertices = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        int vertexCount = 0;
    }

    // ==================== Constructeur ====================

    public ChunkMesh(Chunk chunk, World world, BlockRegistry blockRegistry, TextureArray textureArray) {
        if (chunk == null) {
            throw new IllegalArgumentException("Chunk cannot be null");
        }
        if (world == null) {
            throw new IllegalArgumentException("World cannot be null");
        }
        if (blockRegistry == null) {
            throw new IllegalArgumentException("BlockRegistry cannot be null");
        }
        if (textureArray == null) {
            throw new IllegalArgumentException("TextureArray cannot be null");
        }

        this.chunk = chunk;
        this.world = world;
        this.blockRegistry = blockRegistry;
        this.textureArray = textureArray;
        this.meshData = new MeshData();
    }

    // ==================== Génération du mesh ====================

    public Mesh build() {
        long startTime = System.nanoTime();

        logger.debug("Building mesh with texture array for chunk ({}, {})",
                chunk.getChunkX(), chunk.getChunkZ());

        meshData.vertices.clear();
        meshData.indices.clear();
        meshData.vertexCount = 0;

        for (FaceDirection direction : FaceDirection.values()) {
            greedyMeshDirection(direction);
        }

        float[] vertexArray = toFloatArray(meshData.vertices);
        int[] indexArray = toIntArray(meshData.indices);

        Mesh mesh = new Mesh();
        mesh.uploadData(vertexArray, indexArray);

        long endTime = System.nanoTime();
        double duration = (endTime - startTime) / 1_000_000.0;

        logger.info("Mesh built with array: {} triangles, {:.2f}ms",
                mesh.getTriangleCount(), duration);

        return mesh;
    }

    // ==================== Greedy Meshing par direction ====================

    private void greedyMeshDirection(FaceDirection direction) {
        int width = getMaskWidth(direction);
        int height = getMaskHeight(direction);
        int depth = getMaskDepth(direction);

        MaskEntry[][] mask = new MaskEntry[width][height];

        for (int d = 0; d < depth; d++) {
            clearMask(mask);
            fillMask(mask, direction, d);
            greedyMesh(mask, direction, d);
        }
    }

    private void greedyMesh(MaskEntry[][] mask, FaceDirection direction, int depth) {
        int width = mask.length;
        int height = mask[0].length;

        boolean[][] consumed = new boolean[width][height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (mask[x][y] == null || consumed[x][y]) {
                    continue;
                }

                MaskEntry entry = mask[x][y];

                int w = computeWidth(mask, consumed, x, y, entry);
                int h = computeHeight(mask, consumed, x, y, w, entry);

                generateGreedyQuad(x, y, w, h, depth, direction, entry);

                markConsumed(consumed, x, y, w, h);
            }
        }
    }

    // ==================== Calcul des dimensions du masque ====================

    private int getMaskWidth(FaceDirection direction) {
        return switch (direction) {
            case NORTH, SOUTH -> Chunk.WIDTH;
            case EAST, WEST -> Chunk.DEPTH;
            case UP, DOWN -> Chunk.WIDTH;
        };
    }

    private int getMaskHeight(FaceDirection direction) {
        return switch (direction) {
            case NORTH, SOUTH, EAST, WEST -> Chunk.HEIGHT;
            case UP, DOWN -> Chunk.DEPTH;
        };
    }

    private int getMaskDepth(FaceDirection direction) {
        return switch (direction) {
            case NORTH, SOUTH -> Chunk.DEPTH;
            case EAST, WEST -> Chunk.WIDTH;
            case UP, DOWN -> Chunk.HEIGHT;
        };
    }

    // ==================== Remplissage du masque ====================

    private void clearMask(MaskEntry[][] mask) {
        for (int x = 0; x < mask.length; x++) {
            for (int y = 0; y < mask[0].length; y++) {
                mask[x][y] = null;
            }
        }
    }

    private void fillMask(MaskEntry[][] mask, FaceDirection direction, int depth) {
        int width = mask.length;
        int height = mask[0].length;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int[] localCoords = maskToChunkCoords(x, y, depth, direction);
                int localX = localCoords[0];
                int localY = localCoords[1];
                int localZ = localCoords[2];

                if (localX < 0 || localX >= Chunk.WIDTH ||
                        localY < 0 || localY >= Chunk.HEIGHT ||
                        localZ < 0 || localZ >= Chunk.DEPTH) {
                    continue;
                }

                int blockId = chunk.getVoxel(localX, localY, localZ);

                if (blockId == 0) {
                    continue;
                }

                BlockType blockType = blockRegistry.getBlockByNumericId(blockId);
                if (blockType == null || !blockType.isSolid()) {
                    continue;
                }

                int worldX = chunk.getChunkX() * Chunk.WIDTH + localX;
                int worldY = localY;
                int worldZ = chunk.getChunkZ() * Chunk.DEPTH + localZ;

                int neighborX = worldX + direction.getOffsetX();
                int neighborY = worldY + direction.getOffsetY();
                int neighborZ = worldZ + direction.getOffsetZ();

                if (!world.isBlockOccluding(neighborX, neighborY, neighborZ)) {
                    BlockType.BlockFace blockFace = directionToBlockFace(direction);
                    String texturePath = blockType.getTexturePath(blockFace);
                    int textureIndex = textureArray.getTextureIndex(texturePath);

                    if (textureIndex == -1) {
                        logger.warn("Texture '{}' not found in array", texturePath);
                        continue;
                    }

                    Vector3f tintColor = getTintColor(blockType, worldX, worldY, worldZ, blockFace);

                    mask[x][y] = new MaskEntry(blockType, textureIndex, tintColor, blockFace);
                }
            }
        }
    }

    private BlockType.BlockFace directionToBlockFace(FaceDirection direction) {
        return switch (direction) {
            case UP -> BlockType.BlockFace.TOP;
            case DOWN -> BlockType.BlockFace.BOTTOM;
            case NORTH -> BlockType.BlockFace.NORTH;
            case SOUTH -> BlockType.BlockFace.SOUTH;
            case EAST -> BlockType.BlockFace.EAST;
            case WEST -> BlockType.BlockFace.WEST;
        };
    }

    private Vector3f getTintColor(BlockType blockType, int worldX, int worldY, int worldZ, BlockType.BlockFace blockFace) {
        if (!blockType.isTinted()) {
            return new Vector3f(1.0f, 1.0f, 1.0f);
        }

        if (blockType.id().equals("almostcraft:grass_block") && blockFace == BlockType.BlockFace.TOP) {
            return getGrassColor(worldX, worldZ);
        }

        return new Vector3f(1.0f, 1.0f, 1.0f);
    }

    private Vector3f getGrassColor(int worldX, int worldZ) {
        return new Vector3f(0.55f, 0.88f, 0.31f);
    }

    private int[] maskToChunkCoords(int maskX, int maskY, int depth, FaceDirection direction) {
        return switch (direction) {
            case NORTH -> new int[]{maskX, maskY, depth};
            case SOUTH -> new int[]{maskX, maskY, depth};
            case WEST -> new int[]{depth, maskY, maskX};
            case EAST -> new int[]{depth, maskY, maskX};
            case DOWN -> new int[]{maskX, depth, maskY};
            case UP -> new int[]{maskX, depth, maskY};
        };
    }

    // ==================== Expansion greedy ====================

    private int computeWidth(MaskEntry[][] mask, boolean[][] consumed,
                             int startX, int startY, MaskEntry entry) {
        int width = 1;
        int maxWidth = mask.length;

        while (startX + width < maxWidth &&
                !consumed[startX + width][startY] &&
                entry.canMergeWith(mask[startX + width][startY])) {
            width++;
        }

        return width;
    }

    private int computeHeight(MaskEntry[][] mask, boolean[][] consumed,
                              int startX, int startY, int width, MaskEntry entry) {
        int height = 1;
        int maxHeight = mask[0].length;

        outer:
        while (startY + height < maxHeight) {
            for (int x = startX; x < startX + width; x++) {
                if (consumed[x][startY + height] ||
                        !entry.canMergeWith(mask[x][startY + height])) {
                    break outer;
                }
            }
            height++;
        }

        return height;
    }

    private void markConsumed(boolean[][] consumed, int startX, int startY, int width, int height) {
        for (int x = startX; x < startX + width; x++) {
            for (int y = startY; y < startY + height; y++) {
                consumed[x][y] = true;
            }
        }
    }

    // ==================== Génération du quad greedy ====================

    private void generateGreedyQuad(int maskX, int maskY, int width, int height,
                                    int depth, FaceDirection direction, MaskEntry entry) {
        int[] startCoords = maskToChunkCoords(maskX, maskY, depth, direction);
        int localX = startCoords[0];
        int localY = startCoords[1];
        int localZ = startCoords[2];

        float worldX = chunk.getChunkX() * Chunk.WIDTH + localX;
        float worldY = localY;
        float worldZ = chunk.getChunkZ() * Chunk.DEPTH + localZ;

        Vector3f[] quadVertices = getGreedyQuadVertices(
                worldX, worldY, worldZ, width, height, direction
        );

        // Coordonnées UV simples avec répétition
        float[][] uvCoords;
        if (direction == FaceDirection.UP || direction == FaceDirection.DOWN) {
            uvCoords = new float[][]{
                    {0, 0},
                    {0, height},
                    {width, height},
                    {width, 0}
            };
        } else {
            uvCoords = new float[][]{
                    {0, height},
                    {0, 0},
                    {width, 0},
                    {width, height}
            };
        }

        int startIndex = meshData.vertexCount;
        Vector3f tintColor = entry.tintColor;
        float textureIndex = entry.textureIndex;

        // Ajouter les vertices (position, textureIndex, texCoord, tintColor)
        for (int i = 0; i < 4; i++) {
            Vector3f vertex = quadVertices[i];
            float[] uv = uvCoords[i];

            // Position (3 floats)
            meshData.vertices.add(vertex.x);
            meshData.vertices.add(vertex.y);
            meshData.vertices.add(vertex.z);

            // Texture index (1 float)
            meshData.vertices.add(textureIndex);

            // UV coordinates (2 floats)
            meshData.vertices.add(uv[0]);
            meshData.vertices.add(uv[1]);

            // Tint color (3 floats)
            meshData.vertices.add(tintColor.x);
            meshData.vertices.add(tintColor.y);
            meshData.vertices.add(tintColor.z);

            meshData.vertexCount++;
        }

        // Ajouter les indices
        meshData.indices.add(startIndex);
        meshData.indices.add(startIndex + 1);
        meshData.indices.add(startIndex + 2);

        meshData.indices.add(startIndex + 2);
        meshData.indices.add(startIndex + 3);
        meshData.indices.add(startIndex);
    }

    private Vector3f[] getGreedyQuadVertices(float x, float y, float z, int width, int height, FaceDirection direction) {
        float w = width * BLOCK_SIZE;
        float h = height * BLOCK_SIZE;

        return switch (direction) {
            case NORTH -> new Vector3f[]{
                    new Vector3f(x, y, z),
                    new Vector3f(x, y + h, z),
                    new Vector3f(x + w, y + h, z),
                    new Vector3f(x + w, y, z)
            };

            case SOUTH -> new Vector3f[]{
                    new Vector3f(x + w, y, z + BLOCK_SIZE),
                    new Vector3f(x + w, y + h, z + BLOCK_SIZE),
                    new Vector3f(x, y + h, z + BLOCK_SIZE),
                    new Vector3f(x, y, z + BLOCK_SIZE)
            };

            case WEST -> new Vector3f[]{
                    new Vector3f(x, y, z + w),
                    new Vector3f(x, y + h, z + w),
                    new Vector3f(x, y + h, z),
                    new Vector3f(x, y, z)
            };

            case EAST -> new Vector3f[]{
                    new Vector3f(x + BLOCK_SIZE, y, z),
                    new Vector3f(x + BLOCK_SIZE, y + h, z),
                    new Vector3f(x + BLOCK_SIZE, y + h, z + w),
                    new Vector3f(x + BLOCK_SIZE, y, z + w)
            };

            case DOWN -> new Vector3f[]{
                    new Vector3f(x, y, z),
                    new Vector3f(x + w, y, z),
                    new Vector3f(x + w, y, z + h),
                    new Vector3f(x, y, z + h)
            };

            case UP -> new Vector3f[]{
                    new Vector3f(x, y + BLOCK_SIZE, z),
                    new Vector3f(x, y + BLOCK_SIZE, z + h),
                    new Vector3f(x + w, y + BLOCK_SIZE, z + h),
                    new Vector3f(x + w, y + BLOCK_SIZE, z)
            };
        };
    }

    // ==================== Utilitaires ====================

    private float[] toFloatArray(List<Float> list) {
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    private int[] toIntArray(List<Integer> list) {
        int[] array = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    // ==================== Classe interne MaskEntry ====================

    private static class MaskEntry {
        final BlockType blockType;
        final int textureIndex;
        final Vector3f tintColor;
        final BlockType.BlockFace blockFace;

        MaskEntry(BlockType blockType, int textureIndex,
                  Vector3f tintColor, BlockType.BlockFace blockFace) {
            this.blockType = blockType;
            this.textureIndex = textureIndex;
            this.tintColor = tintColor;
            this.blockFace = blockFace;
        }

        boolean canMergeWith(MaskEntry other) {
            if (other == null) return false;
            return this.blockType.id().equals(other.blockType.id())
                    && this.blockFace == other.blockFace
                    && this.textureIndex == other.textureIndex;
        }
    }

    // ==================== Enum FaceDirection ====================

    private enum FaceDirection {
        NORTH(0, 0, -1),
        SOUTH(0, 0, 1),
        WEST(-1, 0, 0),
        EAST(1, 0, 0),
        DOWN(0, -1, 0),
        UP(0, 1, 0);

        private final int offsetX;
        private final int offsetY;
        private final int offsetZ;

        FaceDirection(int offsetX, int offsetY, int offsetZ) {
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.offsetZ = offsetZ;
        }

        public int getOffsetX() { return offsetX; }
        public int getOffsetY() { return offsetY; }
        public int getOffsetZ() { return offsetZ; }
    }
}
