package org.almostcraft.render;

import org.almostcraft.world.block.BlockRegistry;
import org.almostcraft.world.block.BlockType;
import org.almostcraft.world.chunk.Chunk;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Générateur de mesh pour un chunk.
 * <p>
 * Cette classe convertit un {@link Chunk} (données voxel) en un {@link Mesh}
 * (vertices et indices) prêt à être rendu par OpenGL.
 * </p>
 * <p>
 * <strong>Version actuelle : Naïve</strong>
 * <ul>
 *   <li>Génère toutes les faces de tous les blocs solides</li>
 *   <li>Pas de face culling (faces cachées rendues)</li>
 *   <li>Pas de greedy meshing</li>
 * </ul>
 * Optimisations futures :
 * <ul>
 *   <li>Face culling : Ne pas générer les faces contre d'autres blocs</li>
 *   <li>Greedy meshing : Fusionner les faces adjacentes</li>
 *   <li>Ambient occlusion</li>
 * </ul>
 * </p>
 * <p>
 * Utilisation typique :
 * <pre>{@code
 * Chunk chunk = world.getChunk(0, 0);
 * ChunkMesh chunkMesh = new ChunkMesh(chunk, blockRegistry);
 * Mesh mesh = chunkMesh.build();
 *
 * // Dans la boucle de rendu
 * mesh.render();
 * }</pre>
 * </p>
 *
 * @author Lucas Préaux
 * @version 1.0
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
     * @param blockRegistry le registre de blocs
     * @throws IllegalArgumentException si chunk ou blockRegistry sont null
     */
    public ChunkMesh(Chunk chunk, BlockRegistry blockRegistry) {
        if (chunk == null) {
            throw new IllegalArgumentException("Chunk cannot be null");
        }
        if (blockRegistry == null) {
            throw new IllegalArgumentException("BlockRegistry cannot be null");
        }

        this.chunk = chunk;
        this.blockRegistry = blockRegistry;
        this.vertices = new ArrayList<>();
        this.indices = new ArrayList<>();
        this.vertexCount = 0;
    }

    // ==================== Génération du mesh ====================

    /**
     * Génère et retourne le mesh du chunk.
     * <p>
     * Cette méthode parcourt tous les blocs du chunk et génère les faces
     * pour chaque bloc solide (non-air).
     * </p>
     *
     * @return le mesh prêt à être rendu
     */
    public Mesh build() {
        long startTime = System.nanoTime();

        logger.debug("Building mesh for chunk ({}, {})", chunk.getChunkX(), chunk.getChunkZ());

        // Réinitialiser les listes
        vertices.clear();
        indices.clear();
        vertexCount = 0;

        // Parcourir tous les blocs du chunk
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int y = 0; y < Chunk.HEIGHT; y++) {
                for (int z = 0; z < Chunk.DEPTH; z++) {
                    int blockId = chunk.getVoxel(x, y, z);

                    // Ignorer l'air (ID 0)
                    if (blockId == 0) {
                        continue;
                    }

                    // Récupérer le type de bloc
                    BlockType blockType = blockRegistry.getBlockByNumericId(blockId);
                    if (blockType == null || !blockType.isSolid()) {
                        continue;
                    }

                    // Générer les 6 faces du bloc
                    generateBlockFaces(x, y, z, blockType);
                }
            }
        }

        // Convertir les listes en arrays
        float[] vertexArray = toFloatArray(vertices);
        int[] indexArray = toIntArray(indices);

        // Créer le mesh
        Mesh mesh = new Mesh();
        mesh.uploadData(vertexArray, indexArray);

        long endTime = System.nanoTime();
        double duration = (endTime - startTime) / 1_000_000.0; // en ms

        logger.info("Chunk mesh built: {} vertices, {} triangles, {:.2f}ms",
                vertexCount, mesh.getTriangleCount(), duration);

        return mesh;
    }

    // ==================== Génération des faces ====================

    /**
     * Génère les 6 faces d'un bloc.
     *
     * @param x         coordonnée X locale du bloc
     * @param y         coordonnée Y locale du bloc
     * @param z         coordonnée Z locale du bloc
     * @param blockType le type de bloc
     */
    private void generateBlockFaces(int x, int y, int z, BlockType blockType) {
        // Position du bloc dans le monde (coordonnées du chunk)
        float worldX = chunk.getChunkX() * Chunk.WIDTH + x;
        float worldY = y;
        float worldZ = chunk.getChunkZ() * Chunk.DEPTH + z;

        // Couleur du bloc (temporaire : basée sur le type)
        Vector3f color = getBlockColor(blockType);

        // Générer les 6 faces
        // Pour l'instant, on génère TOUTES les faces (version naïve)
        generateFace(worldX, worldY, worldZ, FaceDirection.NORTH, color);
        generateFace(worldX, worldY, worldZ, FaceDirection.SOUTH, color);
        generateFace(worldX, worldY, worldZ, FaceDirection.EAST, color);
        generateFace(worldX, worldY, worldZ, FaceDirection.WEST, color);
        generateFace(worldX, worldY, worldZ, FaceDirection.UP, color);
        generateFace(worldX, worldY, worldZ, FaceDirection.DOWN, color);
    }

    /**
     * Génère une face d'un bloc dans une direction donnée.
     *
     * @param x         position X du bloc
     * @param y         position Y du bloc
     * @param z         position Z du bloc
     * @param direction direction de la face
     * @param color     couleur de la face
     */
    private void generateFace(float x, float y, float z, FaceDirection direction, Vector3f color) {
        // Les 4 vertices de la face (quad)
        Vector3f[] faceVertices = getFaceVertices(x, y, z, direction);

        // Index de départ pour cette face
        int startIndex = vertexCount;

        // Ajouter les 4 vertices
        for (Vector3f vertex : faceVertices) {
            vertices.add(vertex.x);
            vertices.add(vertex.y);
            vertices.add(vertex.z);
            vertices.add(color.x);
            vertices.add(color.y);
            vertices.add(color.z);
            vertexCount++;
        }

        // Ajouter les indices pour 2 triangles (quad = 2 triangles)
        // Triangle 1 : 0, 1, 2
        indices.add(startIndex);
        indices.add(startIndex + 1);
        indices.add(startIndex + 2);

        // Triangle 2 : 2, 3, 0
        indices.add(startIndex + 2);
        indices.add(startIndex + 3);
        indices.add(startIndex);
    }

    /**
     * Retourne les 4 vertices d'une face dans une direction donnée.
     *
     * @param x         position X du bloc
     * @param y         position Y du bloc
     * @param z         position Z du bloc
     * @param direction direction de la face
     * @return tableau de 4 vertices (dans le sens CCW vu de l'extérieur)
     */
    private Vector3f[] getFaceVertices(float x, float y, float z, FaceDirection direction) {
        float s = BLOCK_SIZE;

        return switch (direction) {
            case NORTH -> new Vector3f[]{ // -Z
                    new Vector3f(x, y, z),
                    new Vector3f(x + s, y, z),
                    new Vector3f(x + s, y + s, z),
                    new Vector3f(x, y + s, z)
            };
            case SOUTH -> new Vector3f[]{ // +Z
                    new Vector3f(x + s, y, z + s),
                    new Vector3f(x, y, z + s),
                    new Vector3f(x, y + s, z + s),
                    new Vector3f(x + s, y + s, z + s)
            };
            case WEST -> new Vector3f[]{ // -X
                    new Vector3f(x, y, z + s),
                    new Vector3f(x, y, z),
                    new Vector3f(x, y + s, z),
                    new Vector3f(x, y + s, z + s)
            };
            case EAST -> new Vector3f[]{ // +X
                    new Vector3f(x + s, y, z),
                    new Vector3f(x + s, y, z + s),
                    new Vector3f(x + s, y + s, z + s),
                    new Vector3f(x + s, y + s, z)
            };
            case DOWN -> new Vector3f[]{ // -Y
                    new Vector3f(x, y, z),
                    new Vector3f(x, y, z + s),
                    new Vector3f(x + s, y, z + s),
                    new Vector3f(x + s, y, z)
            };
            case UP -> new Vector3f[]{ // +Y
                    new Vector3f(x, y + s, z),
                    new Vector3f(x + s, y + s, z),
                    new Vector3f(x + s, y + s, z + s),
                    new Vector3f(x, y + s, z + s)
            };
        };
    }

    // ==================== Couleurs ====================

    /**
     * Retourne une couleur temporaire pour un type de bloc.
     * <p>
     * À terme, cette méthode sera remplacée par un système de textures.
     * </p>
     *
     * @param blockType le type de bloc
     * @return la couleur RGB (0-1)
     */
    private Vector3f getBlockColor(BlockType blockType) {
        // Couleurs basiques selon l'ID du bloc
        return switch (blockType.id()) {
            case "almostcraft:stone" -> new Vector3f(0.5f, 0.5f, 0.5f);      // Gris
            case "almostcraft:dirt" -> new Vector3f(0.6f, 0.4f, 0.2f);       // Marron
            case "almostcraft:grass_block" -> new Vector3f(0.3f, 0.8f, 0.3f); // Vert
            case "almostcraft:cobblestone" -> new Vector3f(0.4f, 0.4f, 0.4f); // Gris foncé
            case "almostcraft:sand" -> new Vector3f(0.9f, 0.8f, 0.6f);       // Beige
            case "almostcraft:oak_planks" -> new Vector3f(0.7f, 0.5f, 0.3f); // Bois
            case "almostcraft:glass" -> new Vector3f(0.8f, 0.9f, 1.0f);      // Bleu clair
            default -> new Vector3f(1.0f, 0.0f, 1.0f);                       // Magenta (erreur)
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

    // ==================== Enum FaceDirection ====================

    /**
     * Direction d'une face de bloc.
     */
    private enum FaceDirection {
        NORTH,  // -Z
        SOUTH,  // +Z
        WEST,   // -X
        EAST,   // +X
        DOWN,   // -Y
        UP      // +Y
    }
}
