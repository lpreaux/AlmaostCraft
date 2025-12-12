package org.almostcraft.render.chunk;

import org.almostcraft.camera.Camera;
import org.almostcraft.render.chunk.frustum.Frustum;
import org.almostcraft.render.core.Mesh;
import org.almostcraft.render.core.Shader;
import org.almostcraft.render.texture.TextureArray;
import org.almostcraft.world.chunk.ChunkCoordinate;
import org.almostcraft.world.World;
import org.almostcraft.world.block.BlockRegistry;
import org.almostcraft.world.chunk.Chunk;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.lwjgl.opengl.GL13.GL_TEXTURE0;

/**
 * Gestionnaire de rendu pour tous les chunks du monde avec optimisations.
 * <p>
 * Le ChunkRenderer gère l'ensemble du pipeline de rendu des chunks :
 * <ul>
 *   <li>Génération progressive des meshes (pour éviter les freeze)</li>
 *   <li>Cache de meshes avec système de dirty chunks</li>
 *   <li>Frustum culling pour éliminer les chunks hors écran</li>
 *   <li>Nettoyage automatique des chunks déchargés</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Architecture :</strong><br>
 * Le renderer ne gère QUE le rendu et les optimisations visuelles.
 * Le chargement/déchargement logique des chunks est géré par le World/ChunkLoader.
 * Cette séparation permet d'avoir des chunks actifs (simulation) même s'ils ne
 * sont pas rendus (hors écran ou trop loin).
 * </p>
 * <p>
 * <strong>Utilisation :</strong>
 * <pre>
 * ChunkRenderer renderer = new ChunkRenderer(world, blockRegistry, shader, textureArray);
 *
 * // Boucle de jeu
 * renderer.update();          // Génère les meshes progressivement
 * renderer.render(camera);    // Rend les chunks visibles
 * </pre>
 * </p>
 *
 * @author Lucas Préaux
 * @version 3.1 (avec frustum culling)
 * @see ChunkMesh
 * @see Frustum
 */
public class ChunkRenderer {

    private static final Logger logger = LoggerFactory.getLogger(ChunkRenderer.class);

    // ==================== Constantes ====================

    /**
     * Nombre maximum de meshes générés par frame.
     * <p>
     * Limite la génération de meshes pour éviter les freeze lors du
     * chargement de nombreux chunks. Valeur typique : 2-8 selon les performances.
     * </p>
     */
    private static final int MAX_MESHES_PER_FRAME = 4;

    // ==================== Attributs ====================

    /** Le monde dont on rend les chunks */
    private final World world;

    /** Registre des types de blocs */
    private final BlockRegistry blockRegistry;

    /** Shader utilisé pour le rendu des chunks */
    private final Shader shader;

    /** Texture array contenant toutes les textures de blocs */
    private final TextureArray textureArray;

    /** Frustum pour le culling des chunks hors écran */
    private final Frustum frustum;

    /** Cache associant coordonnées de chunk → mesh OpenGL */
    private final Map<ChunkCoordinate, Mesh> meshCache;

    /** Ensemble des chunks nécessitant une régénération de mesh */
    private final Set<ChunkCoordinate> dirtyChunks;

    /** Matrice view-projection réutilisée pour éviter les allocations */
    private final Matrix4f viewProjection;

    /** Statistique : nombre total de meshes générés depuis le démarrage */
    private int totalMeshesGenerated;

    // ==================== Constructeur ====================

    /**
     * Crée un nouveau gestionnaire de rendu de chunks.
     * <p>
     * Le renderer doit être nettoyé avec {@link #cleanup()} avant la fermeture
     * de l'application pour libérer les ressources OpenGL.
     * </p>
     *
     * @param world le monde à rendre (non null)
     * @param blockRegistry le registre des blocs (non null)
     * @param shader le shader de rendu (non null)
     * @param textureArray la texture array des blocs (non null)
     * @throws IllegalArgumentException si un paramètre est null
     */
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
        this.frustum = new Frustum();
        this.meshCache = new HashMap<>();
        this.dirtyChunks = new HashSet<>();
        this.viewProjection = new Matrix4f();
        this.totalMeshesGenerated = 0;

