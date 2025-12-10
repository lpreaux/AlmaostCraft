package org.almostcraft.render;

import org.almostcraft.camera.Camera;
import org.almostcraft.world.ChunkCoordinate;
import org.almostcraft.world.World;
import org.almostcraft.world.block.BlockRegistry;
import org.almostcraft.world.chunk.Chunk;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Gestionnaire de rendu pour tous les chunks du monde.
 * <p>
 * Cette classe maintient un cache des meshes générés et coordonne
 * le rendu de tous les chunks visibles.
 * </p>
 * <p>
 * <strong>Version actuelle : Naïve</strong>
 * <ul>
 *   <li>Pas de frustum culling (rend tous les chunks chargés)</li>
 *   <li>Régénération simple des chunks modifiés</li>
 *   <li>Cache basique (Map)</li>
 * </ul>
 * Optimisations futures :
 * <ul>
 *   <li>Frustum culling : Ne rendre que les chunks visibles</li>
 *   <li>Occlusion culling : Ne pas rendre les chunks cachés</li>
 *   <li>Génération asynchrone des meshes (multithreading)</li>
 *   <li>Distance-based LOD</li>
 * </ul>
 * </p>
 * <p>
 * Utilisation typique :
 * <pre>{@code
 * ChunkRenderer renderer = new ChunkRenderer(world, blockRegistry, shader);
 *
 * // Dans la boucle de rendu
 * renderer.update();
 * renderer.render(camera);
 *
 * // Nettoyage
 * renderer.cleanup();
 * }</pre>
 * </p>
 *
 * @author Lucas Préaux
 * @version 1.0
 */
public class ChunkRenderer {

    private static final Logger logger = LoggerFactory.getLogger(ChunkRenderer.class);

    // ==================== Attributs ====================

    /**
     * Référence au monde.
     */
    private final World world;

    /**
     * Registre de blocs pour la génération des meshes.
     */
    private final BlockRegistry blockRegistry;

    /**
     * Shader utilisé pour le rendu.
     */
    private final Shader shader;

    private final TextureManager textureManager;

    /**
     * Cache des meshes par chunk ET par texture.
     * Structure : ChunkCoordinate → (Texture → Mesh)
     */
    private final Map<ChunkCoordinate, Map<Texture, Mesh>> meshCache;

    /**
     * Set des chunks qui nécessitent une régénération de mesh.
     */
    private final Set<ChunkCoordinate> dirtyChunks;

    /**
     * Compteur de meshes générés depuis le démarrage.
     */
    private int totalMeshesGenerated;

    // ==================== Constructeur ====================

    /**
     * Crée un nouveau renderer de chunks.
     *
     * @param world         le monde à rendre
     * @param blockRegistry le registre de blocs
     * @param shader        le shader à utiliser pour le rendu
     * @throws IllegalArgumentException si un paramètre est null
     */
    public ChunkRenderer(World world, BlockRegistry blockRegistry, Shader shader, TextureManager textureManager) {
        if (world == null) {
            throw new IllegalArgumentException("World cannot be null");
        }
        if (blockRegistry == null) {
            throw new IllegalArgumentException("BlockRegistry cannot be null");
        }
        if (shader == null) {
            throw new IllegalArgumentException("Shader cannot be null");
        }
        if (textureManager == null) {
            throw new IllegalArgumentException("TextureManager cannot be null");
        }

        this.world = world;
        this.blockRegistry = blockRegistry;
        this.shader = shader;
        this.textureManager = textureManager;
        this.meshCache = new HashMap<>();
        this.dirtyChunks = new HashSet<>();
        this.totalMeshesGenerated = 0;

        logger.info("ChunkRenderer created");
    }

    // ==================== Mise à jour ====================

    /**
     * Met à jour le renderer.
     * <p>
     * Cette méthode doit être appelée chaque frame pour :
     * <ul>
     *   <li>Détecter les nouveaux chunks chargés</li>
     *   <li>Détecter les chunks modifiés</li>
     *   <li>Nettoyer les meshes des chunks déchargés</li>
     * </ul>
     * </p>
     */
    public void update() {
        // 1. Nettoyer les meshes des chunks déchargés
        cleanupUnloadedChunks();

        // 2. Détecter les chunks modifiés
        detectModifiedChunks();
    }

    /**
     * Nettoie les meshes des chunks qui ne sont plus chargés dans le monde.
     */
    private void cleanupUnloadedChunks() {
        Set<ChunkCoordinate> toRemove = new HashSet<>();

        for (ChunkCoordinate coord : meshCache.keySet()) {
            if (!world.hasChunk(coord.x(), coord.z())) {
                toRemove.add(coord);
            }
        }

        for (ChunkCoordinate coord : toRemove) {
            for (Mesh mesh : meshCache.remove(coord).values()) {
                if (mesh != null) {
                    mesh.cleanup();
                    logger.debug("Cleaned up mesh for unloaded chunk ({}, {})", coord.x(), coord.z());
                }
            }
        }

        if (!toRemove.isEmpty()) {
            logger.debug("Removed {} meshes for unloaded chunks", toRemove.size());
        }
    }

    /**
     * Détecte les chunks modifiés qui nécessitent une régénération du mesh.
     */
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

