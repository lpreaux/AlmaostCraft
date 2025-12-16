package org.almostcraft.render.chunk;

import org.almostcraft.camera.Camera;
import org.almostcraft.graphics.culling.CullingManager;
import org.almostcraft.render.core.Mesh;
import org.almostcraft.render.core.Shader;
import org.almostcraft.render.texture.TextureArray;
import org.almostcraft.world.chunk.ChunkCoordinate;
import org.almostcraft.world.World;
import org.almostcraft.world.block.BlockRegistry;
import org.almostcraft.world.chunk.Chunk;
import org.joml.Matrix4f;
import org.joml.Vector3f;
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
 *   <li>Culling délégué au {@link CullingManager}</li>
 *   <li>Nettoyage automatique des chunks déchargés</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Architecture :</strong><br>
 * Le renderer ne gère QUE le rendu et les optimisations visuelles.
 * Le chargement/déchargement logique des chunks est géré par le World/ChunkLoader.
 * Le culling (frustum, distance) est délégué au CullingManager.
 * </p>
 * <p>
 * <strong>Utilisation :</strong>
 * <pre>
 * ChunkRenderer renderer = new ChunkRenderer(world, blockRegistry, shader, textureArray, renderDistance);
 *
 * // Boucle de jeu
 * renderer.update();                          // Génère les meshes progressivement
 * renderer.render(camera, playerPosition);    // Rend les chunks visibles
 * </pre>
 * </p>
 *
 * @author Lucas Préaux
 * @version 4.0 (refactorisé avec CullingManager)
 * @see ChunkMeshGenerator
 * @see CullingManager
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

    /** Générateur de mesh réutilisable pour tous les chunks */
    private final ChunkMeshGenerator meshGenerator;

    /** Gestionnaire centralisé du culling (frustum + distance) */
    private final CullingManager cullingManager;

    /** Ensemble des chunks nécessitant une régénération de mesh */
    private final Set<ChunkCoordinate> dirtyChunks;

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
     * @param renderDistance distance de rendu en chunks (ex: 8 = 128 blocs)
     * @throws IllegalArgumentException si un paramètre est null ou invalide
     */
    public ChunkRenderer(World world, BlockRegistry blockRegistry, Shader shader,
                         TextureArray textureArray, int renderDistance) {
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
        if (renderDistance <= 0) {
            throw new IllegalArgumentException("Render distance must be positive");
        }

        this.world = world;
        this.blockRegistry = blockRegistry;
        this.shader = shader;
        this.textureArray = textureArray;
        this.meshGenerator = new ChunkMeshGenerator(world, blockRegistry, textureArray);
        this.cullingManager = new CullingManager(renderDistance);
        this.dirtyChunks = new HashSet<>();
        this.totalMeshesGenerated = 0;

        logger.info("ChunkRenderer initialized with render distance: {} chunks ({} blocks)",
                renderDistance, renderDistance * 16);
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

            // Générer le nouveau mesh via le generator
            Mesh mesh = meshGenerator.generateMesh(chunk);
            chunk.setMesh(mesh);

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
     * Les meshes sont maintenant stockés directement dans les chunks,
     * donc on les nettoie via chunk.destroyMesh().
     * </p>
     */
    private void cleanupUnloadedChunks() {
        // Note: Le nettoyage des mesh est maintenant géré par le World
        // quand il décharge un chunk (via chunk.cleanup())
        // Cette méthode est conservée pour compatibilité future
    }

    /**
     * Détecte les chunks modifiés et les marque pour régénération.
     * <p>
     * Parcourt tous les chunks chargés et vérifie leur flag needsMeshRebuild().
     * Les chunks nécessitant un rebuild sont ajoutés à la file de régénération.
     * </p>
     */
    private void detectModifiedChunks() {
        for (Chunk chunk : world.getLoadedChunks()) {
            if (chunk.needsMeshRebuild()) {
                ChunkCoordinate coord = new ChunkCoordinate(
                        chunk.getChunkX(),
                        chunk.getChunkZ()
                );
                dirtyChunks.add(coord);
            }
        }
    }

    // ==================== Rendu ====================

    /**
     * Rend tous les chunks visibles depuis la caméra.
     * <p>
     * <strong>Pipeline de rendu :</strong>
     * <ol>
     *   <li>Met à jour le CullingManager avec la caméra</li>
     *   <li>Obtient la liste des chunks visibles (après culling)</li>
     *   <li>Active le shader et les textures</li>
     *   <li>Pour chaque chunk visible :
     *     <ul>
     *       <li>Calcule la matrice MVP</li>
     *       <li>Rend le chunk si son mesh existe</li>
     *     </ul>
     *   </li>
     *   <li>Désactive le shader</li>
     * </ol>
     * </p>
     * <p>
     * <strong>Optimisations appliquées :</strong>
     * <ul>
     *   <li>Distance culling : élimine les chunks trop loin</li>
     *   <li>Frustum culling : élimine 50-80% des chunks selon l'orientation</li>
     *   <li>Empty chunk culling : skip les chunks vides</li>
     *   <li>Mesh existence check : skip les chunks sans mesh généré</li>
     * </ul>
     * </p>
     *
     * @param camera la caméra définissant le point de vue (non null)
     * @param playerPosition position du joueur pour le culling de distance (non null)
     */
    public void render(Camera camera, Vector3f playerPosition) {
        // 1. Mettre à jour le culling manager
        cullingManager.update(camera, playerPosition);

        // 2. Obtenir les chunks visibles (après distance + frustum culling)
        Collection<Chunk> loadedChunks = world.getLoadedChunks();
        List<Chunk> visibleChunks = cullingManager.cullChunks(loadedChunks);

        // 3. Setup du rendu
        shader.bind();
        textureArray.bind(GL_TEXTURE0);
        shader.setUniform("uTextureArray", 0);

        // 4. Tracker les stats de rendu
        long renderStart = System.nanoTime();
        int rendered = 0;

        // 5. Rendu de chaque chunk visible
        for (Chunk chunk : visibleChunks) {
            // Skip si pas de mesh
            if (!chunk.hasMesh()) {
                continue;
            }

            // Calculer la matrice MVP
            Matrix4f modelMatrix = new Matrix4f().identity();
            Matrix4f mvp = new Matrix4f(camera.getProjectionMatrix())
                    .mul(camera.getViewMatrix())
                    .mul(modelMatrix);

            shader.setUniform("uMVP", mvp);

            // Rendre le chunk
            chunk.getMesh().render();
            rendered++;
        }

        shader.unbind();

        // 6. Mettre à jour les stats de rendu
        long renderEnd = System.nanoTime();
        cullingManager.getStats().addRenderTime(renderEnd - renderStart);

        // 7. Log périodique des stats (toutes les 60 frames)
        if (totalMeshesGenerated % 60 == 0) {
            logger.trace("Render stats: {}", cullingManager.getStats().getCompactSummary());
        }
    }

    // ==================== Configuration ====================

    /**
     * Change la distance de rendu.
     * <p>
     * Une distance plus grande permet de voir plus loin mais réduit les performances.
     * </p>
     *
     * @param renderDistance nouvelle distance en chunks (doit être positive)
     */
    public void setRenderDistance(int renderDistance) {
        cullingManager.setRenderDistance(renderDistance);
        logger.info("Render distance changed to {} chunks ({} blocks)",
                renderDistance, renderDistance * 16);
    }

    /**
     * Obtient la distance de rendu actuelle.
     *
     * @return distance de rendu en chunks
     */
    public int getRenderDistance() {
        return cullingManager.getRenderDistance();
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

        // Marquer aussi le chunk lui-même si il existe
        if (world.hasChunk(chunkX, chunkZ)) {
            world.getChunk(chunkX, chunkZ).markMeshDirty();
        }

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
        if (world.hasChunk(chunkX, chunkZ)) {
            Chunk chunk = world.getChunk(chunkX, chunkZ);
            chunk.destroyMesh();
            chunk.markMeshDirty();
        }

        ChunkCoordinate coord = new ChunkCoordinate(chunkX, chunkZ);
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
        markChunkDirty(chunkX, chunkZ);
        markChunkDirty(chunkX - 1, chunkZ);
        markChunkDirty(chunkX + 1, chunkZ);
        markChunkDirty(chunkX, chunkZ - 1);
        markChunkDirty(chunkX, chunkZ + 1);

        logger.debug("Marked chunk ({}, {}) and neighbors as dirty", chunkX, chunkZ);
    }

    // ==================== Nettoyage ====================

    /**
     * Libère toutes les ressources du renderer.
     * <p>
     * <strong>IMPORTANT :</strong> Cette méthode DOIT être appelée avant
     * la fermeture de l'application pour éviter les fuites mémoire.
     * </p>
     */
    public void cleanup() {
        // Les meshes sont maintenant stockés dans les chunks
        // Ils seront nettoyés quand le World sera nettoyé
        logger.info("ChunkRenderer cleaned up");
    }

    // ==================== Getters (statistiques) ====================

    /**
     * Retourne les statistiques de culling du dernier frame.
     *
     * @return statistiques détaillées du culling
     */
    public String getCullingStats() {
        return cullingManager.getStats().getCompactSummary();
    }

    /**
     * Retourne les statistiques de culling détaillées.
     *
     * @return statistiques complètes avec analyse
     */
    public String getDetailedCullingStats() {
        return cullingManager.getStats().getDetailedSummary();
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
     * Calcule le nombre total de triangles actuellement rendus.
     * <p>
     * Somme tous les triangles de tous les chunks visibles du dernier frame.
     * Utile pour mesurer la charge GPU.
     * </p>
     *
     * @return nombre total de triangles rendus
     */
    public int getRenderedTriangleCount() {
        int total = 0;
        for (Chunk chunk : world.getLoadedChunks()) {
            if (chunk.hasMesh()) {
                total += chunk.getMesh().getTriangleCount();
            }
        }
        return total;
    }

    /**
     * Retourne le gestionnaire de culling pour accès direct si nécessaire.
     *
     * @return le CullingManager
     */
    public CullingManager getCullingManager() {
        return cullingManager;
    }
}
