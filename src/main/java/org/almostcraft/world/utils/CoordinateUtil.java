package org.almostcraft.world.utils;

import org.almostcraft.world.chunk.ChunkCoordinate;
import org.almostcraft.world.chunk.Chunk;

/**
 * Classe utilitaire pour les conversions de coordonnées entre les différents
 * systèmes de référence du monde voxel.
 * <p>
 * Gère les conversions entre :
 * <ul>
 *   <li>Coordonnées mondiales (world coordinates) : position absolue dans le monde</li>
 *   <li>Coordonnées de chunk : identifie quel chunk contient une position</li>
 *   <li>Coordonnées locales : position relative à l'intérieur d'un chunk (0-15 pour X/Z, 0-255 pour Y)</li>
 * </ul>
 * </p>
 * <p>
 * Cette classe ne peut pas être instanciée.
 * </p>
 *
 * @author Lucas Préaux
 * @version 1.0
 */
public class CoordinateUtil {

    // ==================== Constructeur privé ====================

    /**
     * Constructeur privé pour empêcher l'instanciation.
     * <p>
     * Cette classe ne contient que des méthodes utilitaires statiques.
     * </p>
     */
    private CoordinateUtil() {
        throw new AssertionError("CoordinateUtil cannot be instantiated");
    }

    // ==================== World → Chunk ====================

    /**
     * Convertit une coordonnée X mondiale en coordonnée de chunk.
     * <p>
     * Utilise {@link Math#floorDiv(int, int)} pour gérer correctement
     * les coordonnées négatives.
     * </p>
     * <p>
     * Exemples :
     * <ul>
     *   <li>worldX = 0 → chunkX = 0</li>
     *   <li>worldX = 15 → chunkX = 0</li>
     *   <li>worldX = 16 → chunkX = 1</li>
     *   <li>worldX = -1 → chunkX = -1</li>
     *   <li>worldX = -16 → chunkX = -1</li>
     *   <li>worldX = -17 → chunkX = -2</li>
     * </ul>
     * </p>
     *
     * @param worldX la coordonnée X mondiale
     * @return la coordonnée X du chunk contenant cette position
     */
    public static int worldToChunkX(int worldX) {
        return Math.floorDiv(worldX, Chunk.WIDTH);
    }

    /**
     * Convertit une coordonnée Z mondiale en coordonnée de chunk.
     * <p>
     * Utilise {@link Math#floorDiv(int, int)} pour gérer correctement
     * les coordonnées négatives.
     * </p>
     * <p>
     * Exemples :
     * <ul>
     *   <li>worldZ = 0 → chunkZ = 0</li>
     *   <li>worldZ = 15 → chunkZ = 0</li>
     *   <li>worldZ = 16 → chunkZ = 1</li>
     *   <li>worldZ = -1 → chunkZ = -1</li>
     *   <li>worldZ = -16 → chunkZ = -1</li>
     *   <li>worldZ = -17 → chunkZ = -2</li>
     * </ul>
     * </p>
     *
     * @param worldZ la coordonnée Z mondiale
     * @return la coordonnée Z du chunk contenant cette position
     */
    public static int worldToChunkZ(int worldZ) {
        return Math.floorDiv(worldZ, Chunk.DEPTH);
    }

    /**
     * Convertit des coordonnées mondiales X et Z en ChunkCoordinate.
     * <p>
     * Méthode de commodité qui combine {@link #worldToChunkX(int)}
     * et {@link #worldToChunkZ(int)}.
     * </p>
     *
     * @param worldX coordonnée X mondiale
     * @param worldZ coordonnée Z mondiale
     * @return les coordonnées du chunk contenant cette position
     */
    public static ChunkCoordinate worldToChunk(int worldX, int worldZ) {
        return new ChunkCoordinate(
                worldToChunkX(worldX),
                worldToChunkZ(worldZ)
        );
    }

    // ==================== World → Local ====================

    /**
     * Convertit une coordonnée X mondiale en coordonnée X locale (0-15).
     * <p>
     * Utilise {@link Math#floorMod(int, int)} pour gérer correctement
     * les coordonnées négatives.
     * </p>
     * <p>
     * Exemples :
     * <ul>
     *   <li>worldX = 0 → localX = 0</li>
     *   <li>worldX = 15 → localX = 15</li>
     *   <li>worldX = 16 → localX = 0</li>
     *   <li>worldX = 17 → localX = 1</li>
     *   <li>worldX = -1 → localX = 15</li>
     *   <li>worldX = -16 → localX = 0</li>
     *   <li>worldX = -17 → localX = 15</li>
     * </ul>
     * </p>
     *
     * @param worldX la coordonnée X mondiale
     * @return la coordonnée X locale dans le chunk (0-15)
     */
    public static int worldToLocalX(int worldX) {
        return Math.floorMod(worldX, Chunk.WIDTH);
    }

