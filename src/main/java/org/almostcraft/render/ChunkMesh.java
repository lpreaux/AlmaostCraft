package org.almostcraft.render;

import org.almostcraft.world.World;
import org.almostcraft.world.block.BlockRegistry;
import org.almostcraft.world.block.BlockType;
import org.almostcraft.world.chunk.Chunk;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Générateur de mesh pour un chunk avec algorithme de Greedy Meshing.
 * <p>
 * Cette version optimisée fusionne les faces adjacentes identiques pour
 * réduire drastiquement le nombre de vertices générés.
 * </p>
 * <p>
 * Optimisations :
 * <ul>
 *   <li>Face culling : Ne génère que les faces exposées</li>
 *   <li>Greedy meshing : Fusionne les faces adjacentes du même type</li>
 *   <li>Réduction typique de 80-90% des vertices</li>
 * </ul>
 * </p>
 *
 * @author Lucas Préaux
 * @version 2.0 (Greedy Meshing)
 */
public class ChunkMesh {

    private static final Logger logger = LoggerFactory.getLogger(ChunkMesh.class);

    // ==================== Constantes ====================

    /**
     * Taille d'un bloc (1 unité = 1 bloc).
     */
    private static final float BLOCK_SIZE = 1.0f;

    /**
     * Nombre de floats par vertex (position + couleur).
     */
    private static final int FLOATS_PER_VERTEX = 6; // x, y, z, r, g, b

    // ==================== Attributs ====================

    /**
     * Le chunk à convertir en mesh.
     */
    private final Chunk chunk;

    /**
     * Référence au monde (pour vérifier les voisins inter-chunks).
     */
    private final World world;

    /**
     * Registre de blocs pour obtenir les propriétés des blocs.
     */
    private final BlockRegistry blockRegistry;

    /**
     * Liste dynamique des vertices (x, y, z, r, g, b).
     */
    private final List<Float> vertices;

    /**
     * Liste dynamique des indices des triangles.
     */
    private final List<Integer> indices;

    /**
     * Compteur de vertices (utilisé pour calculer les indices).
     */
    private int vertexCount;

    // ==================== Constructeur ====================

    /**
     * Crée un générateur de mesh pour le chunk spécifié.
     *
     * @param chunk         le chunk à convertir
     * @param world         le monde (pour vérifier les voisins)
     * @param blockRegistry le registre de blocs
     * @throws IllegalArgumentException si chunk, world ou blockRegistry sont null
     */
    public ChunkMesh(Chunk chunk, World world, BlockRegistry blockRegistry) {
        if (chunk == null) {
            throw new IllegalArgumentException("Chunk cannot be null");
        }
        if (world == null) {
            throw new IllegalArgumentException("World cannot be null");
        }
        if (blockRegistry == null) {
            throw new IllegalArgumentException("BlockRegistry cannot be null");
        }

        this.chunk = chunk;
        this.world = world;
        this.blockRegistry = blockRegistry;
        this.vertices = new ArrayList<>();
        this.indices = new ArrayList<>();
        this.vertexCount = 0;
    }

    // ==================== Génération du mesh ====================

    /**
     * Génère et retourne le mesh du chunk avec greedy meshing.
     *
     * @return le mesh optimisé prêt à être rendu
     */
    public Mesh build() {
        long startTime = System.nanoTime();

        logger.debug("Building greedy mesh for chunk ({}, {})", chunk.getChunkX(), chunk.getChunkZ());

        // Réinitialiser les listes
        vertices.clear();
        indices.clear();
        vertexCount = 0;

        // Générer les meshes pour chaque direction
        for (FaceDirection direction : FaceDirection.values()) {
            greedyMeshDirection(direction);
        }

        // Convertir les listes en arrays
        float[] vertexArray = toFloatArray(vertices);
        int[] indexArray = toIntArray(indices);

        // Créer le mesh
        Mesh mesh = new Mesh();
        mesh.uploadData(vertexArray, indexArray);

        long endTime = System.nanoTime();
        double duration = (endTime - startTime) / 1_000_000.0; // en ms

        logger.info("Greedy mesh built: {} vertices, {} triangles, {:.2f}ms",
                vertexCount, mesh.getTriangleCount(), duration);

        return mesh;
    }

    // ==================== Greedy Meshing par direction ====================

