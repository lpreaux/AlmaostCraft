package org.almostcraft.render.chunk;

import org.almostcraft.camera.Camera;
import org.almostcraft.render.core.Mesh;
import org.almostcraft.render.core.Shader;
import org.almostcraft.render.texture.TextureArray;
import org.almostcraft.world.ChunkCoordinate;
import org.almostcraft.world.World;
import org.almostcraft.world.block.BlockRegistry;
import org.almostcraft.world.chunk.Chunk;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.lwjgl.opengl.GL13.GL_TEXTURE0;

/**
 * Gestionnaire de rendu pour tous les chunks du monde avec TextureArray.
 *
 * @author Lucas Préaux
 * @version 3.0 (avec TextureArray)
 */
public class ChunkRenderer {

    private static final Logger logger = LoggerFactory.getLogger(ChunkRenderer.class);

    // ==================== Attributs ====================

    private final World world;
    private final BlockRegistry blockRegistry;
    private final Shader shader;
    private final TextureArray textureArray;

    private final Map<ChunkCoordinate, Mesh> meshCache;
    private final Set<ChunkCoordinate> dirtyChunks;

    private int totalMeshesGenerated;

    // ==================== Constructeur ====================

    public ChunkRenderer(World world, BlockRegistry blockRegistry, Shader shader, TextureArray textureArray) {
        if (world == null) {
            throw new IllegalArgumentException("World cannot be null");
        }
        if (blockRegistry == null) {
            throw new IllegalArgumentException("BlockRegistry cannot be null");
        }
        if (shader == null) {
            throw new IllegalArgumentException("Shader cannot be null");
        }
        if (textureArray == null) {
            throw new IllegalArgumentException("TextureArray cannot be null");
        }

        this.world = world;
        this.blockRegistry = blockRegistry;
        this.shader = shader;
        this.textureArray = textureArray;
        this.meshCache = new HashMap<>();
        this.dirtyChunks = new HashSet<>();
        this.totalMeshesGenerated = 0;

        logger.info("ChunkRenderer created with TextureArray");
    }

    // ==================== Mise à jour ====================

    private static final int MAX_MESHES_PER_FRAME = 4;

    public void update() {
        cleanupUnloadedChunks();
        detectModifiedChunks();
        generatePendingMeshes();
    }

    private void generatePendingMeshes() {
        int generated = 0;

        Iterator<ChunkCoordinate> iterator = dirtyChunks.iterator();
        while (iterator.hasNext() && generated < MAX_MESHES_PER_FRAME) {
            ChunkCoordinate coord = iterator.next();

            if (!world.hasChunk(coord.x(), coord.z())) {
                iterator.remove();
                continue;
            }

            Chunk chunk = world.getChunk(coord.x(), coord.z());

            if (!chunk.isGenerated() || chunk.isEmpty()) {
                iterator.remove();
                continue;
            }

            logger.debug("Generating mesh for chunk ({}, {})", coord.x(), coord.z());

            Mesh oldMesh = meshCache.get(coord);
            if (oldMesh != null) {
                oldMesh.cleanup();
            }

            ChunkMesh chunkMeshBuilder = new ChunkMesh(
                    chunk, world, blockRegistry, textureArray
            );
            Mesh mesh = chunkMeshBuilder.build();

            meshCache.put(coord, mesh);
            totalMeshesGenerated++;
            generated++;

            iterator.remove();
        }

        if (generated > 0) {
            logger.debug("Generated {} meshes this frame ({} remaining)",
                    generated, dirtyChunks.size());
        }
    }

    private void cleanupUnloadedChunks() {
        Set<ChunkCoordinate> toRemove = new HashSet<>();

        for (ChunkCoordinate coord : meshCache.keySet()) {
            if (!world.hasChunk(coord.x(), coord.z())) {
                toRemove.add(coord);
            }
        }

        for (ChunkCoordinate coord : toRemove) {
            Mesh mesh = meshCache.remove(coord);
            if (mesh != null) {
                mesh.cleanup();
                logger.debug("Cleaned up mesh for unloaded chunk ({}, {})", coord.x(), coord.z());
            }
        }

        if (!toRemove.isEmpty()) {
            logger.debug("Removed {} meshes for unloaded chunks", toRemove.size());
        }
    }

