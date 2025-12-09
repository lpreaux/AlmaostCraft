package org.almostcraft.world;

import org.almostcraft.render.ChunkRenderer;
import org.almostcraft.world.chunk.Chunk;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Gestionnaire de chargement et déchargement des chunks autour du joueur.
 * <p>
 * Ce système maintient une "bulle" de chunks chargés autour de la position du joueur
 * et décharge les chunks trop éloignés pour optimiser la mémoire.
 * </p>
 * <p>
 * Le chargement/déchargement est progressif (limité par frame) pour éviter
 * les freezes lors des déplacements rapides.
 * </p>
 *
 * @author Lucas Préaux
 * @version 1.0
 */
public class ChunkLoader {

    private static final Logger logger = LoggerFactory.getLogger(ChunkLoader.class);

    // ==================== Constantes ====================

    /**
     * Distance de rendu par défaut en chunks (rayon).
     * <p>
     * Une distance de 8 signifie un carré de 17×17 chunks (289 chunks).
     * </p>
     */
    private static final int DEFAULT_RENDER_DISTANCE = 8;

    /**
     * Distance de déchargement par défaut (render distance + buffer).
     * <p>
     * Le buffer évite les cycles charge/décharge constants quand le joueur
     * se déplace à la limite de la render distance.
     * </p>
     */
    private static final int DEFAULT_UNLOAD_DISTANCE_BUFFER = 2;

    /**
     * Nombre maximum de chunks à charger par frame.
     * <p>
     * Limite à 4 pour éviter les freezes. Ajuster selon les performances.
     * </p>
     */
    private static final int MAX_CHUNKS_TO_LOAD_PER_FRAME = 4;

    /**
     * Nombre maximum de chunks à décharger par frame.
     * <p>
     * Le déchargement est généralement plus rapide que le chargement.
     * </p>
     */
    private static final int MAX_CHUNKS_TO_UNLOAD_PER_FRAME = 8;

    // ==================== Attributs ====================

    /**
     * Référence au monde pour charger/décharger les chunks.
     */
    private final World world;

    private final ChunkRenderer chunkRenderer;

    /**
     * Distance de rendu en chunks (rayon autour du joueur).
     */
    private int renderDistance;

    /**
     * Distance de déchargement en chunks.
     */
    private int unloadDistance;

    /**
     * Position du chunk où se trouve actuellement le joueur.
     * <p>
     * Utilisé pour détecter quand le joueur change de chunk et déclencher
     * une mise à jour.
     * </p>
     */
    private ChunkCoordinate lastPlayerChunk;

    /**
     * Set des chunks qui devraient être chargés selon la position actuelle.
     * <p>
     * Recalculé à chaque mise à jour de position.
     * </p>
     */
    private Set<ChunkCoordinate> targetChunks;

    /**
     * File d'attente des chunks à charger.
     * <p>
     * Triée par distance au joueur (les plus proches en premier).
     * </p>
     */
    private final Queue<ChunkCoordinate> loadQueue;

    /**
     * File d'attente des chunks à décharger.
     */
    private final Queue<ChunkCoordinate> unloadQueue;

    // ==================== Constructeur ====================

    /**
     * Crée un nouveau gestionnaire de chunks avec distance par défaut.
     *
     * @param world le monde à gérer
     */
    public ChunkLoader(World world, ChunkRenderer chunkRenderer) {
        this(world, DEFAULT_RENDER_DISTANCE, chunkRenderer);
    }

    /**
     * Crée un nouveau gestionnaire de chunks avec distance personnalisée.
     *
     * @param world          le monde à gérer
     * @param renderDistance la distance de rendu en chunks
     * @throws IllegalArgumentException si world est null ou renderDistance invalide
     */
    public ChunkLoader(World world, int renderDistance, ChunkRenderer chunkRenderer) {
        if (world == null) {
            throw new IllegalArgumentException("World cannot be null");
        }
        if (renderDistance < 1 || renderDistance > 32) {
            throw new IllegalArgumentException(
                    String.format("Render distance must be between 1 and 32, got: %d", renderDistance)
            );
        }

        this.world = world;
        this.renderDistance = renderDistance;
        this.unloadDistance = renderDistance + DEFAULT_UNLOAD_DISTANCE_BUFFER;
        this.targetChunks = new HashSet<>();
        this.loadQueue = new LinkedList<>();
        this.unloadQueue = new LinkedList<>();
        this.lastPlayerChunk = null;
        this.chunkRenderer = chunkRenderer;

        logger.info("ChunkLoader created: renderDistance={}, unloadDistance={}",
                renderDistance, unloadDistance);
    }

    // ==================== Méthode principale : update ====================

