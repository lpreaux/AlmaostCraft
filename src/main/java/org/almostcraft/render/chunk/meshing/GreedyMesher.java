package org.almostcraft.render.chunk.meshing;

import org.almostcraft.render.texture.TextureArray;
import org.almostcraft.world.World;
import org.almostcraft.world.block.BlockRegistry;
import org.almostcraft.world.block.BlockType;
import org.almostcraft.world.chunk.Chunk;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implémentation de l'algorithme Greedy Meshing pour optimiser le nombre de faces.
 * <p>
 * L'algorithme fonctionne en balayant le chunk dans chaque direction (6 faces),
 * puis en fusionnant les faces adjacentes identiques en rectangles plus grands.
 * Cela réduit drastiquement le nombre de triangles à rendre.
 * </p>
 *
 * @author Lucas Préaux
 * @version 1.0
 */
public class GreedyMesher {

    private static final Logger logger = LoggerFactory.getLogger(GreedyMesher.class);

    // ==================== Attributs ====================

    private final Chunk chunk;
    private final World world;
    private final BlockRegistry blockRegistry;
    private final TextureArray textureArray;

    // ==================== Constructeur ====================

    public GreedyMesher(Chunk chunk, World world, BlockRegistry blockRegistry, TextureArray textureArray) {
        this.chunk = chunk;
        this.world = world;
        this.blockRegistry = blockRegistry;
        this.textureArray = textureArray;
    }

    // ==================== Méthode principale ====================

    /**
     * Génère le mesh optimisé pour une direction donnée.
     *
     * @param direction direction de la face à traiter
     * @param builder   builder pour accumuler les quads
     */
    public void meshDirection(FaceDirection direction, MeshBuilder builder) {
        int width = getMaskWidth(direction);
        int height = getMaskHeight(direction);
        int depth = getMaskDepth(direction);

        MaskEntry[][] mask = new MaskEntry[width][height];

        // Pour chaque "slice" du chunk dans cette direction
        for (int d = 0; d < depth; d++) {
            clearMask(mask);
            fillMask(mask, direction, d);
            greedyMesh(mask, direction, d, builder);
        }
    }

    // ==================== Algorithme Greedy ====================