    /**
     * Convertit une coordonnée Y mondiale en coordonnée Y locale.
     * <p>
     * Note : Les coordonnées Y sont identiques entre le système mondial
     * et local car il n'y a pas de chunks verticaux dans ce moteur.
     * Cette méthode valide simplement que Y est dans les limites (0-255).
     * </p>
     *
     * @param worldY la coordonnée Y mondiale
     * @return la coordonnée Y locale (identique à worldY)
     * @throws IllegalArgumentException si worldY est hors limites (< 0 ou >= 256)
     */
    public static int worldToLocalY(int worldY) {
        if (worldY < 0 || worldY >= Chunk.HEIGHT) {
            throw new IllegalArgumentException(
                    String.format("World Y coordinate %d is out of bounds (0-%d)",
                            worldY, Chunk.HEIGHT - 1)
            );
        }
        return worldY;
    }

    /**
     * Convertit une coordonnée Z mondiale en coordonnée Z locale (0-15).
     * <p>
     * Utilise {@link Math#floorMod(int, int)} pour gérer correctement
     * les coordonnées négatives.
     * </p>
     * <p>
     * Exemples :
     * <ul>
     *   <li>worldZ = 0 → localZ = 0</li>
     *   <li>worldZ = 15 → localZ = 15</li>
     *   <li>worldZ = 16 → localZ = 0</li>
     *   <li>worldZ = 17 → localZ = 1</li>
     *   <li>worldZ = -1 → localZ = 15</li>
     *   <li>worldZ = -16 → localZ = 0</li>
     *   <li>worldZ = -17 → localZ = 15</li>
     * </ul>
     * </p>
     *
     * @param worldZ la coordonnée Z mondiale
     * @return la coordonnée Z locale dans le chunk (0-15)
     */
    public static int worldToLocalZ(int worldZ) {
        return Math.floorMod(worldZ, Chunk.DEPTH);
    }

    // ==================== Chunk → World ====================

    /**
     * Convertit une coordonnée X de chunk en coordonnée X mondiale.
     * <p>
     * Retourne la coordonnée X mondiale du coin inférieur gauche du chunk
     * (bloc à la position locale 0 du chunk).
     * </p>
     * <p>
     * Exemples :
     * <ul>
     *   <li>chunkX = 0 → worldX = 0</li>
     *   <li>chunkX = 1 → worldX = 16</li>
     *   <li>chunkX = -1 → worldX = -16</li>
     *   <li>chunkX = -2 → worldX = -32</li>
     * </ul>
     * </p>
     *
     * @param chunkX la coordonnée X du chunk
     * @return la coordonnée X mondiale du coin du chunk
     */
    public static int chunkToWorldX(int chunkX) {
        return chunkX * Chunk.WIDTH;
    }

    /**
     * Convertit une coordonnée Z de chunk en coordonnée Z mondiale.
     * <p>
     * Retourne la coordonnée Z mondiale du coin inférieur gauche du chunk
     * (bloc à la position locale 0 du chunk).
     * </p>
     * <p>
     * Exemples :
     * <ul>
     *   <li>chunkZ = 0 → worldZ = 0</li>
     *   <li>chunkZ = 1 → worldZ = 16</li>
     *   <li>chunkZ = -1 → worldZ = -16</li>
     *   <li>chunkZ = -2 → worldZ = -32</li>
     * </ul>
     * </p>
     *
     * @param chunkZ la coordonnée Z du chunk
     * @return la coordonnée Z mondiale du coin du chunk
     */
    public static int chunkToWorldZ(int chunkZ) {
        return chunkZ * Chunk.DEPTH;
    }

    // ==================== Validation ====================

    /**
     * Vérifie si une coordonnée Y mondiale est valide.
     * <p>
     * Une coordonnée Y est valide si elle est comprise entre 0 (inclus)
     * et {@link Chunk#HEIGHT} (exclus).
     * </p>
     *
     * @param worldY la coordonnée Y à vérifier
     * @return true si valide (0-255), false sinon
     */
    public static boolean isValidWorldY(int worldY) {
        return worldY >= 0 && worldY < Chunk.HEIGHT;
    }

    /**
     * Vérifie si des coordonnées locales sont valides pour un chunk.
     *
     * @param localX coordonnée X locale
     * @param localY coordonnée Y locale
     * @param localZ coordonnée Z locale
     * @return true si toutes les coordonnées sont dans les limites du chunk
     */
    public static boolean isValidLocal(int localX, int localY, int localZ) {
        return localX >= 0 && localX < Chunk.WIDTH
                && localY >= 0 && localY < Chunk.HEIGHT
                && localZ >= 0 && localZ < Chunk.DEPTH;
    }
}