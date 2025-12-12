package org.almostcraft.render.chunk.meshing;

import org.almostcraft.world.block.BlockType;

/**
 * Représente les 6 directions des faces d'un bloc dans l'espace 3D.
 * <p>
 * Chaque direction possède un offset (dx, dy, dz) permettant de calculer
 * les coordonnées du bloc voisin dans cette direction.
 * </p>
 *
 * @author Lucas Préaux
 * @version 1.0
 */
public enum FaceDirection {
    /**
     * Face Nord (vers -Z).
     */
    NORTH(0, 0, -1, BlockType.BlockFace.NORTH),

    /**
     * Face Sud (vers +Z).
     */
    SOUTH(0, 0, 1, BlockType.BlockFace.SOUTH),

    /**
     * Face Ouest (vers -X).
     */
    WEST(-1, 0, 0, BlockType.BlockFace.WEST),

    /**
     * Face Est (vers +X).
     */
    EAST(1, 0, 0, BlockType.BlockFace.EAST),

    /**
     * Face Bas (vers -Y).
     */
    DOWN(0, -1, 0, BlockType.BlockFace.BOTTOM),

    /**
     * Face Haut (vers +Y).
     */
    UP(0, 1, 0, BlockType.BlockFace.TOP);

    // ==================== Attributs ====================

    private final int offsetX;
    private final int offsetY;
    private final int offsetZ;
    private final BlockType.BlockFace blockFace;

    // ==================== Constructeur ====================

    FaceDirection(int offsetX, int offsetY, int offsetZ, BlockType.BlockFace blockFace) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.blockFace = blockFace;
    }

    // ==================== Getters ====================

    /**
     * Retourne l'offset X pour calculer la position du voisin.
     *
     * @return l'offset X (-1, 0, ou 1)
     */
    public int getOffsetX() {
        return offsetX;
    }

    /**
     * Retourne l'offset Y pour calculer la position du voisin.
     *
     * @return l'offset Y (-1, 0, ou 1)
     */
    public int getOffsetY() {
        return offsetY;
    }

    /**
     * Retourne l'offset Z pour calculer la position du voisin.
     *
     * @return l'offset Z (-1, 0, ou 1)
     */
    public int getOffsetZ() {
        return offsetZ;
    }

    /**
     * Retourne la face de bloc correspondante.
     *
     * @return la BlockFace correspondante
     */
    public BlockType.BlockFace getBlockFace() {
        return blockFace;
    }
}
