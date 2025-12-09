package org.almostcraft.world.chunk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Représente un chunk de 16×16×256 blocs.
 * <p>
 * Un chunk est l'unité de base de stockage du monde. Il contient les IDs
 * numériques des blocs dans un tableau 1D compact pour optimiser la mémoire
 * et les performances.
 * </p>
 * <p>
 * Système de coordonnées locales :
 * <ul>
 *   <li>X : 0-15 (largeur, axe ouest-est)</li>
 *   <li>Y : 0-255 (hauteur, axe bas-haut)</li>
 *   <li>Z : 0-15 (profondeur, axe nord-sud)</li>
 * </ul>
 * </p>
 * <p>
 * Utilisation typique :
 * <pre>{@code
 * Chunk chunk = new Chunk(0, 0);
 *
 * // Placer un bloc de pierre
 * int stoneId = registry.getNumericId("almostcraft:stone");
 * chunk.setVoxel(8, 64, 8, stoneId);
 *
 * // Lire un bloc
 * int blockId = chunk.getVoxel(8, 64, 8);
 * BlockType block = registry.getBlockByNumericId(blockId);
 * }</pre>
 * </p>
 *
 * @author Lucas Préaux
 * @version 1.0
 */
public class Chunk {

    private static final Logger logger = LoggerFactory.getLogger(Chunk.class);

    // ==================== Constantes de dimensions ====================

    /**
     * Largeur du chunk en blocs (axe X).
     */
    public static final int WIDTH = 16;

    /**
     * Profondeur du chunk en blocs (axe Z).
     */
    public static final int DEPTH = 16;

    /**
     * Hauteur du chunk en blocs (axe Y).
     */
    public static final int HEIGHT = 256;

    /**
     * Nombre total de voxels dans un chunk (16 × 16 × 256 = 65 536).
     */
    public static final int TOTAL_VOXELS = WIDTH * DEPTH * HEIGHT;

    // ==================== Attributs ====================

    /**
     * Position X du chunk dans le monde (coordonnées chunk).
     */
    private final int chunkX;

    /**
     * Position Z du chunk dans le monde (coordonnées chunk).
     */
    private final int chunkZ;

    /**
     * Tableau de stockage des IDs de blocs.
     * <p>
     * Utilise un tableau 1D pour optimiser la mémoire et la localité du cache.
     * L'indexation se fait via la formule : index = x + z * WIDTH + y * WIDTH * DEPTH
     * </p>
     */
    private final short[] voxels;

    /**
     * Indique si le chunk a été modifié depuis la dernière sauvegarde.
     */
    private boolean modified;

    /**
     * Indique si le chunk a été complètement généré.
     */
    private boolean generated;

    // ==================== Constructeur ====================

    /**
     * Crée un nouveau chunk vide à la position spécifiée.
     * <p>
     * Tous les voxels sont initialisés à 0 (AIR par défaut).
     * </p>
     *
     * @param chunkX la position X du chunk dans le monde
     * @param chunkZ la position Z du chunk dans le monde
     */
    public Chunk(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.voxels = new short[TOTAL_VOXELS];
        this.modified = false;
        this.generated = false;

        logger.debug("Created chunk at ({}, {}) with {} voxels", chunkX, chunkZ, TOTAL_VOXELS);
    }

    // ==================== Getters/Setters de voxels ====================

    /**
     * Récupère l'ID du bloc à la position locale spécifiée.
     *
     * @param x coordonnée X locale (0-15)
     * @param y coordonnée Y locale (0-255)
     * @param z coordonnée Z locale (0-15)
     * @return l'ID numérique du bloc
     * @throws IndexOutOfBoundsException si les coordonnées sont hors limites
     */
    public int getVoxel(int x, int y, int z) {
        validateCoordinates(x, y, z);
        int index = toIndex(x, y, z);
        return voxels[index] & 0xFFFF;  // Conversion short → int positif
    }

    /**
     * Définit l'ID du bloc à la position locale spécifiée.
     *
     * @param x       coordonnée X locale (0-15)
     * @param y       coordonnée Y locale (0-255)
     * @param z       coordonnée Z locale (0-15)
     * @param blockId l'ID numérique du bloc (0-65535)
     * @throws IndexOutOfBoundsException si les coordonnées sont hors limites
     * @throws IllegalArgumentException  si l'ID du bloc est invalide
     */
    public void setVoxel(int x, int y, int z, int blockId) {
        validateCoordinates(x, y, z);
        validateBlockId(blockId);

        int index = toIndex(x, y, z);
        voxels[index] = (short) blockId;
        modified = true;
    }

    // ==================== Remplissage ====================

    /**
     * Remplit tout le chunk avec un type de bloc spécifique.
     * <p>
     * Utile pour l'initialisation ou les tests.
     * </p>
     *
     * @param blockId l'ID numérique du bloc à utiliser
     * @throws IllegalArgumentException si l'ID du bloc est invalide
     */
    public void fill(int blockId) {
        validateBlockId(blockId);

        short blockIdShort = (short) blockId;
        for (int i = 0; i < TOTAL_VOXELS; i++) {
            voxels[i] = blockIdShort;
        }

        modified = true;
        logger.debug("Filled chunk ({}, {}) with block ID {}", chunkX, chunkZ, blockId);
    }

