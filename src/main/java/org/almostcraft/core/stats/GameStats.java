package org.almostcraft.core.stats;

import org.almostcraft.camera.Camera;
import org.almostcraft.render.chunk.ChunkRenderer;
import org.almostcraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gestionnaire centralisé des statistiques du jeu.
 * <p>
 * Cette classe collecte et agrège diverses métriques de performance et d'état
 * du jeu provenant de différents subsystems :
 * <ul>
 *   <li>FPS et frame time (via {@link FPSCounter})</li>
 *   <li>Statistiques de chunks (nombre chargés, meshes, triangles)</li>
 *   <li>Position et direction du joueur</li>
 *   <li>Utilisation mémoire (optionnel)</li>
 * </ul>
 * </p>
 * <p>
 * Les statistiques peuvent être consultées individuellement ou formatées
 * en une seule chaîne pour l'affichage.
 * </p>
 * <p>
 * Utilisation typique :
 * <pre>{@code
 * GameStats stats = new GameStats();
 *
 * // Dans la boucle de jeu
 * stats.update(currentTime, world, chunkRenderer, camera);
 *
 * if (stats.shouldLog()) {
 *     logger.info(stats.getFormattedStats());
 * }
 * }</pre>
 * </p>
 *
 * @author Lucas Préaux
 * @version 1.0
 */
public class GameStats {

    private static final Logger logger = LoggerFactory.getLogger(GameStats.class);

    // ==================== Constantes ====================

    /**
     * Intervalle de logging par défaut en secondes.
     */
    private static final double DEFAULT_LOG_INTERVAL = 1.0;

    // ==================== Attributs ====================

    /**
     * Compteur de FPS intégré.
     */
    private final FPSCounter fpsCounter;

    /**
     * Intervalle de logging des statistiques.
     */
    private final double logInterval;

    // ==================== Statistiques de chunks ====================

    /**
     * Nombre de chunks actuellement chargés en mémoire.
     */
    private int loadedChunks;

    /**
     * Nombre de meshes en cache.
     */
    private int cachedMeshes;

    /**
     * Nombre total de triangles à rendre.
     */
    private int totalTriangles;

    /**
     * Nombre de chunks en attente de génération de mesh.
     */
    private int dirtyChunks;

    // ==================== Statistiques du joueur ====================

    /**
     * Position X du joueur.
     */
    private float playerX;

    /**
     * Position Y du joueur.
     */
    private float playerY;

    /**
     * Position Z du joueur.
     */
    private float playerZ;

    /**
     * Direction cardinale du joueur (N, S, E, W).
     */
    private String playerDirection;

    // ==================== Statistiques mémoire ====================

    /**
     * Mémoire utilisée en MB.
     */
    private long usedMemoryMB;

    /**
     * Mémoire totale allouée en MB.
     */
    private long totalMemoryMB;

    /**
     * Indique si les statistiques mémoire sont activées.
     */
    private boolean memoryStatsEnabled;

    // ==================== Constructeur ====================

    /**
     * Crée un nouveau gestionnaire de statistiques avec intervalle par défaut.
     */
    public GameStats() {
        this(DEFAULT_LOG_INTERVAL);
    }

    /**
     * Crée un nouveau gestionnaire de statistiques avec intervalle personnalisé.
     *
     * @param logInterval intervalle de logging en secondes
     */
    public GameStats(double logInterval) {
        this.fpsCounter = new FPSCounter();
        this.logInterval = logInterval;
        this.memoryStatsEnabled = false;

        logger.debug("GameStats created with log interval: {}s", logInterval);
    }

    // ==================== Mise à jour ====================

