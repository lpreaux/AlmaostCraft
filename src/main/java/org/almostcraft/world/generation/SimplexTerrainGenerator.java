package org.almostcraft.world.generation;


import com.github.fastnoise.FastNoise;
import com.github.fastnoise.FloatArray;
import org.almostcraft.world.block.BlockRegistry;
import org.almostcraft.world.block.Blocks;
import org.almostcraft.world.chunk.Chunk;
import org.almostcraft.world.utils.CoordinateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Générateur de terrain utilisant le bruit de Simplex pour créer des collines et vallées naturelles.
 * <p>
 * Utilise FastNoise2 pour générer une heightmap 2D, puis place les blocs en stratification
 * classique (pierre, terre, herbe). La génération est déterministe : une même seed produira
 * toujours le même terrain.
 * </p>
 * <p>
 * Configuration des paramètres :
 * <ul>
 *   <li><strong>Frequency</strong> : Contrôle la taille des collines (0.01 = grandes, 0.1 = petites)</li>
 *   <li><strong>Min/Max Height</strong> : Plage de hauteur du terrain (ex: 50-80)</li>
 *   <li><strong>Dirt Depth</strong> : Épaisseur de la couche de terre sous l'herbe</li>
 *   <li><strong>Seed</strong> : Graine pour la génération procédurale</li>
 * </ul>
 * </p>
 *
 * @author Lucas Préaux
 * @version 1.0
 */
public class SimplexTerrainGenerator implements TerrainGenerator {

    private static final Logger logger = LoggerFactory.getLogger(SimplexTerrainGenerator.class);

    // ==================== Constantes par défaut ====================

    /**
     * Fréquence par défaut du bruit (0.01 = grandes collines douces).
     * <p>
     * Valeurs typiques :
     * - 0.005 : très grandes collines, terrain plat
     * - 0.01 : grandes collines naturelles (recommandé)
     * - 0.02 : collines moyennes
     * - 0.05 : terrain vallonné
     * - 0.1+ : terrain chaotique, montagnes russes
     * </p>
     */
    private static final float DEFAULT_FREQUENCY = 0.0054f;

    /**
     * Hauteur minimale du terrain (Y minimum pour la surface).
     */
    private static final int DEFAULT_MIN_HEIGHT = 50;

    /**
     * Hauteur maximale du terrain (Y maximum pour la surface).
     */
    private static final int DEFAULT_MAX_HEIGHT = 80;

    /**
     * Profondeur de la couche de terre sous l'herbe (en blocs).
     */
    private static final int DEFAULT_DIRT_DEPTH = 3;

    /**
     * Graine par défaut pour la génération.
     */
    private static final long DEFAULT_SEED = 12345L;

    // ==================== Attributs de configuration ====================

    /**
     * Fréquence du bruit de Simplex (contrôle la taille des collines).
     */
    private final float frequency;

    /**
     * Hauteur minimale de la surface du terrain.
     */
    private final int minTerrainHeight;

    /**
     * Hauteur maximale de la surface du terrain.
     */
    private final int maxTerrainHeight;

    /**
     * Profondeur de la couche de terre sous l'herbe.
     */
    private final int dirtLayerDepth;

    /**
     * Graine pour la génération procédurale (converti en int pour FastNoise).
     */
    private final int seed;

    // ==================== IDs des blocs ====================

    private final int airId;
    private final int grassId;
    private final int dirtId;
    private final int stoneId;

    // ==================== Constructeurs ====================

    /**
     * Crée un générateur de terrain avec paramètres par défaut.
     *
     * @param blockRegistry le registre de blocs
     */
    public SimplexTerrainGenerator(BlockRegistry blockRegistry) {
        this(blockRegistry, DEFAULT_SEED);
    }

