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

    /**
     * Cache des meshes générés (ChunkCoordinate → Mesh).
     */
    private final Map<ChunkCoordinate, Mesh> meshCache;

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
    public ChunkRenderer(World world, BlockRegistry blockRegistry, Shader shader) {
        if (world == null) {
            throw new IllegalArgumentException("World cannot be null");
        }
        if (blockRegistry == null) {
            throw new IllegalArgumentException("BlockRegistry cannot be null");
        }
        if (shader == null) {
            throw new IllegalArgumentException("Shader cannot be null");
        }

        this.world = world;
        this.blockRegistry = blockRegistry;
        this.shader = shader;
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
        // Binder le shader
        shader.bind();

        int chunksRendered = 0;

        // Parcourir tous les chunks chargés
        for (Chunk chunk : world.getLoadedChunks()) {
            ChunkCoordinate coord = new ChunkCoordinate(
                    chunk.getChunkX(),
                    chunk.getChunkZ()
            );

            // Récupérer ou générer le mesh
            Mesh mesh = getOrCreateMesh(coord, chunk);

            // Si le mesh est vide (chunk vide), ne pas rendre
            if (mesh == null || mesh.getTriangleCount() == 0) {
                continue;
            }

            // Calculer la matrice Model pour ce chunk
            // Les blocs sont déjà en coordonnées mondiales, donc Model = Identity
            Matrix4f modelMatrix = new Matrix4f().identity();

            // Calculer MVP
            Matrix4f mvp = new Matrix4f(camera.getProjectionMatrix())
                    .mul(camera.getViewMatrix())
                    .mul(modelMatrix);

            // Envoyer au shader
            shader.setUniform("uMVP", mvp);

            // Rendre le mesh
            mesh.render();
            chunksRendered++;
        }

        shader.unbind();

        // Log occasionnel
        if (chunksRendered > 0 && totalMeshesGenerated % 10 == 0) {
            logger.trace("Rendered {} chunks", chunksRendered);
        }
    }

    /**
     * Récupère le mesh d'un chunk depuis le cache, ou le génère s'il n'existe pas.
     *
     * @param coord coordonnées du chunk
     * @param chunk le chunk
     * @return le mesh, ou null si le chunk est vide
     */
    private Mesh getOrCreateMesh(ChunkCoordinate coord, Chunk chunk) {
        // Si le chunk est marqué comme "dirty", régénérer le mesh
        if (dirtyChunks.contains(coord)) {
            Mesh oldMesh = meshCache.remove(coord);
            if (oldMesh != null) {
                oldMesh.cleanup();
            }
            dirtyChunks.remove(coord);
            logger.debug("Regenerating mesh for modified chunk ({}, {})", coord.x(), coord.z());
        }

        // Récupérer depuis le cache
        Mesh mesh = meshCache.get(coord);
        if (mesh != null) {
            return mesh;
        }

        // Vérifier si le chunk est vide
        if (chunk.isEmpty()) {
            logger.trace("Chunk ({}, {}) is empty, skipping mesh generation", coord.x(), coord.z());
            return null;
        }

        // Générer le mesh
        logger.debug("Generating mesh for chunk ({}, {})", coord.x(), coord.z());
        long startTime = System.nanoTime();

        ChunkMesh chunkMeshBuilder = new ChunkMesh(chunk, blockRegistry);
        mesh = chunkMeshBuilder.build();

        long endTime = System.nanoTime();
        double duration = (endTime - startTime) / 1_000_000.0; // en ms

        // Mettre en cache
        meshCache.put(coord, mesh);
        totalMeshesGenerated++;

        logger.debug("Mesh generated for chunk ({}, {}) in {:.2f}ms - {} triangles",
                coord.x(), coord.z(), duration, mesh.getTriangleCount());

        return mesh;
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
        Mesh oldMesh = meshCache.remove(coord);
        if (oldMesh != null) {
            oldMesh.cleanup();
        }

        // Marquer comme dirty pour régénération au prochain render
        dirtyChunks.add(coord);

        logger.debug("Forced regeneration of chunk ({}, {})", chunkX, chunkZ);
    }

    // ==================== Nettoyage ====================

    /**
     * Libère toutes les ressources OpenGL.
     * <p>
     * À appeler avant la fermeture de l'application.
     * </p>
     */
    public void cleanup() {
        logger.info("Cleaning up ChunkRenderer ({} meshes)", meshCache.size());

        for (Mesh mesh : meshCache.values()) {
            mesh.cleanup();
        }

        meshCache.clear();
        dirtyChunks.clear();

        logger.info("ChunkRenderer cleaned up");
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
                .mapToInt(Mesh::getTriangleCount)
                .sum();
    }
}
