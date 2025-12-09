package org.almostcraft.world.generation;

import org.almostcraft.world.block.BlockRegistry;
import org.almostcraft.world.block.Blocks;
import org.almostcraft.world.chunk.Chunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Générateur de terrain plat avec stratification pierre/terre/herbe.
 * <p>
 * Génère un monde plat à une hauteur configurable avec :
 * <ul>
 *   <li>Pierre du bedrock jusqu'à surfaceHeight - 3</li>
 *   <li>3 couches de terre</li>
 *   <li>1 couche d'herbe en surface</li>
 *   <li>Air au-dessus</li>
 * </ul>
 * </p>
 *
 * @author Lucas Préaux
 * @version 1.0
 */
public class FlatTerrainGenerator implements TerrainGenerator {

    private static final Logger logger = LoggerFactory.getLogger(FlatTerrainGenerator.class);

    // ==================== Constantes ====================

    /**
     * Hauteur de surface par défaut (Y=64).
     */
    public static final int DEFAULT_TERRAIN_HEIGHT = 64;

    /**
     * Profondeur de la couche de terre sous l'herbe (3 blocs).
     */
    public static final int DIRT_LAYER_DEPTH = 3;

    // ==================== Attributs ====================

    private final int airId;
    private final int grassId;
    private final int dirtId;
    private final int stoneId;

    private final int surfaceHeight;

    // ==================== Constructeurs ====================

    /**
     * Crée un générateur de terrain plat avec hauteur par défaut (Y=64).
     *
     * @param blockRegistry le registre de blocs
     */
    public FlatTerrainGenerator(BlockRegistry blockRegistry) {
        this(blockRegistry, DEFAULT_TERRAIN_HEIGHT);
    }

    /**
     * Crée un générateur de terrain plat avec hauteur personnalisée.
     *
     * @param blockRegistry le registre de blocs
     * @param surfaceHeight la hauteur de la surface (0-255)
     * @throws IllegalArgumentException si la hauteur est invalide
     * @throws IllegalStateException si un bloc requis n'est pas enregistré
     */
    public FlatTerrainGenerator(BlockRegistry blockRegistry, int surfaceHeight) {
        validateSurfaceHeight(surfaceHeight);

        this.surfaceHeight = surfaceHeight;
        this.airId = requireBlock(blockRegistry, Blocks.AIR.id());
        this.grassId = requireBlock(blockRegistry, Blocks.GRASS.id());
        this.dirtId = requireBlock(blockRegistry, Blocks.DIRT.id());
        this.stoneId = requireBlock(blockRegistry, Blocks.STONE.id());

        logger.debug("FlatTerrainGenerator created with surface height: {}", surfaceHeight);
    }

    // ==================== Génération ====================

    @Override
    public void generate(Chunk chunk, int chunkX, int chunkZ) {
        logger.trace("Generating flat terrain for chunk ({}, {})", chunkX, chunkZ);

        for (int y = 0; y < Chunk.HEIGHT; y++) {
            int blockId = determineBlockType(y);
            chunk.fillLayer(y, blockId);
        }
    }

    /**
     * Détermine le type de bloc selon la hauteur Y.
     *
     * @param y la coordonnée Y (0-255)
     * @return l'ID numérique du bloc approprié
     */
    private int determineBlockType(int y) {
        if (y > surfaceHeight) {
            return airId;
        } else if (y == surfaceHeight) {
            return grassId;
        } else if (y >= surfaceHeight - DIRT_LAYER_DEPTH) {
            return dirtId;
        } else {
            return stoneId;
        }
    }

    // ==================== Validation ====================

    private void validateSurfaceHeight(int height) {
        if (height < 0 || height >= Chunk.HEIGHT) {
            throw new IllegalArgumentException(
                    String.format("Surface height must be between 0 and %d, got: %d",
                            Chunk.HEIGHT - 1, height)
            );
        }
    }

    private int requireBlock(BlockRegistry registry, String blockId) {
        Integer id = registry.getNumericId(blockId);
        if (id == null) {
            throw new IllegalStateException(
                    "Required block '" + blockId + "' is not registered in BlockRegistry"
            );
        }
        return id;
    }

    // ==================== Getters ====================

    /**
     * Retourne la hauteur de surface configurée.
     *
     * @return la hauteur Y de la surface
     */
    public int getSurfaceHeight() {
        return surfaceHeight;
    }
}