    /**
     * Applique l'algorithme greedy sur le masque pour fusionner les faces.
     */
    private void greedyMesh(MaskEntry[][] mask, FaceDirection direction, int depth, MeshBuilder builder) {
        int width = mask.length;
        int height = mask[0].length;

        boolean[][] consumed = new boolean[width][height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (mask[x][y] == null || consumed[x][y]) {
                    continue;
                }

                MaskEntry entry = mask[x][y];

                // Expand horizontalement puis verticalement
                int w = computeWidth(mask, consumed, x, y, entry);
                int h = computeHeight(mask, consumed, x, y, w, entry);

                // Générer le quad optimisé
                generateQuad(x, y, w, h, depth, direction, entry, builder);

                // Marquer les cellules comme consommées
                markConsumed(consumed, x, y, w, h);
            }
        }
    }

    // ==================== Calcul des dimensions ====================

    private int getMaskWidth(FaceDirection direction) {
        return switch (direction) {
            case NORTH, SOUTH -> Chunk.WIDTH;
            case EAST, WEST -> Chunk.DEPTH;
            case UP, DOWN -> Chunk.WIDTH;
        };
    }

    private int getMaskHeight(FaceDirection direction) {
        return switch (direction) {
            case NORTH, SOUTH, EAST, WEST -> Chunk.HEIGHT;
            case UP, DOWN -> Chunk.DEPTH;
        };
    }

    private int getMaskDepth(FaceDirection direction) {
        return switch (direction) {
            case NORTH, SOUTH -> Chunk.DEPTH;
            case EAST, WEST -> Chunk.WIDTH;
            case UP, DOWN -> Chunk.HEIGHT;
        };
    }

    // ==================== Gestion du masque ====================

    private void clearMask(MaskEntry[][] mask) {
        for (int x = 0; x < mask.length; x++) {
            for (int y = 0; y < mask[0].length; y++) {
                mask[x][y] = null;
            }
        }
    }

    private void fillMask(MaskEntry[][] mask, FaceDirection direction, int depth) {
        int width = mask.length;
        int height = mask[0].length;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                MaskEntry entry = computeMaskEntry(x, y, depth, direction);
                mask[x][y] = entry;
            }
        }
    }

    /**
     * Calcule l'entrée du masque pour une position donnée.
     * Retourne null si aucune face ne doit être générée.
     */
    private MaskEntry computeMaskEntry(int maskX, int maskY, int depth, FaceDirection direction) {
        // Convertir les coordonnées du masque en coordonnées chunk
        int[] localCoords = maskToChunkCoords(maskX, maskY, depth, direction);
        int localX = localCoords[0];
        int localY = localCoords[1];
        int localZ = localCoords[2];

        // Vérifier les limites du chunk
        if (localX < 0 || localX >= Chunk.WIDTH ||
                localY < 0 || localY >= Chunk.HEIGHT ||
                localZ < 0 || localZ >= Chunk.DEPTH) {
            return null;
        }

        // Récupérer le bloc
        int blockId = chunk.getVoxel(localX, localY, localZ);
        if (blockId == 0) {
            return null; // Air
        }

        BlockType blockType = blockRegistry.getBlockByNumericId(blockId);
        if (blockType == null || !blockType.isSolid()) {
            return null;
        }

        // Calculer les coordonnées mondiales
        int worldX = chunk.getChunkX() * Chunk.WIDTH + localX;
        int worldY = localY;
        int worldZ = chunk.getChunkZ() * Chunk.DEPTH + localZ;

        // Vérifier le voisin dans la direction de la face
        int neighborX = worldX + direction.getOffsetX();
        int neighborY = worldY + direction.getOffsetY();
        int neighborZ = worldZ + direction.getOffsetZ();

        // Si le voisin est opaque, ne pas générer la face
        if (world.isBlockOccluding(neighborX, neighborY, neighborZ)) {
            return null;
        }

        // Récupérer les informations de texture
        BlockType.BlockFace blockFace = direction.getBlockFace();
        String texturePath = blockType.getTexturePath(blockFace);
        int textureIndex = textureArray.getTextureIndex(texturePath);

        if (textureIndex == -1) {
            logger.warn("Texture '{}' not found in array", texturePath);
            return null;
        }

        // Calculer la couleur de teinte
        Vector3f tintColor = getTintColor(blockType, worldX, worldY, worldZ, blockFace);

        return new MaskEntry(blockType, textureIndex, tintColor, blockFace);
    }

    /**
     * Convertit les coordonnées du masque en coordonnées chunk locales.
     */
    private int[] maskToChunkCoords(int maskX, int maskY, int depth, FaceDirection direction) {
        return switch (direction) {
            case NORTH -> new int[]{maskX, maskY, depth};
            case SOUTH -> new int[]{maskX, maskY, depth};
            case WEST -> new int[]{depth, maskY, maskX};
            case EAST -> new int[]{depth, maskY, maskX};
            case DOWN -> new int[]{maskX, depth, maskY};
            case UP -> new int[]{maskX, depth, maskY};
        };
    }

    // ==================== Expansion greedy ====================

    /**
     * Calcule la largeur maximale du rectangle en expandant horizontalement.
     */
    private int computeWidth(MaskEntry[][] mask, boolean[][] consumed,
                             int startX, int startY, MaskEntry entry) {
        int width = 1;
        int maxWidth = mask.length;

        while (startX + width < maxWidth &&
                !consumed[startX + width][startY] &&
                entry.canMergeWith(mask[startX + width][startY])) {
            width++;
        }

        return width;
    }

    /**
     * Calcule la hauteur maximale du rectangle en expandant verticalement.
     */
    private int computeHeight(MaskEntry[][] mask, boolean[][] consumed,
                              int startX, int startY, int width, MaskEntry entry) {
        int height = 1;
        int maxHeight = mask[0].length;

        outer:
        while (startY + height < maxHeight) {
            for (int x = startX; x < startX + width; x++) {
                if (consumed[x][startY + height] ||
                        !entry.canMergeWith(mask[x][startY + height])) {
                    break outer;
                }
            }
            height++;
        }

        return height;
    }

    /**
     * Marque une zone rectangulaire comme consommée.
     */
    private void markConsumed(boolean[][] consumed, int startX, int startY, int width, int height) {
        for (int x = startX; x < startX + width; x++) {
            for (int y = startY; y < startY + height; y++) {
                consumed[x][y] = true;
            }
        }
    }

    // ==================== Génération du quad ====================

    /**
     * Génère un quad optimisé et l'ajoute au builder.
     */
    private void generateQuad(int maskX, int maskY, int width, int height,
                              int depth, FaceDirection direction, MaskEntry entry,
                              MeshBuilder builder) {
        // Convertir les coordonnées du masque en coordonnées monde
        int[] startCoords = maskToChunkCoords(maskX, maskY, depth, direction);
        int localX = startCoords[0];
        int localY = startCoords[1];
        int localZ = startCoords[2];

        float worldX = chunk.getChunkX() * Chunk.WIDTH + localX;
        float worldY = localY;
        float worldZ = chunk.getChunkZ() * Chunk.DEPTH + localZ;

        // Ajouter le quad au builder
        builder.addQuad(
                worldX, worldY, worldZ,
                width, height,
                direction,
                entry.textureIndex,
                entry.tintColor
        );
    }

    // ==================== Couleurs de teinte ====================

    /**
     * Calcule la couleur de teinte pour un bloc (ex: herbe verte).
     */
    private Vector3f getTintColor(BlockType blockType, int worldX, int worldY, int worldZ,
                                  BlockType.BlockFace blockFace) {
        if (!blockType.isTinted()) {
            return new Vector3f(1.0f, 1.0f, 1.0f);
        }

        // Cas spécial : herbe
        if (blockType.id().equals("almostcraft:grass_block") && blockFace == BlockType.BlockFace.TOP) {
            return getGrassColor(worldX, worldZ);
        }

        return new Vector3f(1.0f, 1.0f, 1.0f);
    }

    /**
     * Calcule la couleur de l'herbe (pourrait utiliser un biome plus tard).
     */
    private Vector3f getGrassColor(int worldX, int worldZ) {
        // Couleur d'herbe tempérée par défaut
        return new Vector3f(0.55f, 0.88f, 0.31f);
    }

    // ==================== Classe interne MaskEntry ====================

    /**
     * Représente une entrée dans le masque de faces.
     */
    private static class MaskEntry {
        final BlockType blockType;
        final int textureIndex;
        final Vector3f tintColor;
        final BlockType.BlockFace blockFace;

        MaskEntry(BlockType blockType, int textureIndex,
                  Vector3f tintColor, BlockType.BlockFace blockFace) {
            this.blockType = blockType;
            this.textureIndex = textureIndex;
            this.tintColor = tintColor;
            this.blockFace = blockFace;
        }

        /**
         * Vérifie si cette entrée peut être fusionnée avec une autre.
         * <p>
         * Deux entrées peuvent être fusionnées si elles ont le même bloc,
         * la même face et la même texture.
         * </p>
         */
        boolean canMergeWith(MaskEntry other) {
            if (other == null) return false;
            return this.blockType.id().equals(other.blockType.id())
                    && this.blockFace == other.blockFace
                    && this.textureIndex == other.textureIndex;
        }
    }
}
