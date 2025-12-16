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
 * <p>
 * <strong>Usage typique :</strong>
 * <pre>{@code
 * ChunkMeshGenerator generator = new ChunkMeshGenerator(world, blockRegistry, textureArray);
 * Mesh mesh = generator.generateMesh(chunk);
 * chunk.setMesh(mesh);
 * }</pre>
 * </p>
 *
 * @author Lucas Préaux
 * @version 7.0 (refactorisé pour le culling)
 */
public class ChunkMeshGenerator {

    private static final Logger logger = LoggerFactory.getLogger(ChunkMeshGenerator.class);

    // ==================== Attributs ====================

    private final World world;
    private final BlockRegistry blockRegistry;
    private final TextureArray textureArray;

    // ==================== Constructeur ====================

    /**
     * Crée un générateur de mesh réutilisable.
     * <p>
     * Le générateur peut être réutilisé pour générer les mesh de plusieurs chunks.
     * </p>
     *
     * @param world         le monde (pour l'occlusion des voisins)
     * @param blockRegistry le registre de blocs
     * @param textureArray  l'array de textures
     * @throws IllegalArgumentException si un paramètre est null
     */
    public ChunkMeshGenerator(World world, BlockRegistry blockRegistry, TextureArray textureArray) {
        if (world == null) {
            throw new IllegalArgumentException("World cannot be null");
        }
        if (blockRegistry == null) {
            throw new IllegalArgumentException("BlockRegistry cannot be null");
        }
        if (textureArray == null) {
            throw new IllegalArgumentException("TextureArray cannot be null");
        }

        this.world = world;
        this.blockRegistry = blockRegistry;
        this.textureArray = textureArray;
    }

    // ==================== Génération du mesh ====================

    /**
     * Génère le mesh optimisé pour un chunk.
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
     * @param chunk le chunk à convertir en mesh
     * @return le mesh OpenGL prêt à être rendu (peut être vide)
     * @throws IllegalArgumentException si le chunk est null
     */
    public Mesh generateMesh(Chunk chunk) {
        if (chunk == null) {
            throw new IllegalArgumentException("Chunk cannot be null");
        }

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

        // Créer le mesh (même s'il est vide)
        Mesh mesh = new Mesh();

        // Vérifier si le mesh est vide
        if (builder.isEmpty()) {
            logger.debug("Chunk ({}, {}) generated empty mesh",
                    chunk.getChunkX(), chunk.getChunkZ());
            // Upload un mesh vide (tableaux vides)
            mesh.uploadData(new float[0], new int[0]);
            return mesh;
        }

        // Exporter vers un Mesh OpenGL
        float[] vertexArray = builder.toVertexArray();
        int[] indexArray = builder.toIndexArray();

        mesh.uploadData(vertexArray, indexArray);

        // Log des statistiques
        long endTime = System.nanoTime();
        double duration = (endTime - startTime) / 1_000_000.0;

        logger.info("Chunk ({}, {}) meshed: {} vertices, {} triangles, {:.2f}ms",
                chunk.getChunkX(), chunk.getChunkZ(),
                mesh.getVertexCount(), mesh.getTriangleCount(), duration);

        return mesh;
    }

    /**
     * Régénère le mesh d'un chunk si nécessaire.
     * <p>
     * Vérifie d'abord si le chunk a besoin d'une reconstruction.
     * Si oui, génère le mesh et le définit sur le chunk.
     * </p>
     *
     * @param chunk le chunk à vérifier/régénérer
     * @return true si le mesh a été régénéré, false sinon
     */
    public boolean regenerateIfNeeded(Chunk chunk) {
        if (chunk.needsMeshRebuild()) {
            Mesh mesh = generateMesh(chunk);
            chunk.setMesh(mesh);
            return true;
        }
        return false;
    }

    /**
     * Régénère les mesh de plusieurs chunks en batch.
     * <p>
     * Pratique pour régénérer tous les chunks modifiés en une fois.
     * Limite optionnelle pour éviter de bloquer trop longtemps.
     * </p>
     *
     * @param chunks   les chunks à traiter
     * @param maxCount nombre maximum de mesh à générer (0 = illimité)
     * @return le nombre de mesh régénérés
     */
    public int regenerateAll(Iterable<Chunk> chunks, int maxCount) {
        int count = 0;
        for (Chunk chunk : chunks) {
            if (maxCount > 0 && count >= maxCount) {
                break;
            }
            if (regenerateIfNeeded(chunk)) {
                count++;
            }
        }

        if (count > 0) {
            logger.debug("Regenerated {} chunk meshes", count);
        }

        return count;
    }

    /**
     * Régénère tous les mesh sans limite.
     *
     * @param chunks les chunks à traiter
     * @return le nombre de mesh régénérés
     */
    public int regenerateAll(Iterable<Chunk> chunks) {
        return regenerateAll(chunks, 0);
    }
}