    /**
     * Crée un générateur de terrain avec une seed personnalisée.
     *
     * @param blockRegistry le registre de blocs
     * @param seed          la graine pour la génération
     */
    public SimplexTerrainGenerator(BlockRegistry blockRegistry, long seed) {
        this(blockRegistry, seed, DEFAULT_FREQUENCY, DEFAULT_MIN_HEIGHT,
                DEFAULT_MAX_HEIGHT, DEFAULT_DIRT_DEPTH);
    }

    /**
     * Crée un générateur de terrain avec tous les paramètres personnalisés.
     *
     * @param blockRegistry le registre de blocs
     * @param seed          la graine pour la génération
     * @param frequency     la fréquence du bruit (0.005 à 0.1)
     * @param minHeight     la hauteur minimale du terrain (0-255)
     * @param maxHeight     la hauteur maximale du terrain (0-255)
     * @param dirtDepth     l'épaisseur de la couche de terre (1-10)
     * @throws IllegalArgumentException si les paramètres sont invalides
     * @throws IllegalStateException    si un bloc requis n'est pas enregistré
     */
    public SimplexTerrainGenerator(BlockRegistry blockRegistry, long seed,
                                   float frequency, int minHeight,
                                   int maxHeight, int dirtDepth) {
        validateParameters(frequency, minHeight, maxHeight, dirtDepth);

        this.seed = (int) seed;
        this.frequency = frequency;
        this.minTerrainHeight = minHeight;
        this.maxTerrainHeight = maxHeight;
        this.dirtLayerDepth = dirtDepth;

        // Récupérer et valider les IDs de blocs
        this.airId = requireBlock(blockRegistry, Blocks.AIR.id());
        this.grassId = requireBlock(blockRegistry, Blocks.GRASS.id());
        this.dirtId = requireBlock(blockRegistry, Blocks.DIRT.id());
        this.stoneId = requireBlock(blockRegistry, Blocks.STONE.id());

        logger.info("SimplexTerrainGenerator created: seed={}, frequency={}, height=[{}-{}], dirtDepth={}",
                seed, frequency, minHeight, maxHeight, dirtDepth);
    }

    // ==================== Génération ====================

    /**
     * Génère le terrain pour un chunk donné.
     * <p>
     * Processus en deux étapes :
     * 1. Génération d'une heightmap 2D via bruit de Simplex
     * 2. Placement des blocs en fonction de la heightmap
     * </p>
     *
     * @param chunk  le chunk à remplir
     * @param chunkX coordonnée X du chunk
     * @param chunkZ coordonnée Z du chunk
     */
    @Override
    public void generate(Chunk chunk, int chunkX, int chunkZ) {
        logger.trace("Generating Simplex terrain for chunk ({}, {})", chunkX, chunkZ);

        // Étape 1 : Générer la heightmap
        int[][] heightMap = generateHeightMap(chunkX, chunkZ);

        // Étape 2 : Placer les blocs
        placeBlocks(chunk, heightMap);

        logger.trace("Chunk ({}, {}) generated successfully", chunkX, chunkZ);
    }

    // ==================== Génération de la heightmap ====================