    /**
     * Met à jour le chargement des chunks en fonction de la position du joueur.
     * <p>
     * À appeler chaque frame dans la boucle de jeu. Cette méthode :
     * <ol>
     *   <li>Détecte si le joueur a changé de chunk</li>
     *   <li>Recalcule les chunks à charger si nécessaire</li>
     *   <li>Charge progressivement les chunks manquants</li>
     *   <li>Décharge progressivement les chunks trop éloignés</li>
     * </ol>
     * </p>
     *
     * @param playerPosition la position actuelle du joueur dans le monde
     */
    public void update(Vector3f playerPosition) {
        // Convertir la position du joueur en coordonnées de chunk
        int playerChunkX = (int) Math.floor(playerPosition.x / 16);
        int playerChunkZ = (int) Math.floor(playerPosition.z / 16);
        ChunkCoordinate currentChunk = new ChunkCoordinate(playerChunkX, playerChunkZ);

        // Vérifier si le joueur a changé de chunk
        if (!currentChunk.equals(lastPlayerChunk)) {
            onPlayerChunkChanged(currentChunk);
            lastPlayerChunk = currentChunk;
        }

        // Traiter les files de chargement/déchargement
        processLoadQueue();
        processUnloadQueue();
    }

    // ==================== Changement de chunk du joueur ====================

    /**
     * Appelé quand le joueur entre dans un nouveau chunk.
     * <p>
     * Recalcule les chunks à charger et met à jour les files d'attente.
     * </p>
     *
     * @param playerChunk le chunk où se trouve maintenant le joueur
     */
    private void onPlayerChunkChanged(ChunkCoordinate playerChunk) {
        logger.debug("Player moved to chunk ({}, {})", playerChunk.x(), playerChunk.z());

        // Recalculer les chunks cibles
        targetChunks = calculateTargetChunks(playerChunk);

        // Mettre à jour les files d'attente
        updateLoadQueue(playerChunk);
        updateUnloadQueue(playerChunk);

        logger.debug("Target chunks: {}, Load queue: {}, Unload queue: {}",
                targetChunks.size(), loadQueue.size(), unloadQueue.size());
    }

    // ==================== Calcul des chunks cibles ====================

    /**
     * Calcule l'ensemble des chunks qui devraient être chargés autour du joueur.
     * <p>
     * Génère un carré de chunks de taille (2*renderDistance + 1)².
     * </p>
     *
     * @param center le chunk central (position du joueur)
     * @return un set de coordonnées de chunks à charger
     */
    private Set<ChunkCoordinate> calculateTargetChunks(ChunkCoordinate center) {
        Set<ChunkCoordinate> targets = new HashSet<>();

        for (int dx = -renderDistance; dx <= renderDistance; dx++) {
            for (int dz = -renderDistance; dz <= renderDistance; dz++) {
                targets.add(new ChunkCoordinate(center.x() + dx, center.z() + dz));
            }
        }

        return targets;
    }

    // ==================== Gestion de la file de chargement ====================

    /**
     * Met à jour la file des chunks à charger.
     * <p>
     * Ajoute les chunks cibles qui ne sont pas encore chargés,
     * triés par distance au joueur (spirale du centre vers l'extérieur).
     * </p>
     *
     * @param playerChunk position du chunk du joueur
     */
    private void updateLoadQueue(ChunkCoordinate playerChunk) {
        loadQueue.clear();

        // Trouver les chunks à charger (dans targetChunks mais pas dans world)
        List<ChunkCoordinate> toLoad = new ArrayList<>();
        for (ChunkCoordinate target : targetChunks) {
            if (!world.hasChunk(target.x(), target.z())) {
                toLoad.add(target);
            }
        }

        // Trier par distance au joueur (chunks les plus proches en premier)
        toLoad.sort((a, b) -> {
            int distA = manhattanDistance(playerChunk, a);
            int distB = manhattanDistance(playerChunk, b);
            return Integer.compare(distA, distB);
        });

        loadQueue.addAll(toLoad);

        if (!loadQueue.isEmpty()) {
            logger.debug("Load queue updated: {} chunks to load", loadQueue.size());
        }
    }

    /**
     * Traite la file de chargement (charge jusqu'à N chunks par frame).
     */
    private void processLoadQueue() {
        int loadedThisFrame = 0;

        while (!loadQueue.isEmpty() && loadedThisFrame < MAX_CHUNKS_TO_LOAD_PER_FRAME) {
            ChunkCoordinate coord = loadQueue.poll();

            // Vérifier que le chunk n'a pas déjà été chargé entre-temps
            if (!world.hasChunk(coord.x(), coord.z())) {
                // Charger le chunk (cela déclenche la génération automatique)
                world.getChunk(coord.x(), coord.z());
                loadedThisFrame++;

                onChunkLoaded(coord);

                logger.trace("Loaded chunk ({}, {})", coord.x(), coord.z());
            }
        }

        if (loadedThisFrame > 0) {
            logger.debug("Loaded {} chunks this frame (queue: {})",
                    loadedThisFrame, loadQueue.size());
        }
    }

    private void onChunkLoaded(ChunkCoordinate coord) {
        if (chunkRenderer != null) {
            chunkRenderer.onChunkLoaded(coord.x(), coord.z());
        }
    }

    // ==================== Gestion de la file de déchargement ====================

