package org.almostcraft.world;

import org.almostcraft.world.block.BlockRegistry;
import org.almostcraft.world.block.BlockType;
import org.almostcraft.world.chunk.Chunk;
import org.almostcraft.world.generation.TerrainGenerator;
import org.almostcraft.world.utils.CoordinateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Représente le monde du jeu, composé de multiples chunks.
 * <p>
 * Le monde gère la création, le chargement et l'accès aux chunks,
 * ainsi qu'une API unifiée pour accéder aux blocs via des coordonnées mondiales.
 * </p>
 * <p>
 * Thread-safe : utilise ConcurrentHashMap pour permettre l'accès concurrent.
 * </p>
 *
 * @author Lucas Préaux
 * @version 1.0
 */
public class World {

    private static final Logger logger = LoggerFactory.getLogger(World.class);

    // ==================== Attributs ====================

    /**
     * Map des chunks chargés en mémoire, indexés par leurs coordonnées.
     */
    private final Map<ChunkCoordinate, Chunk> chunks = new ConcurrentHashMap<>();

    private final TerrainGenerator terrainGenerator;

    /**
     * Référence au registre de blocs pour la conversion ID ↔ BlockType.
     */
    private final BlockRegistry blockRegistry;

    // ==================== Constructeur ====================

    /**
     * Crée un nouveau monde avec le registre de blocs spécifié.
     *
     * @param blockRegistry le registre de blocs à utiliser
     * @throws IllegalArgumentException si terrainGenerator ou blockRegistry sont null
     */
    public World(TerrainGenerator terrainGenerator, BlockRegistry blockRegistry) {
        if (terrainGenerator == null) {
            throw new IllegalArgumentException("TerrainGenerator cannot be null");
        }
        if (blockRegistry == null) {
            throw new IllegalArgumentException("BlockRegistry cannot be null");
        }
        this.terrainGenerator = terrainGenerator;
        this.blockRegistry = blockRegistry;
        logger.info("World created");
    }

    // ==================== Gestion des chunks ====================

    /**
     * Récupère un chunk à la position spécifiée.
     * <p>
     * Si le chunk n'existe pas, il est créé automatiquement (lazy loading).
     * </p>
     *
     * @param chunkX coordonnée X du chunk
     * @param chunkZ coordonnée Z du chunk
     * @return le chunk à cette position (jamais null)
     */
    public Chunk getChunk(int chunkX, int chunkZ) {
        return chunks.computeIfAbsent(
                new ChunkCoordinate(chunkX, chunkZ),
                coord -> {
                    logger.debug("Creating new chunk at ({}, {})", chunkX, chunkZ);
                    Chunk chunk = new Chunk(coord.x(), coord.z());
                    terrainGenerator.generate(chunk, coord.x(), coord.z());
                    return chunk;
                }
        );
    }

    /**
     * Vérifie si un chunk existe à la position spécifiée.
     *
     * @param chunkX coordonnée X du chunk
     * @param chunkZ coordonnée Z du chunk
     * @return true si le chunk est chargé en mémoire, false sinon
     */
    public boolean hasChunk(int chunkX, int chunkZ) {
        return chunks.containsKey(new ChunkCoordinate(chunkX, chunkZ));
    }

    /**
     * Récupère ou crée un chunk à la position spécifiée.
     * <p>
     * Alias explicite de {@link #getChunk(int, int)}.
     * </p>
     *
     * @param chunkX coordonnée X du chunk
     * @param chunkZ coordonnée Z du chunk
     * @return le chunk à cette position (jamais null)
     */
    public Chunk getOrCreateChunk(int chunkX, int chunkZ) {
        return getChunk(chunkX, chunkZ);
    }

    /**
     * Supprime un chunk de la mémoire.
     * <p>
     * Utilisé pour le déchargement de chunks (unloading).
     * </p>
     *
     * @param chunkX coordonnée X du chunk
     * @param chunkZ coordonnée Z du chunk
     * @return le chunk supprimé, ou null s'il n'existait pas
     */
    public Chunk removeChunk(int chunkX, int chunkZ) {
        Chunk removed = chunks.remove(new ChunkCoordinate(chunkX, chunkZ));
        if (removed != null) {
            logger.debug("Removed chunk at ({}, {})", chunkX, chunkZ);
        }
        return removed;
    }