    /**
     * Met à jour toutes les statistiques du jeu.
     * <p>
     * Cette méthode doit être appelée une fois par frame. Elle collecte
     * les données de tous les subsystems et met à jour le compteur de FPS.
     * </p>
     *
     * @param currentTime   temps actuel en secondes
     * @param world         le monde (pour les stats de chunks)
     * @param chunkRenderer le renderer (pour les stats de rendu)
     * @param camera        la caméra (pour la position du joueur)
     */
    public void update(double currentTime, World world, ChunkRenderer chunkRenderer, Camera camera) {
        // Mettre à jour le compteur de FPS
        fpsCounter.update(currentTime);

        // Collecter les statistiques de chunks
        if (world != null) {
            loadedChunks = world.getLoadedChunkCount();
        }

        if (chunkRenderer != null) {
            cachedMeshes = chunkRenderer.getTotalMeshesGenerated();
            totalTriangles = chunkRenderer.getRenderedTriangleCount();
            dirtyChunks = chunkRenderer.getDirtyChunkCount();
        }

        // Collecter les statistiques du joueur
        if (camera != null) {
            playerX = camera.getPosition().x;
            playerY = camera.getPosition().y;
            playerZ = camera.getPosition().z;
            playerDirection = camera.getCardinalDirection().getAbbreviation();
        }

        // Collecter les statistiques mémoire si activées
        if (memoryStatsEnabled) {
            updateMemoryStats();
        }
    }

    /**
     * Mise à jour simplifiée sans les composants optionnels.
     *
     * @param currentTime temps actuel en secondes
     */
    public void update(double currentTime) {
        update(currentTime, null, null, null);
    }