    /**
     * Met à jour la file des chunks à décharger.
     * <p>
     * Ajoute les chunks actuellement chargés qui ne sont plus dans
     * la zone de render (targetChunks).
     * </p>
     *
     * @param playerChunk position du chunk du joueur
     */
    private void updateUnloadQueue(ChunkCoordinate playerChunk) {
        unloadQueue.clear();

        // Parcourir tous les chunks chargés
        Collection<Chunk> loadedChunks = world.getLoadedChunks();

        for (Chunk chunk : loadedChunks) {
            ChunkCoordinate coord = new ChunkCoordinate(chunk.getChunkX(), chunk.getChunkZ());

            // Décharger uniquement si le chunk n'est plus dans les chunks cibles
            if (!targetChunks.contains(coord)) {
                unloadQueue.add(coord);
            }
        }

        if (!unloadQueue.isEmpty()) {
            logger.debug("Unload queue updated: {} chunks to unload", unloadQueue.size());
        }
    }

    /**
     * Traite la file de déchargement (décharge jusqu'à N chunks par frame).
     */
    private void processUnloadQueue() {
        // Ne décharger QUE si le chargement est terminé
        if (!loadQueue.isEmpty()) {
            logger.trace("Skipping unload: {} chunks still loading", loadQueue.size());
            return;
        }

        int unloadedThisFrame = 0;

        while (!unloadQueue.isEmpty() && unloadedThisFrame < MAX_CHUNKS_TO_UNLOAD_PER_FRAME) {
            ChunkCoordinate coord = unloadQueue.poll();

            // Décharger le chunk du monde
            world.removeChunk(coord.x(), coord.z());
            unloadedThisFrame++;

            logger.trace("Unloaded chunk ({}, {})", coord.x(), coord.z());
        }

        if (unloadedThisFrame > 0) {
            logger.debug("Unloaded {} chunks this frame (queue: {})",
                    unloadedThisFrame, unloadQueue.size());
        }
    }

    // ==================== Utilitaires ====================

    /**
     * Calcule la distance de Manhattan entre deux chunks.
     * <p>
     * Distance de Manhattan = |x1 - x2| + |z1 - z2|
     * </p>
     *
     * @param a premier chunk
     * @param b second chunk
     * @return la distance en chunks
     */
    private int manhattanDistance(ChunkCoordinate a, ChunkCoordinate b) {
        return Math.abs(a.x() - b.x()) + Math.abs(a.z() - b.z());
    }

    // ==================== Chargement initial ====================

    /**
     * Charge initialement les chunks autour d'une position.
     * <p>
     * À appeler au démarrage du jeu pour charger les premiers chunks
     * avant de commencer la boucle de jeu.
     * </p>
     *
     * @param startPosition la position de départ (spawn)
     */
    public void loadInitialChunks(Vector3f startPosition) {
        logger.info("Loading initial chunks around position ({}, {}, {})",
                startPosition.x, startPosition.y, startPosition.z);

        int playerChunkX = (int) Math.floor(startPosition.x / 16);
        int playerChunkZ = (int) Math.floor(startPosition.z / 16);
        ChunkCoordinate playerChunk = new ChunkCoordinate(playerChunkX, playerChunkZ);

        // Calculer et charger tous les chunks cibles
        targetChunks = calculateTargetChunks(playerChunk);
        lastPlayerChunk = playerChunk;

        int loadedCount = 0;
        for (ChunkCoordinate coord : targetChunks) {
            world.getChunk(coord.x(), coord.z());
            loadedCount++;
        }

        logger.info("Loaded {} initial chunks", loadedCount);
    }

    // ==================== Getters / Setters ====================

    /**
     * Retourne la distance de rendu actuelle.
     *
     * @return la render distance en chunks
     */
    public int getRenderDistance() {
        return renderDistance;
    }

    /**
     * Définit une nouvelle distance de rendu.
     * <p>
     * Force une mise à jour immédiate du chargement des chunks.
     * </p>
     *
     * @param renderDistance la nouvelle distance (1-32)
     */
    public void setRenderDistance(int renderDistance) {
        if (renderDistance < 1 || renderDistance > 32) {
            throw new IllegalArgumentException("Render distance must be between 1 and 32");
        }

        if (this.renderDistance != renderDistance) {
            logger.info("Render distance changed: {} -> {}", this.renderDistance, renderDistance);
            this.renderDistance = renderDistance;
            this.unloadDistance = renderDistance + DEFAULT_UNLOAD_DISTANCE_BUFFER;

            // Forcer une mise à jour si le joueur est déjà positionné
            if (lastPlayerChunk != null) {
                onPlayerChunkChanged(lastPlayerChunk);
            }
        }
    }

    /**
     * Retourne le nombre de chunks actuellement dans la file de chargement.
     *
     * @return la taille de la load queue
     */
    public int getLoadQueueSize() {
        return loadQueue.size();
    }

    /**
     * Retourne le nombre de chunks actuellement dans la file de déchargement.
     *
     * @return la taille de l'unload queue
     */
    public int getUnloadQueueSize() {
        return unloadQueue.size();
    }

    /**
     * Retourne le nombre total de chunks qui devraient être chargés.
     *
     * @return la taille du set de chunks cibles
     */
    public int getTargetChunkCount() {
        return targetChunks.size();
    }
}
