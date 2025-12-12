package org.almostcraft.render.chunk;

import org.almostcraft.render.chunk.meshing.FaceDirection;
import org.almostcraft.render.chunk.meshing.GreedyMesher;
import org.almostcraft.render.chunk.meshing.MeshBuilder;
import org.almostcraft.render.core.Mesh;
import org.almostcraft.render.texture.TextureArray;
import org.almostcraft.world.World;
import org.almostcraft.world.block.BlockRegistry;
import org.almostcraft.world.chunk.Chunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Générateur de mesh pour un chunk avec Greedy Meshing et TextureArray.
 * <p>
 * Cette classe orchestre la génération du mesh en déléguant les responsabilités :
 * <ul>
 *   <li>{@link GreedyMesher} : Algorithme d'optimisation greedy meshing</li>
 *   <li>{@link MeshBuilder} : Construction progressive du mesh</li>
 *   <li>{@link FaceDirection} : Gestion des 6 directions des faces</li>
 * </ul>
 * </p>
 *
 * @author Lucas Préaux
 * @version 6.0 (refactorisé)
 */
public class ChunkMesh {

    private static final Logger logger = LoggerFactory.getLogger(ChunkMesh.class);

    // ==================== Attributs ====================

    private final Chunk chunk;
    private final World world;
    private final BlockRegistry blockRegistry;
    private final TextureArray textureArray;

    // ==================== Constructeur ====================

    /**
     * Crée un générateur de mesh pour un chunk.
     *
     * @param chunk         le chunk à convertir en mesh
     * @param world         le monde (pour l'occlusion des voisins)
     * @param blockRegistry le registre de blocs
     * @param textureArray  l'array de textures
     * @throws IllegalArgumentException si un paramètre est null
     */
    public ChunkMesh(Chunk chunk, World world, BlockRegistry blockRegistry, TextureArray textureArray) {
        if (chunk == null) {
            throw new IllegalArgumentException("Chunk cannot be null");
        }
        if (world == null) {
            throw new IllegalArgumentException("World cannot be null");
        }
        if (blockRegistry == null) {
            throw new IllegalArgumentException("BlockRegistry cannot be null");
        }
        if (textureArray == null) {
            throw new IllegalArgumentException("TextureArray cannot be null");
        }

        this.chunk = chunk;
        this.world = world;
        this.blockRegistry = blockRegistry;
        this.textureArray = textureArray;
    }

    // ==================== Génération du mesh ====================

    /**
     * Génère le mesh optimisé pour le chunk.
     * <p>
     * Processus :
     * <ol>
     *   <li>Crée un MeshBuilder pour accumuler les quads</li>
     *   <li>Crée un GreedyMesher pour optimiser les faces</li>
     *   <li>Traite chaque direction (6 faces)</li>
     *   <li>Exporte les données vers un Mesh OpenGL</li>
     * </ol>
     * </p>
     *
     * @return le mesh OpenGL prêt à être rendu
     */
    public Mesh build() {
        long startTime = System.nanoTime();

        logger.debug("Building mesh for chunk ({}, {})",
                chunk.getChunkX(), chunk.getChunkZ());

        // Créer le builder pour accumuler les quads
        MeshBuilder builder = new MeshBuilder();

        // Créer le greedy mesher
        GreedyMesher mesher = new GreedyMesher(chunk, world, blockRegistry, textureArray);

        // Traiter chaque direction
        for (FaceDirection direction : FaceDirection.values()) {
            mesher.meshDirection(direction, builder);
        }

        // Vérifier si le mesh est vide
        if (builder.isEmpty()) {
            logger.debug("Chunk ({}, {}) generated empty mesh",
                    chunk.getChunkX(), chunk.getChunkZ());
            return createEmptyMesh();
        }

        // Exporter vers un Mesh OpenGL
        float[] vertexArray = builder.toVertexArray();
        int[] indexArray = builder.toIndexArray();

        Mesh mesh = new Mesh();
        mesh.uploadData(vertexArray, indexArray);

        // Log des statistiques
        long endTime = System.nanoTime();
        double duration = (endTime - startTime) / 1_000_000.0;

        logger.info("Chunk ({}, {}) meshed: {} triangles, {:.2f}ms",
                chunk.getChunkX(), chunk.getChunkZ(),
                mesh.getTriangleCount(), duration);

        return mesh;
    }

    // ==================== Utilitaires ====================

    /**
     * Crée un mesh vide (pas de vertices).
     * <p>
     * Utilisé pour les chunks entièrement vides (tous blocs d'air).
     * </p>
     */
    private Mesh createEmptyMesh() {
        Mesh mesh = new Mesh();
        mesh.uploadData(new float[0], new int[0]);
        return mesh;
    }
}