    /**
     * Met à jour les statistiques mémoire.
     */
    private void updateMemoryStats() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        usedMemoryMB = usedMemory / (1024 * 1024);
        totalMemoryMB = totalMemory / (1024 * 1024);
    }

    // ==================== Getters - FPS ====================

    /**
     * Retourne le nombre de FPS actuel.
     *
     * @return les frames par seconde
     */
    public int getFPS() {
        return fpsCounter.getFPS();
    }

    /**
     * Retourne le temps moyen par frame en millisecondes.
     *
     * @return le frame time en ms
     */
    public double getFrameTime() {
        return fpsCounter.getFrameTime();
    }

    /**
     * Retourne le compteur de FPS pour un accès direct.
     *
     * @return le FPSCounter
     */
    public FPSCounter getFpsCounter() {
        return fpsCounter;
    }

    // ==================== Getters - Chunks ====================

    /**
     * Retourne le nombre de chunks chargés.
     *
     * @return le nombre de chunks en mémoire
     */
    public int getLoadedChunks() {
        return loadedChunks;
    }

    /**
     * Retourne le nombre de meshes en cache.
     *
     * @return le nombre de meshes
     */
    public int getCachedMeshes() {
        return cachedMeshes;
    }

    /**
     * Retourne le nombre total de triangles à rendre.
     *
     * @return le nombre de triangles
     */
    public int getTotalTriangles() {
        return totalTriangles;
    }

    /**
     * Retourne le nombre de triangles en milliers.
     *
     * @return les triangles en K
     */
    public int getTotalTrianglesK() {
        return totalTriangles / 1000;
    }

    /**
     * Retourne le nombre de chunks en attente de mesh.
     *
     * @return le nombre de chunks dirty
     */
    public int getDirtyChunks() {
        return dirtyChunks;
    }

    // ==================== Getters - Joueur ====================

    /**
     * Retourne la position X du joueur.
     *
     * @return la coordonnée X
     */
    public float getPlayerX() {
        return playerX;
    }

    /**
     * Retourne la position Y du joueur.
     *
     * @return la coordonnée Y
     */
    public float getPlayerY() {
        return playerY;
    }

    /**
     * Retourne la position Z du joueur.
     *
     * @return la coordonnée Z
     */
    public float getPlayerZ() {
        return playerZ;
    }

    /**
     * Retourne la direction cardinale du joueur.
     *
     * @return "N", "S", "E", ou "W"
     */
    public String getPlayerDirection() {
        return playerDirection;
    }

    // ==================== Getters - Mémoire ====================

    /**
     * Retourne la mémoire utilisée en MB.
     *
     * @return la mémoire utilisée
     */
    public long getUsedMemoryMB() {
        return usedMemoryMB;
    }

    /**
     * Retourne la mémoire totale allouée en MB.
     *
     * @return la mémoire totale
     */
    public long getTotalMemoryMB() {
        return totalMemoryMB;
    }

    /**
     * Active ou désactive la collecte des statistiques mémoire.
     * <p>
     * Désactivé par défaut car l'appel à Runtime peut avoir un léger impact.
     * </p>
     *
     * @param enabled true pour activer, false pour désactiver
     */
    public void setMemoryStatsEnabled(boolean enabled) {
        this.memoryStatsEnabled = enabled;
        logger.debug("Memory stats {}", enabled ? "enabled" : "disabled");
    }

    /**
     * Vérifie si les statistiques mémoire sont activées.
     *
     * @return true si activées, false sinon
     */
    public boolean isMemoryStatsEnabled() {
        return memoryStatsEnabled;
    }

    // ==================== Logging ====================

    /**
     * Vérifie s'il est temps de logger les statistiques.
     * <p>
     * Retourne true une fois par intervalle (par défaut chaque seconde),
     * synchronisé avec le FPSCounter.
     * </p>
     *
     * @return true si un nouveau log doit être effectué, false sinon
     */
    public boolean shouldLog() {
        return fpsCounter.hasNewSecond();
    }

    /**
     * Retourne une chaîne formatée avec toutes les statistiques principales.
     * <p>
     * Format : "FPS: 60, Chunks: 81, Meshes: 81, Triangles: 523K, Pos: (8.0, 70.0, 8.0), Direction: N"
     * </p>
     *
     * @return une chaîne contenant les statistiques
     */
    public String getFormattedStats() {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("FPS: %d", getFPS()));
        sb.append(String.format(", Chunks: %d", loadedChunks));
        sb.append(String.format(", Meshes: %d", cachedMeshes));
        sb.append(String.format(", Triangles: %dK", getTotalTrianglesK()));
        sb.append(String.format(", Pos: (%.1f, %.1f, %.1f)", playerX, playerY, playerZ));
        sb.append(String.format(", Direction: %s", playerDirection));

        if (dirtyChunks > 0) {
            sb.append(String.format(", Dirty: %d", dirtyChunks));
        }

        if (memoryStatsEnabled) {
            sb.append(String.format(", Mem: %d/%dMB", usedMemoryMB, totalMemoryMB));
        }

        return sb.toString();
    }

    /**
     * Retourne une chaîne formatée détaillée avec toutes les statistiques.
     * <p>
     * Inclut des informations supplémentaires comme le frame time.
     * </p>
     *
     * @return une chaîne détaillée
     */
    public String getDetailedStats() {
        StringBuilder sb = new StringBuilder();

        sb.append("=== Game Statistics ===\n");
        sb.append(String.format("FPS: %d (%.2fms per frame)\n", getFPS(), getFrameTime()));
        sb.append(String.format("Chunks: %d loaded, %d meshes cached", loadedChunks, cachedMeshes));

        if (dirtyChunks > 0) {
            sb.append(String.format(", %d dirty", dirtyChunks));
        }
        sb.append("\n");

        sb.append(String.format("Triangles: %,d (%dK)\n", totalTriangles, getTotalTrianglesK()));
        sb.append(String.format("Player: (%.1f, %.1f, %.1f) facing %s\n",
                playerX, playerY, playerZ, playerDirection));

        if (memoryStatsEnabled) {
            sb.append(String.format("Memory: %dMB used / %dMB allocated (%.1f%%)\n",
                    usedMemoryMB, totalMemoryMB,
                    (double) usedMemoryMB / totalMemoryMB * 100));
        }

        return sb.toString();
    }

    // ==================== Utilitaires ====================

    /**
     * Réinitialise toutes les statistiques.
     */
    public void reset() {
        fpsCounter.reset();
        loadedChunks = 0;
        cachedMeshes = 0;
        totalTriangles = 0;
        dirtyChunks = 0;
        playerX = 0;
        playerY = 0;
        playerZ = 0;
        playerDirection = "N";
        usedMemoryMB = 0;
        totalMemoryMB = 0;

        logger.debug("GameStats reset");
    }

    /**
     * Vérifie si les performances sont critiques.
     *
     * @param minFPS le seuil minimum de FPS
     * @return true si sous le seuil, false sinon
     */
    public boolean isPerformanceCritical(int minFPS) {
        return fpsCounter.isUnderPerforming(minFPS);
    }

    @Override
    public String toString() {
        return String.format("GameStats[fps=%d, chunks=%d, meshes=%d, triangles=%dK]",
                getFPS(), loadedChunks, cachedMeshes, getTotalTrianglesK());
    }
}