    // ==================== Accès aux blocs (API unifiée) ====================

    /**
     * Récupère l'ID du bloc aux coordonnées mondiales spécifiées.
     * <p>
     * Crée automatiquement le chunk si nécessaire.
     * </p>
     *
     * @param worldX coordonnée X mondiale
     * @param worldY coordonnée Y mondiale (0-255)
     * @param worldZ coordonnée Z mondiale
     * @return l'ID numérique du bloc
     * @throws IllegalArgumentException si worldY est hors limites
     */
    public int getBlockAt(int worldX, int worldY, int worldZ) {
        // Convertir world → chunk coordinates
        int chunkX = CoordinateUtil.worldToChunkX(worldX);
        int chunkZ = CoordinateUtil.worldToChunkZ(worldZ);

        // Récupérer ou créer le chunk
        Chunk chunk = getChunk(chunkX, chunkZ);

        // Convertir world → local coordinates
        int localX = CoordinateUtil.worldToLocalX(worldX);
        int localY = CoordinateUtil.worldToLocalY(worldY);  // Valide et lance exception si invalide
        int localZ = CoordinateUtil.worldToLocalZ(worldZ);

        return chunk.getVoxel(localX, localY, localZ);
    }

    /**
     * Définit le bloc aux coordonnées mondiales spécifiées.
     * <p>
     * Crée automatiquement le chunk si nécessaire.
     * </p>
     *
     * @param worldX  coordonnée X mondiale
     * @param worldY  coordonnée Y mondiale (0-255)
     * @param worldZ  coordonnée Z mondiale
     * @param blockId l'ID numérique du bloc à placer
     * @throws IllegalArgumentException si worldY ou blockId sont invalides
     */
    public void setBlockAt(int worldX, int worldY, int worldZ, int blockId) {
        // Convertir world → chunk coordinates
        int chunkX = CoordinateUtil.worldToChunkX(worldX);
        int chunkZ = CoordinateUtil.worldToChunkZ(worldZ);

        // Récupérer ou créer le chunk
        Chunk chunk = getChunk(chunkX, chunkZ);

        // Convertir world → local coordinates
        int localX = CoordinateUtil.worldToLocalX(worldX);
        int localY = CoordinateUtil.worldToLocalY(worldY);  // Valide et lance exception si invalide
        int localZ = CoordinateUtil.worldToLocalZ(worldZ);

        chunk.setVoxel(localX, localY, localZ, blockId);
    }

    /**
     * Récupère le BlockType complet aux coordonnées mondiales spécifiées.
     * <p>
     * Méthode de commodité qui combine {@link #getBlockAt(int, int, int)}
     * et {@link BlockRegistry#getBlockByNumericId(int)}.
     * </p>
     *
     * @param worldX coordonnée X mondiale
     * @param worldY coordonnée Y mondiale (0-255)
     * @param worldZ coordonnée Z mondiale
     * @return le BlockType, ou null si l'ID ne correspond à aucun bloc enregistré
     * @throws IllegalArgumentException si worldY est hors limites
     */
    public BlockType getBlockTypeAt(int worldX, int worldY, int worldZ) {
        int blockId = getBlockAt(worldX, worldY, worldZ);
        return blockRegistry.getBlockByNumericId(blockId);
    }

    // ==================== Utilitaires ====================

    /**
     * Retourne tous les chunks actuellement chargés en mémoire.
     *
     * @return une collection immuable des chunks chargés
     */
    public Collection<Chunk> getLoadedChunks() {
        return chunks.values();
    }

    /**
     * Retourne le nombre de chunks actuellement chargés en mémoire.
     *
     * @return le nombre de chunks
     */
    public int getLoadedChunkCount() {
        return chunks.size();
    }

    /**
     * Retourne le générateur de terrain utilisé par ce monde.
     *
     * @return le TerrainGenerator
     */
    public TerrainGenerator getTerrainGenerator() {
        return terrainGenerator;
    }

    /**
     * Retourne le registre de blocs utilisé par ce monde.
     *
     * @return le BlockRegistry
     */
    public BlockRegistry getBlockRegistry() {
        return blockRegistry;
    }
}