    /**
     * Remplit une couche horizontale (plan XZ) avec un type de bloc.
     * <p>
     * Utile pour la génération de terrain (ex: couche de pierre à Y=50).
     * </p>
     *
     * @param y       la coordonnée Y de la couche (0-255)
     * @param blockId l'ID numérique du bloc
     * @throws IndexOutOfBoundsException si Y est hors limites
     * @throws IllegalArgumentException  si l'ID du bloc est invalide
     */
    public void fillLayer(int y, int blockId) {
        if (y < 0 || y >= HEIGHT) {
            throw new IndexOutOfBoundsException(
                    String.format("Y coordinate %d is out of bounds (0-%d)", y, HEIGHT - 1)
            );
        }
        validateBlockId(blockId);

        short blockIdShort = (short) blockId;
        int startIndex = y * WIDTH * DEPTH;
        int endIndex = startIndex + (WIDTH * DEPTH);

        for (int i = startIndex; i < endIndex; i++) {
            voxels[i] = blockIdShort;
        }

        modified = true;
    }

    // ==================== Conversion de coordonnées ====================

    /**
     * Convertit des coordonnées 3D locales en index 1D dans le tableau.
     * <p>
     * Formule : index = x + z * WIDTH + y * WIDTH * DEPTH
     * </p>
     * <p>
     * Cette formule optimise la localité du cache en stockant les blocs
     * d'une même couche Y de manière contiguë en mémoire.
     * </p>
     *
     * @param x coordonnée X locale (0-15)
     * @param y coordonnée Y locale (0-255)
     * @param z coordonnée Z locale (0-15)
     * @return l'index dans le tableau voxels
     */
    private int toIndex(int x, int y, int z) {
        return x + z * WIDTH + y * WIDTH * DEPTH;
    }

    // ==================== Validation ====================

    /**
     * Valide que les coordonnées locales sont dans les limites du chunk.
     *
     * @param x coordonnée X
     * @param y coordonnée Y
     * @param z coordonnée Z
     * @throws IndexOutOfBoundsException si une coordonnée est hors limites
     */
    private void validateCoordinates(int x, int y, int z) {
        if (x < 0 || x >= WIDTH) {
            throw new IndexOutOfBoundsException(
                    String.format("X coordinate %d is out of bounds (0-%d)", x, WIDTH - 1)
            );
        }
        if (y < 0 || y >= HEIGHT) {
            throw new IndexOutOfBoundsException(
                    String.format("Y coordinate %d is out of bounds (0-%d)", y, HEIGHT - 1)
            );
        }
        if (z < 0 || z >= DEPTH) {
            throw new IndexOutOfBoundsException(
                    String.format("Z coordinate %d is out of bounds (0-%d)", z, DEPTH - 1)
            );
        }
    }

    /**
     * Valide qu'un ID de bloc est dans la plage autorisée.
     *
     * @param blockId l'ID à valider
     * @throws IllegalArgumentException si l'ID est négatif ou trop grand
     */
    private void validateBlockId(int blockId) {
        if (blockId < 0 || blockId > 65535) {
            throw new IllegalArgumentException(
                    String.format("Block ID %d is out of range (0-65535)", blockId)
            );
        }
    }

    // ==================== Getters de position ====================

    /**
     * Retourne la position X du chunk dans le monde.
     *
     * @return la coordonnée X du chunk
     */
    public int getChunkX() {
        return chunkX;
    }

    /**
     * Retourne la position Z du chunk dans le monde.
     *
     * @return la coordonnée Z du chunk
     */
    public int getChunkZ() {
        return chunkZ;
    }

    // ==================== État du chunk ====================

    /**
     * Vérifie si le chunk a été modifié.
     *
     * @return true si le chunk a été modifié, false sinon
     */
    public boolean isModified() {
        return modified;
    }

    /**
     * Marque le chunk comme non modifié.
     * <p>
     * À appeler après la sauvegarde du chunk sur disque.
     * </p>
     */
    public void clearModified() {
        modified = false;
    }

    /**
     * Marque explicitement le chunk comme modifié.
     * <p>
     * Utile si des modifications externes nécessitent une sauvegarde.
     * </p>
     */
    public void markModified() {
        modified = true;
    }

    /**
     * Marque le chunk comme généré.
     * À appeler après la génération du terrain.
     */
    public void markGenerated() {
        this.generated = true;
    }

    /**
     * Vérifie si le chunk a été généré.
     */
    public boolean isGenerated() {
        return generated;
    }

    // ==================== Utilitaires ====================

    /**
     * Compte le nombre de blocs non-air dans le chunk.
     * <p>
     * Utile pour l'optimisation du rendu (chunks vides).
     * </p>
     *
     * @return le nombre de blocs solides
     */
    public int countNonAirBlocks() {
        int count = 0;
        for (short voxel : voxels) {
            if (voxel != 0) {  // 0 = AIR
                count++;
            }
        }
        return count;
    }

    /**
     * Vérifie si le chunk est entièrement vide (que de l'air).
     *
     * @return true si tous les blocs sont de l'air, false sinon
     */
    public boolean isEmpty() {
        for (short voxel : voxels) {
            if (voxel != 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Retourne une représentation textuelle du chunk.
     *
     * @return une chaîne décrivant le chunk
     */
    @Override
    public String toString() {
        return String.format("Chunk[pos=(%d, %d), voxels=%d, modified=%b, nonAir=%d]",
                chunkX, chunkZ, TOTAL_VOXELS, modified, countNonAirBlocks());
    }
}