    private void detectModifiedChunks() {
        for (Chunk chunk : world.getLoadedChunks()) {
            if (chunk.isModified()) {
                ChunkCoordinate coord = new ChunkCoordinate(
                        chunk.getChunkX(),
                        chunk.getChunkZ()
                );
                dirtyChunks.add(coord);
                chunk.clearModified();
            }
        }
    }

    // ==================== Rendu ====================

    public void render(Camera camera) {
        shader.bind();
        textureArray.bind(GL_TEXTURE0);
        shader.setUniform("uTextureArray", 0);

        for (Chunk chunk : world.getLoadedChunks()) {
            ChunkCoordinate coord = new ChunkCoordinate(
                    chunk.getChunkX(),
                    chunk.getChunkZ()
            );

            Mesh mesh = getOrCreateMesh(coord, chunk);

            if (mesh == null) {
                continue;
            }

            Matrix4f modelMatrix = new Matrix4f().identity();
            Matrix4f mvp = new Matrix4f(camera.getProjectionMatrix())
                    .mul(camera.getViewMatrix())
                    .mul(modelMatrix);

            shader.setUniform("uMVP", mvp);

            mesh.render();
        }

        shader.unbind();
    }

    private Mesh getOrCreateMesh(ChunkCoordinate coord, Chunk chunk) {
        if (!chunk.isGenerated()) {
            return null;
        }

        if (dirtyChunks.contains(coord)) {
            Mesh oldMesh = meshCache.remove(coord);
            if (oldMesh != null) {
                oldMesh.cleanup();
            }
            dirtyChunks.remove(coord);
        }

        Mesh mesh = meshCache.get(coord);
        if (mesh != null) {
            return mesh;
        }

        if (chunk.isEmpty()) {
            return null;
        }

        logger.debug("Generating mesh for chunk ({}, {})", coord.x(), coord.z());

        ChunkMesh chunkMeshBuilder = new ChunkMesh(
                chunk, world, blockRegistry, textureArray
        );
        mesh = chunkMeshBuilder.build();

        meshCache.put(coord, mesh);
        totalMeshesGenerated++;

        return mesh;
    }

    // ==================== Utilitaires ====================

    public void markChunkDirty(int chunkX, int chunkZ) {
        dirtyChunks.add(new ChunkCoordinate(chunkX, chunkZ));
        logger.trace("Chunk ({}, {}) marked dirty", chunkX, chunkZ);
    }

    public void regenerateChunk(int chunkX, int chunkZ) {
        ChunkCoordinate coord = new ChunkCoordinate(chunkX, chunkZ);

        Mesh oldMesh = meshCache.remove(coord);
        if (oldMesh != null) {
            oldMesh.cleanup();
        }

        dirtyChunks.add(coord);

        logger.debug("Forced regeneration of chunk ({}, {})", chunkX, chunkZ);
    }

    public void onChunkLoaded(int chunkX, int chunkZ) {
        markChunkDirty(chunkX - 1, chunkZ);
        markChunkDirty(chunkX + 1, chunkZ);
        markChunkDirty(chunkX, chunkZ - 1);
        markChunkDirty(chunkX, chunkZ + 1);

        logger.info("Marked neighbors of chunk ({}, {}) as dirty", chunkX, chunkZ);
    }

    // ==================== Nettoyage ====================

    public void cleanup() {
        for (Mesh mesh : meshCache.values()) {
            mesh.cleanup();
        }
        meshCache.clear();
    }

    // ==================== Getters ====================

    public int getCachedMeshCount() {
        return meshCache.size();
    }

    public int getTotalMeshesGenerated() {
        return totalMeshesGenerated;
    }

    public int getDirtyChunkCount() {
        return dirtyChunks.size();
    }

    public int getTotalTriangleCount() {
        return meshCache.values().stream()
                .mapToInt(Mesh::getTriangleCount)
                .sum();
    }
}