    /**
     * Génère une heightmap 2D pour le chunk spécifié.
     * <p>
     * Utilise FastNoise2 avec algorithme Simplex pour créer du bruit cohérent.
     * Les valeurs de bruit (-1 à +1) sont converties en hauteurs de terrain.
     * </p>
     *
     * @param chunkX coordonnée X du chunk
     * @param chunkZ coordonnée Z du chunk
     * @return tableau [16][16] contenant les hauteurs de surface (0-255)
     */
    private int[][] generateHeightMap(int chunkX, int chunkZ) {
        int[][] heightMap = new int[Chunk.WIDTH][Chunk.DEPTH];

        try (FastNoise noise = new FastNoise("Simplex")) {
            // Tableau pour recevoir les valeurs de bruit (16x16 = 256 valeurs)
            FloatArray noiseOutput = new FloatArray(Chunk.WIDTH * Chunk.DEPTH);

            // Calculer les coordonnées mondiales du coin inférieur gauche du chunk
            int worldX = CoordinateUtil.chunkToWorldX(chunkX);
            int worldZ = CoordinateUtil.chunkToWorldZ(chunkZ);

            // Générer le bruit 2D sur une grille de 16x16
            noise.genUniformGrid2D(
                    noiseOutput,     // Tableau de sortie
                    worldX,          // Position X de départ
                    worldZ,          // Position Z de départ
                    Chunk.WIDTH,     // Nombre de points sur l'axe X (16)
                    Chunk.DEPTH,     // Nombre de points sur l'axe Z (16)
                    frequency,       // Fréquence du bruit
                    seed             // Graine
            );

            // Convertir les valeurs de bruit en hauteurs de terrain
            int index = 0;
            for (int z = 0; z < Chunk.DEPTH; z++) {
                for (int x = 0; x < Chunk.WIDTH; x++) {
                    float noiseValue = noiseOutput.get(index++);
                    heightMap[x][z] = noiseToHeight(noiseValue);
                }
            }

            // Log de débogage pour le premier chunk
            if (logger.isDebugEnabled() && chunkX == 0 && chunkZ == 0) {
                logHeightMapStats(heightMap, chunkX, chunkZ);
            }
        }

        return heightMap;
    }

    /**
     * Convertit une valeur de bruit de Simplex en hauteur de terrain.
     * <p>
     * Le bruit de Simplex retourne des valeurs entre -1.0 et +1.0.
     * Cette méthode les normalise vers [0, 1], puis les mappe vers
     * la plage [minTerrainHeight, maxTerrainHeight].
     * </p>
     *
     * @param noiseValue valeur du bruit (-1.0 à +1.0)
     * @return hauteur Y correspondante (entre minTerrainHeight et maxTerrainHeight)
     */
    private int noiseToHeight(float noiseValue) {
        // Étape 1 : Normaliser de [-1, 1] vers [0, 1]
        float normalized = (noiseValue + 1.0f) / 2.0f;

        // Étape 2 : Mapper vers la plage de hauteur souhaitée
        int range = maxTerrainHeight - minTerrainHeight;
        int height = minTerrainHeight + (int) (normalized * range);

        // Étape 3 : Clamp pour garantir qu'on reste dans [0, 255]
        return Math.max(0, Math.min(Chunk.HEIGHT - 1, height));
    }

    // ==================== Placement des blocs ====================