    /**
     * Rend tous les chunks visibles.
     * <p>
     * Cette méthode génère les meshes manquants, régénère les meshes
     * des chunks modifiés, et rend tous les chunks chargés.
     * </p>
     *
     * @param camera la caméra pour calculer les matrices MVP
     */
    public void render(Camera camera) {
        shader.bind();

        for (Chunk chunk : world.getLoadedChunks()) {
            ChunkCoordinate coord = new ChunkCoordinate(
                    chunk.getChunkX(),
                    chunk.getChunkZ()
            );

            Map<Texture, Mesh> meshes = getOrCreateMesh(coord, chunk);

            if (meshes == null || meshes.isEmpty()) {
                continue;
            }

            Matrix4f modelMatrix = new Matrix4f().identity();
            Matrix4f mvp = new Matrix4f(camera.getProjectionMatrix())
                    .mul(camera.getViewMatrix())
                    .mul(modelMatrix);

            shader.setUniform("uMVP", mvp);

            // ==================== RENDRE CHAQUE MESH AVEC SA TEXTURE ====================
            for (Map.Entry<Texture, Mesh> entry : meshes.entrySet()) {
                Texture texture = entry.getKey();
                Mesh mesh = entry.getValue();

                // Binder la texture pour ce mesh
                texture.bind();
                shader.setUniform("uTexture", 0);

                // Rendre
                mesh.render();
            }
        }

        shader.unbind();
    }

    /**
     * Récupère le mesh d'un chunk depuis le cache, ou le génère s'il n'existe pas.
     *
     * @param coord coordonnées du chunk
     * @param chunk le chunk
     * @return le mesh, ou null si le chunk est vide
     */
    private Map<Texture, Mesh> getOrCreateMesh(ChunkCoordinate coord, Chunk chunk) {
        if (!chunk.isGenerated()) {
            return null;
        }

        if (dirtyChunks.contains(coord)) {
            Map<Texture, Mesh> oldMeshes = meshCache.remove(coord);
            if (oldMeshes != null) {
                for (Mesh mesh : oldMeshes.values()) {
                    mesh.cleanup();
                }
            }
            dirtyChunks.remove(coord);
        }

        Map<Texture, Mesh> meshes = meshCache.get(coord);
        if (meshes != null) {
            return meshes;
        }

        if (chunk.isEmpty()) {
            return null;
        }

        logger.debug("Generating meshes for chunk ({}, {})", coord.x(), coord.z());

        ChunkMesh chunkMeshBuilder = new ChunkMesh(
                chunk, world, blockRegistry, textureManager
        );
        meshes = chunkMeshBuilder.build();

        meshCache.put(coord, meshes);
        totalMeshesGenerated++;

        return meshes;
    }

    // ==================== Utilitaires ====================

    /**
     * Marque un chunk comme nécessitant une régénération de mesh.
     * <p>
     * À appeler quand un bloc est modifié dans un chunk.
     * </p>
     *
     * @param chunkX coordonnée X du chunk
     * @param chunkZ coordonnée Z du chunk
     */
    public void markChunkDirty(int chunkX, int chunkZ) {
        dirtyChunks.add(new ChunkCoordinate(chunkX, chunkZ));
        logger.trace("Chunk ({}, {}) marked dirty", chunkX, chunkZ);
    }

    /**
     * Force la régénération immédiate du mesh d'un chunk.
     *
     * @param chunkX coordonnée X du chunk
     * @param chunkZ coordonnée Z du chunk
     */
    public void regenerateChunk(int chunkX, int chunkZ) {
        ChunkCoordinate coord = new ChunkCoordinate(chunkX, chunkZ);

        // Nettoyer l'ancien mesh
        for (Mesh oldMesh: meshCache.remove(coord).values()) {
            if (oldMesh != null) {
                oldMesh.cleanup();
            }
        }

        // Marquer comme dirty pour régénération au prochain render
        dirtyChunks.add(coord);

        logger.debug("Forced regeneration of chunk ({}, {})", chunkX, chunkZ);
    }

    /**
     * Notifie le renderer qu'un nouveau chunk a été chargé.
     * Marque les voisins comme dirty pour régénérer leurs bordures.
     *
     * @param chunkX coordonnée X du chunk chargé
     * @param chunkZ coordonnée Z du chunk chargé
     */
    public void onChunkLoaded(int chunkX, int chunkZ) {
        // Marquer les 4 voisins comme dirty
        markChunkDirty(chunkX - 1, chunkZ);     // Ouest
        markChunkDirty(chunkX + 1, chunkZ);     // Est
        markChunkDirty(chunkX, chunkZ - 1);     // Nord
        markChunkDirty(chunkX, chunkZ + 1);     // Sud

        logger.info("Marked neighbors of chunk ({}, {}) as dirty", chunkX, chunkZ);
    }

    // ==================== Nettoyage ====================

    /**
     * Libère toutes les ressources OpenGL.
     * <p>
     * À appeler avant la fermeture de l'application.
     * </p>
     */
    public void cleanup() {
        for (Map<Texture, Mesh> meshes : meshCache.values()) {
            for (Mesh mesh : meshes.values()) {
                mesh.cleanup();
            }
        }
        meshCache.clear();
    }

    // ==================== Getters ====================

    /**
     * Retourne le nombre de meshes actuellement en cache.
     *
     * @return le nombre de meshes
     */
    public int getCachedMeshCount() {
        return meshCache.size();
    }

    /**
     * Retourne le nombre total de meshes générés depuis le démarrage.
     *
     * @return le nombre total de meshes générés
     */
    public int getTotalMeshesGenerated() {
        return totalMeshesGenerated;
    }

    /**
     * Retourne le nombre de chunks marqués comme "dirty".
     *
     * @return le nombre de chunks dirty
     */
    public int getDirtyChunkCount() {
        return dirtyChunks.size();
    }

    /**
     * Compte le nombre total de triangles dans tous les meshes.
     *
     * @return le nombre total de triangles
     */
    public int getTotalTriangleCount() {
        return meshCache.values().stream()
                .flatMap(entry -> entry.values().stream())
                .mapToInt(Mesh::getTriangleCount)
                .sum();
    }
}