    /**
     * Génère le mesh greedy pour une direction spécifique.
     *
     * @param direction la direction des faces à traiter
     */
    private void greedyMeshDirection(FaceDirection direction) {
        // Dimensions du masque selon la direction
        int width = getMaskWidth(direction);
        int height = getMaskHeight(direction);
        int depth = getMaskDepth(direction);

        // Créer le masque 2D
        MaskEntry[][] mask = new MaskEntry[width][height];

        // Pour chaque tranche le long de l'axe de la direction
        for (int d = 0; d < depth; d++) {
            // Réinitialiser le masque
            clearMask(mask);

            // Remplir le masque pour cette tranche
            fillMask(mask, direction, d);

            // Appliquer l'algorithme greedy sur ce masque
            greedyMesh(mask, direction, d);
        }
    }

    /**
     * Applique l'algorithme greedy meshing sur un masque 2D.
     *
     * @param mask      le masque contenant les faces exposées
     * @param direction la direction des faces
     * @param depth     la position de la tranche (coordonnée le long de l'axe de direction)
     */
    private void greedyMesh(MaskEntry[][] mask, FaceDirection direction, int depth) {
        int width = mask.length;
        int height = mask[0].length;

        // Masque de cases déjà consommées
        boolean[][] consumed = new boolean[width][height];

        // Parcourir le masque
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Si la case est vide ou déjà consommée, passer
                if (mask[x][y] == null || consumed[x][y]) {
                    continue;
                }

                MaskEntry entry = mask[x][y];

                // Étape 1 : Expansion horizontale (largeur)
                int w = computeWidth(mask, consumed, x, y, entry);

                // Étape 2 : Expansion verticale (hauteur) avec cette largeur
                int h = computeHeight(mask, consumed, x, y, w, entry);

                // Étape 3 : Générer le quad fusionné
                generateGreedyQuad(x, y, w, h, depth, direction, entry);

                // Étape 4 : Marquer les cases comme consommées
                markConsumed(consumed, x, y, w, h);
            }
        }
    }

    // ==================== Calcul des dimensions du masque ====================

    /**
     * Retourne la largeur du masque pour une direction donnée.
     */
    private int getMaskWidth(FaceDirection direction) {
        return switch (direction) {
            case NORTH, SOUTH -> Chunk.WIDTH;   // Axe X
            case EAST, WEST -> Chunk.DEPTH;     // Axe Z
            case UP, DOWN -> Chunk.WIDTH;       // Axe X
        };
    }

    /**
     * Retourne la hauteur du masque pour une direction donnée.
     */
    private int getMaskHeight(FaceDirection direction) {
        return switch (direction) {
            case NORTH, SOUTH, EAST, WEST -> Chunk.HEIGHT; // Axe Y
            case UP, DOWN -> Chunk.DEPTH;                  // Axe Z
        };
    }

    /**
     * Retourne la profondeur (nombre de tranches) pour une direction donnée.
     */
    private int getMaskDepth(FaceDirection direction) {
        return switch (direction) {
            case NORTH, SOUTH -> Chunk.DEPTH;   // Tranches le long de Z
            case EAST, WEST -> Chunk.WIDTH;     // Tranches le long de X
            case UP, DOWN -> Chunk.HEIGHT;      // Tranches le long de Y
        };
    }

    // ==================== Remplissage du masque ====================

    /**
     * Réinitialise un masque (met toutes les cases à null).
     */
    private void clearMask(MaskEntry[][] mask) {
        for (int x = 0; x < mask.length; x++) {
            for (int y = 0; y < mask[0].length; y++) {
                mask[x][y] = null;
            }
        }
    }

    /**
     * Remplit le masque pour une tranche donnée.
     *
     * @param mask      le masque à remplir
     * @param direction la direction des faces
     * @param depth     la position de la tranche
     */
    private void fillMask(MaskEntry[][] mask, FaceDirection direction, int depth) {
        int width = mask.length;
        int height = mask[0].length;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Convertir (x, y, depth) en coordonnées de chunk (localX, localY, localZ)
                int[] localCoords = maskToChunkCoords(x, y, depth, direction);
                int localX = localCoords[0];
                int localY = localCoords[1];
                int localZ = localCoords[2];

                // Vérifier que c'est dans les limites du chunk
                if (localX < 0 || localX >= Chunk.WIDTH ||
                        localY < 0 || localY >= Chunk.HEIGHT ||
                        localZ < 0 || localZ >= Chunk.DEPTH) {
                    continue;
                }

                // Récupérer le bloc à cette position
                int blockId = chunk.getVoxel(localX, localY, localZ);

                // Ignorer l'air
                if (blockId == 0) {
                    continue;
                }

                // Récupérer le type de bloc
                BlockType blockType = blockRegistry.getBlockByNumericId(blockId);
                if (blockType == null || !blockType.isSolid()) {
                    continue;
                }

                // Convertir en coordonnées mondiales pour vérifier le voisin
                int worldX = chunk.getChunkX() * Chunk.WIDTH + localX;
                int worldY = localY;
                int worldZ = chunk.getChunkZ() * Chunk.DEPTH + localZ;

                // Vérifier si la face est exposée (le voisin ne bloque pas)
                int neighborX = worldX + direction.getOffsetX();
                int neighborY = worldY + direction.getOffsetY();
                int neighborZ = worldZ + direction.getOffsetZ();

                if (!world.isBlockOccluding(neighborX, neighborY, neighborZ)) {
                    // Face exposée : ajouter au masque
                    Vector3f color = getBlockColor(blockType);
                    mask[x][y] = new MaskEntry(blockType, color);
                }
            }
        }
    }

    /**
     * Convertit des coordonnées de masque en coordonnées de chunk.
     *
     * @param maskX     coordonnée X dans le masque
     * @param maskY     coordonnée Y dans le masque
     * @param depth     profondeur de la tranche
     * @param direction direction du masque
     * @return tableau [localX, localY, localZ]
     */
    private int[] maskToChunkCoords(int maskX, int maskY, int depth, FaceDirection direction) {
        return switch (direction) {
            case NORTH -> new int[]{maskX, maskY, depth};           // X, Y, Z
            case SOUTH -> new int[]{maskX, maskY, depth};           // X, Y, Z
            case WEST -> new int[]{depth, maskY, maskX};            // Z→X, Y, X→Z
            case EAST -> new int[]{depth, maskY, maskX};            // Z→X, Y, X→Z
            case DOWN -> new int[]{maskX, depth, maskY};            // X, Z→Y, Y→Z
            case UP -> new int[]{maskX, depth, maskY};              // X, Z→Y, Y→Z
        };
    }

    // ==================== Expansion greedy ====================

    /**
     * Calcule la largeur maximale d'expansion horizontale.
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
     * Calcule la hauteur maximale d'expansion verticale pour une largeur donnée.
     */
    private int computeHeight(MaskEntry[][] mask, boolean[][] consumed,
                              int startX, int startY, int width, MaskEntry entry) {
        int height = 1;
        int maxHeight = mask[0].length;

        outer:
        while (startY + height < maxHeight) {
            // Vérifier que toute la ligne de largeur 'width' peut être fusionnée
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

    // ==================== Génération du quad greedy ====================

    /**
     * Génère un quad fusionné (greedy).
     *
     * @param maskX     position X dans le masque
     * @param maskY     position Y dans le masque
     * @param width     largeur du quad (en blocs)
     * @param height    hauteur du quad (en blocs)
     * @param depth     profondeur de la tranche
     * @param direction direction de la face
     * @param entry     entrée du masque (contient le type et la couleur)
     */
    private void generateGreedyQuad(int maskX, int maskY, int width, int height,
                                    int depth, FaceDirection direction, MaskEntry entry) {
        // Convertir les coordonnées du masque en coordonnées mondiales
        int[] startCoords = maskToChunkCoords(maskX, maskY, depth, direction);
        int localX = startCoords[0];
        int localY = startCoords[1];
        int localZ = startCoords[2];

        // Convertir en coordonnées mondiales
        float worldX = chunk.getChunkX() * Chunk.WIDTH + localX;
        float worldY = localY;
        float worldZ = chunk.getChunkZ() * Chunk.DEPTH + localZ;

        // Générer les 4 vertices du quad fusionné
        Vector3f[] quadVertices = getGreedyQuadVertices(
                worldX, worldY, worldZ, width, height, direction
        );

        // Couleur
        Vector3f color = entry.color;

        // Index de départ
        int startIndex = vertexCount;

        // Ajouter les 4 vertices
        for (Vector3f vertex : quadVertices) {
            vertices.add(vertex.x);
            vertices.add(vertex.y);
            vertices.add(vertex.z);
            vertices.add(color.x);
            vertices.add(color.y);
            vertices.add(color.z);
            vertexCount++;
        }

        // Ajouter les indices (2 triangles)
        indices.add(startIndex);
        indices.add(startIndex + 1);
        indices.add(startIndex + 2);

        indices.add(startIndex + 2);
        indices.add(startIndex + 3);
        indices.add(startIndex);
    }

    /**
     * Retourne les 4 vertices d'un quad greedy étendu.
     *
     * @param x         position X de départ
     * @param y         position Y de départ
     * @param z         position Z de départ
     * @param width     largeur en blocs
     * @param height    hauteur en blocs
     * @param direction direction de la face
     * @return tableau de 4 vertices (CCW vu de l'extérieur)
     */
    private Vector3f[] getGreedyQuadVertices(float x, float y, float z,
                                             int width, int height,
                                             FaceDirection direction) {
        float w = width * BLOCK_SIZE;
        float h = height * BLOCK_SIZE;

        return switch (direction) {
            case NORTH -> new Vector3f[]{
                    new Vector3f(x, y, z),
                    new Vector3f(x + w, y, z),
                    new Vector3f(x + w, y + h, z),
                    new Vector3f(x, y + h, z)
            };
            case SOUTH -> new Vector3f[]{
                    new Vector3f(x + w, y, z + BLOCK_SIZE),
                    new Vector3f(x, y, z + BLOCK_SIZE),
                    new Vector3f(x, y + h, z + BLOCK_SIZE),
                    new Vector3f(x + w, y + h, z + BLOCK_SIZE)
            };
            case WEST -> new Vector3f[]{
                    new Vector3f(x, y, z + w),
                    new Vector3f(x, y, z),
                    new Vector3f(x, y + h, z),
                    new Vector3f(x, y + h, z + w)
            };
            case EAST -> new Vector3f[]{
                    new Vector3f(x + BLOCK_SIZE, y, z),
                    new Vector3f(x + BLOCK_SIZE, y, z + w),
                    new Vector3f(x + BLOCK_SIZE, y + h, z + w),
                    new Vector3f(x + BLOCK_SIZE, y + h, z)
            };
            case DOWN -> new Vector3f[]{
                    new Vector3f(x, y, z),
                    new Vector3f(x + w, y, z),
                    new Vector3f(x + w, y, z + h),
                    new Vector3f(x, y, z + h)
            };
            case UP -> new Vector3f[]{
                    new Vector3f(x, y + BLOCK_SIZE, z),
                    new Vector3f(x, y + BLOCK_SIZE, z + h),
                    new Vector3f(x + w, y + BLOCK_SIZE, z + h),
                    new Vector3f(x + w, y + BLOCK_SIZE, z)
            };
        };
    }

    // ==================== Couleurs ====================

    /**
     * Retourne une couleur temporaire pour un type de bloc.
     */
    private Vector3f getBlockColor(BlockType blockType) {
        return switch (blockType.id()) {
            case "almostcraft:stone" -> new Vector3f(0.5f, 0.5f, 0.5f);
            case "almostcraft:dirt" -> new Vector3f(0.6f, 0.4f, 0.2f);
            case "almostcraft:grass_block" -> new Vector3f(0.3f, 0.8f, 0.3f);
            case "almostcraft:cobblestone" -> new Vector3f(0.4f, 0.4f, 0.4f);
            case "almostcraft:sand" -> new Vector3f(0.9f, 0.8f, 0.6f);
            case "almostcraft:oak_planks" -> new Vector3f(0.7f, 0.5f, 0.3f);
            case "almostcraft:glass" -> new Vector3f(0.8f, 0.9f, 1.0f);
            default -> new Vector3f(1.0f, 0.0f, 1.0f);
        };
    }

    // ==================== Utilitaires ====================

    /**
     * Convertit une liste de Float en float[].
     */
    private float[] toFloatArray(List<Float> list) {
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    /**
     * Convertit une liste d'Integer en int[].
     */
    private int[] toIntArray(List<Integer> list) {
        int[] array = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    // ==================== Classe interne MaskEntry ====================

    /**
     * Représente une entrée dans le masque 2D.
     */
    private static class MaskEntry {
        final BlockType blockType;
        final Vector3f color;

        MaskEntry(BlockType blockType, Vector3f color) {
            this.blockType = blockType;
            this.color = color;
        }

        /**
         * Vérifie si deux entrées peuvent être fusionnées (même type de bloc).
         */
        boolean canMergeWith(MaskEntry other) {
            if (other == null) return false;
            return this.blockType.id().equals(other.blockType.id());
        }
    }

    // ==================== Enum FaceDirection ====================

    /**
     * Direction d'une face de bloc.
     */
    private enum FaceDirection {
        NORTH(0, 0, -1),
        SOUTH(0, 0, 1),
        WEST(-1, 0, 0),
        EAST(1, 0, 0),
        DOWN(0, -1, 0),
        UP(0, 1, 0);

        private final int offsetX;
        private final int offsetY;
        private final int offsetZ;

        FaceDirection(int offsetX, int offsetY, int offsetZ) {
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.offsetZ = offsetZ;
        }

        public int getOffsetX() {
            return offsetX;
        }

        public int getOffsetY() {
            return offsetY;
        }

        public int getOffsetZ() {
            return offsetZ;
        }
    }
}