    /**
     * Place les blocs dans le chunk selon la heightmap.
     * <p>
     * Pour chaque colonne (x, z), remplit verticalement de Y=0 à Y=255
     * en fonction de la hauteur de surface :
     * - Air au-dessus de la surface
     * - Herbe à la surface
     * - Terre juste en dessous (3 couches par défaut)
     * - Pierre en profondeur
     * </p>
     *
     * @param chunk     le chunk à remplir
     * @param heightMap la heightmap [16][16] à utiliser
     */
    private void placeBlocks(Chunk chunk, int[][] heightMap) {
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                int surfaceHeight = heightMap[x][z];

                // Remplir la colonne verticale
                for (int y = 0; y < Chunk.HEIGHT; y++) {
                    int blockId = determineBlockType(y, surfaceHeight);
                    chunk.setVoxel(x, y, z, blockId);
                }
            }
        }
    }

    /**
     * Détermine le type de bloc à placer selon la position Y et la hauteur de surface.
     * <p>
     * Stratification standard :
     * <ul>
     *   <li>Y > surface : Air</li>
     *   <li>Y == surface : Herbe</li>
     *   <li>surface - dirtDepth <= Y < surface : Terre</li>
     *   <li>Y < surface - dirtDepth : Pierre</li>
     * </ul>
     * </p>
     *
     * @param y             la coordonnée Y du bloc
     * @param surfaceHeight la hauteur de la surface pour cette colonne
     * @return l'ID numérique du bloc approprié
     */
    private int determineBlockType(int y, int surfaceHeight) {
        if (y > surfaceHeight) {
            return airId;
        } else if (y == surfaceHeight) {
            return grassId;
        } else if (y >= surfaceHeight - dirtLayerDepth) {
            return dirtId;
        } else {
            return stoneId;
        }
    }

    // ==================== Validation ====================

    /**
     * Valide les paramètres du générateur.
     *
     * @throws IllegalArgumentException si un paramètre est invalide
     */
    private void validateParameters(float frequency, int minHeight, int maxHeight, int dirtDepth) {
        if (frequency <= 0 || frequency > 1.0f) {
            throw new IllegalArgumentException(
                    String.format("Frequency must be between 0 and 1, got: %.4f", frequency)
            );
        }

        if (minHeight < 0 || minHeight >= Chunk.HEIGHT) {
            throw new IllegalArgumentException(
                    String.format("Min height must be between 0 and %d, got: %d",
                            Chunk.HEIGHT - 1, minHeight)
            );
        }

        if (maxHeight < 0 || maxHeight >= Chunk.HEIGHT) {
            throw new IllegalArgumentException(
                    String.format("Max height must be between 0 and %d, got: %d",
                            Chunk.HEIGHT - 1, maxHeight)
            );
        }

        if (minHeight >= maxHeight) {
            throw new IllegalArgumentException(
                    String.format("Min height (%d) must be less than max height (%d)",
                            minHeight, maxHeight)
            );
        }

        if (dirtDepth < 1 || dirtDepth > 20) {
            throw new IllegalArgumentException(
                    String.format("Dirt depth must be between 1 and 20, got: %d", dirtDepth)
            );
        }
    }

    /**
     * Vérifie qu'un bloc est enregistré et retourne son ID.
     *
     * @param registry le registre de blocs
     * @param blockId  l'ID string du bloc
     * @return l'ID numérique du bloc
     * @throws IllegalStateException si le bloc n'est pas enregistré
     */
    private int requireBlock(BlockRegistry registry, String blockId) {
        Integer id = registry.getNumericId(blockId);
        if (id == null) {
            throw new IllegalStateException(
                    "Required block '" + blockId + "' is not registered in BlockRegistry"
            );
        }
        return id;
    }

    // ==================== Logging et débogage ====================

    /**
     * Log les statistiques de la heightmap pour le débogage.
     * <p>
     * Affiche les hauteurs min/max et quelques échantillons.
     * Appelé uniquement pour le chunk (0, 0) en mode DEBUG.
     * </p>
     */
    private void logHeightMapStats(int[][] heightMap, int chunkX, int chunkZ) {
        int min = Arrays.stream(heightMap)
                .flatMapToInt(Arrays::stream)
                .min()
                .orElse(0);

        int max = Arrays.stream(heightMap)
                .flatMapToInt(Arrays::stream)
                .max()
                .orElse(0);

        logger.debug("HeightMap stats for chunk ({}, {}):", chunkX, chunkZ);
        logger.debug("  Min height: {}, Max height: {}, Range: {}", min, max, max - min);
        logger.debug("  Samples: [0][0]={}, [8][8]={}, [15][15]={}",
                heightMap[0][0], heightMap[8][8], heightMap[15][15]);
    }

    // ==================== Getters ====================

    /**
     * Retourne la fréquence du bruit utilisée.
     *
     * @return la fréquence
     */
    public float getFrequency() {
        return frequency;
    }

    /**
     * Retourne la hauteur minimale du terrain.
     *
     * @return la hauteur minimale (Y)
     */
    public int getMinTerrainHeight() {
        return minTerrainHeight;
    }

    /**
     * Retourne la hauteur maximale du terrain.
     *
     * @return la hauteur maximale (Y)
     */
    public int getMaxTerrainHeight() {
        return maxTerrainHeight;
    }

    /**
     * Retourne la profondeur de la couche de terre.
     *
     * @return l'épaisseur en blocs
     */
    public int getDirtLayerDepth() {
        return dirtLayerDepth;
    }

    /**
     * Retourne la graine utilisée pour la génération.
     *
     * @return la seed
     */
    public long getSeed() {
        return seed;
    }
}