        logger.info("ChunkRenderer initialized with frustum culling");
    }

    // ==================== Mise à jour ====================

    /**
     * Met à jour le renderer (à appeler chaque frame).
     * <p>
     * Cette méthode effectue :
     * <ol>
     *   <li>Nettoyage des meshes de chunks déchargés</li>
     *   <li>Détection des chunks modifiés</li>
     *   <li>Génération progressive des meshes en attente</li>
     * </ol>
     * La génération est limitée à {@link #MAX_MESHES_PER_FRAME} meshes par
     * frame pour maintenir un framerate stable.
     * </p>
     */
    public void update() {
        cleanupUnloadedChunks();
        detectModifiedChunks();
        generatePendingMeshes();
    }

    /**
     * Génère progressivement les meshes des chunks en attente.
     * <p>
     * Traite jusqu'à {@link #MAX_MESHES_PER_FRAME} chunks par frame pour
     * éviter les freeze lors du chargement massif de chunks.
     * </p>
     */
    private void generatePendingMeshes() {
        int generated = 0;

        Iterator<ChunkCoordinate> iterator = dirtyChunks.iterator();
        while (iterator.hasNext() && generated < MAX_MESHES_PER_FRAME) {
            ChunkCoordinate coord = iterator.next();

            // Vérifier que le chunk existe toujours
            if (!world.hasChunk(coord.x(), coord.z())) {
                iterator.remove();
                continue;
            }

            Chunk chunk = world.getChunk(coord.x(), coord.z());

            // Ignorer les chunks non générés ou vides
            if (!chunk.isGenerated() || chunk.isEmpty()) {
                iterator.remove();
                continue;
            }

            logger.debug("Generating mesh for chunk ({}, {})", coord.x(), coord.z());

            // Nettoyer l'ancien mesh s'il existe
            Mesh oldMesh = meshCache.get(coord);
            if (oldMesh != null) {
                oldMesh.cleanup();
            }

            // Générer le nouveau mesh
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

    /**
     * Nettoie les meshes des chunks qui ont été déchargés du monde.
     * <p>
     * Cette méthode parcourt le cache de meshes et supprime ceux dont
     * les chunks ne sont plus chargés dans le monde.
     * </p>
     */
    private void cleanupUnloadedChunks() {
        Set<ChunkCoordinate> toRemove = new HashSet<>();

        // Identifier les meshes à supprimer
        for (ChunkCoordinate coord : meshCache.keySet()) {
            if (!world.hasChunk(coord.x(), coord.z())) {
                toRemove.add(coord);
            }
        }

        // Supprimer et nettoyer les meshes
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
     * Détecte les chunks modifiés et les marque pour régénération.
     * <p>
     * Parcourt tous les chunks chargés et vérifie leur flag isModified().
     * Les chunks modifiés sont ajoutés à la file de régénération.
     * </p>
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
     * Rend tous les chunks visibles depuis la caméra.
     * <p>
     * <strong>Pipeline de rendu :</strong>
     * <ol>
     *   <li>Active le shader et les textures</li>
     *   <li>Calcule le frustum depuis la caméra</li>
     *   <li>Pour chaque chunk chargé :
     *     <ul>
     *       <li>Teste la visibilité (frustum culling)</li>
     *       <li>Récupère ou génère le mesh si nécessaire</li>
     *       <li>Calcule la matrice MVP et rend le chunk</li>
     *     </ul>
     *   </li>
     *   <li>Désactive le shader</li>
     * </ol>
     * </p>
     * <p>
     * <strong>Optimisations appliquées :</strong>
     * <ul>
     *   <li>Frustum culling : élimine 50-80% des chunks selon l'orientation</li>
     *   <li>Cache de meshes : évite la régénération inutile</li>
     *   <li>Early-exit pour chunks vides ou non générés</li>
     * </ul>
     * </p>
     *
     * @param camera la caméra définissant le point de vue (non null)
     */
    public void render(Camera camera) {
        // Setup du rendu
        shader.bind();
        textureArray.bind(GL_TEXTURE0);
        shader.setUniform("uTextureArray", 0);

        // Mise à jour du frustum pour le culling
        viewProjection.set(camera.getProjectionMatrix())
                .mul(camera.getViewMatrix());
        frustum.updateFrustum(viewProjection);

        // Rendu de chaque chunk visible
        for (Chunk chunk : world.getLoadedChunks()) {
            // Frustum culling : skip les chunks hors écran
            if (!frustum.isChunkVisible(
                    chunk.getChunkX(),
                    chunk.getChunkZ(),
                    chunk.WIDTH,
                    chunk.HEIGHT,
                    chunk.DEPTH,
                    0
            )) {
                continue;
            }

            ChunkCoordinate coord = new ChunkCoordinate(
                    chunk.getChunkX(),
                    chunk.getChunkZ()
            );

            // Récupérer ou générer le mesh
            Mesh mesh = getOrCreateMesh(coord, chunk);

            if (mesh == null) {
                continue;
            }

            // Calculer la matrice MVP et rendre
            Matrix4f modelMatrix = new Matrix4f().identity();
            Matrix4f mvp = new Matrix4f(camera.getProjectionMatrix())
                    .mul(camera.getViewMatrix())
                    .mul(modelMatrix);

            shader.setUniform("uMVP", mvp);

            mesh.render();
        }

        shader.unbind();
    }

    /**
     * Récupère un mesh depuis le cache ou le génère si nécessaire.
     * <p>
     * Cette méthode gère également la régénération des meshes marqués
     * comme dirty (modifiés).
     * </p>
     *
     * @param coord les coordonnées du chunk
     * @param chunk le chunk à rendre
     * @return le mesh du chunk, ou null si le chunk n'est pas prêt/vide
     */
    private Mesh getOrCreateMesh(ChunkCoordinate coord, Chunk chunk) {
        // Chunk pas encore généré
        if (!chunk.isGenerated()) {
            return null;
        }

        // Régénérer si le chunk est dirty
        if (dirtyChunks.contains(coord)) {
            Mesh oldMesh = meshCache.remove(coord);
            if (oldMesh != null) {
                oldMesh.cleanup();
            }
            dirtyChunks.remove(coord);
        }

        // Vérifier le cache
        Mesh mesh = meshCache.get(coord);
        if (mesh != null) {
            return mesh;
        }

        // Chunk vide : pas besoin de mesh
        if (chunk.isEmpty()) {
            return null;
        }

        // Générer le mesh
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

    /**
     * Marque un chunk comme nécessitant une régénération de mesh.
     * <p>
     * Utilisé quand un chunk est modifié (placement/destruction de blocs).
     * Le mesh sera régénéré lors du prochain appel à {@link #update()}.
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
     * Force la régénération immédiate d'un chunk (supprime le mesh existant).
     * <p>
     * Le nouveau mesh sera généré lors du prochain {@link #update()}.
     * </p>
     *
     * @param chunkX coordonnée X du chunk
     * @param chunkZ coordonnée Z du chunk
     */
    public void regenerateChunk(int chunkX, int chunkZ) {
        ChunkCoordinate coord = new ChunkCoordinate(chunkX, chunkZ);

        Mesh oldMesh = meshCache.remove(coord);
        if (oldMesh != null) {
            oldMesh.cleanup();
        }

        dirtyChunks.add(coord);

        logger.debug("Forced regeneration of chunk ({}, {})", chunkX, chunkZ);
    }

    /**
     * Callback appelé quand un chunk est chargé dans le monde.
     * <p>
     * Marque les chunks voisins comme dirty car leurs faces adjacentes
     * peuvent maintenant être cachées (optimisation du greedy meshing).
     * </p>
     *
     * @param chunkX coordonnée X du chunk chargé
     * @param chunkZ coordonnée Z du chunk chargé
     */
    public void onChunkLoaded(int chunkX, int chunkZ) {
        markChunkDirty(chunkX - 1, chunkZ);
        markChunkDirty(chunkX + 1, chunkZ);
        markChunkDirty(chunkX, chunkZ - 1);
        markChunkDirty(chunkX, chunkZ + 1);

        logger.info("Marked neighbors of chunk ({}, {}) as dirty", chunkX, chunkZ);
    }

    // ==================== Nettoyage ====================

    /**
     * Libère toutes les ressources OpenGL du renderer.
     * <p>
     * <strong>IMPORTANT :</strong> Cette méthode DOIT être appelée avant
     * la fermeture de l'application pour éviter les fuites mémoire GPU.
     * </p>
     */
    public void cleanup() {
        for (Mesh mesh : meshCache.values()) {
            mesh.cleanup();
        }
        meshCache.clear();
        logger.info("ChunkRenderer cleaned up ({} meshes freed)", meshCache.size());
    }

    // ==================== Getters (statistiques) ====================

    /**
     * Retourne le nombre de meshes actuellement en cache.
     *
     * @return nombre de meshes cachés
     */
    public int getCachedMeshCount() {
        return meshCache.size();
    }

    /**
     * Retourne le nombre total de meshes générés depuis le démarrage.
     * <p>
     * Statistique cumulative utile pour le debug/profiling.
     * </p>
     *
     * @return nombre total de meshes générés
     */
    public int getTotalMeshesGenerated() {
        return totalMeshesGenerated;
    }

    /**
     * Retourne le nombre de chunks en attente de régénération.
     *
     * @return nombre de chunks dirty
     */
    public int getDirtyChunkCount() {
        return dirtyChunks.size();
    }

    /**
     * Calcule le nombre total de triangles rendus.
     * <p>
     * Somme tous les triangles de tous les meshes en cache.
     * Utile pour mesurer la charge GPU.
     * </p>
     *
     * @return nombre total de triangles
     */
    public int getTotalTriangleCount() {
        return meshCache.values().stream()
                .mapToInt(Mesh::getTriangleCount)
                .sum();
    }
}
