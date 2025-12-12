package org.almostcraft.core.subsystems;

import org.almostcraft.world.ChunkLoader;
import org.almostcraft.world.World;
import org.almostcraft.world.block.BlockRegistry;
import org.almostcraft.world.block.Blocks;
import org.almostcraft.world.generation.SimplexTerrainGenerator;
import org.almostcraft.world.generation.TerrainGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Subsystem gérant le monde du jeu : registre de blocs, génération de terrain.
 * <p>
 * Le chargement des chunks est géré séparément par {@link ChunkLoader} car il dépend
 * de la caméra (position du joueur) et du renderer.
 * </p>
 */
public class WorldSubsystem implements Subsystem {
    private static final Logger logger = LoggerFactory.getLogger(WorldSubsystem.class);

    // ==================== Attributs ====================

    private final BlockRegistry blockRegistry;
    private World world;

    // ==================== Constructeur ====================

    public WorldSubsystem() {
        logger.debug("Creating world subsystem");

        // Créer et initialiser le registre de blocs
        this.blockRegistry = new BlockRegistry();
        Blocks.register(blockRegistry);
        blockRegistry.freeze();

        // Créer le générateur de terrain
        TerrainGenerator generator = createTerrainGenerator();

        // Créer le monde
        world = new World(generator, blockRegistry);

        logger.debug("World subsystem created with {} blocks registered",
                blockRegistry.size());
    }

    // ==================== Lifecycle ====================

    @Override
    public void initialize() {
        logger.debug("Initializing world subsystem");

        // Pour l'instant, rien à initialiser
        // Cette méthode pourrait servir pour :
        // - Précharger certains chunks
        // - Initialiser des systèmes de sauvegarde
        // - etc.

        logger.debug("World subsystem initialized");
    }

    @Override
    public void update(float deltaTime) {
        // Le monde n'a pas besoin d'update pour le moment
        // (la logique de chunk loading est dans ChunkLoader)
    }

    @Override
    public void cleanup() {
        logger.info("Cleaning up world subsystem");
        // Rien à nettoyer pour le moment (les chunks sont gérés par ChunkLoader)
        logger.debug("World subsystem cleaned up");
    }

    // ==================== Helpers ====================

    /**
     * Crée le générateur de terrain pour le monde.
     * <p>
     * Méthode séparée pour faciliter le changement de générateur.
     * </p>
     */
    private TerrainGenerator createTerrainGenerator() {
        // Choix du générateur (commenter/décommenter selon besoin)

        // Terrain plat (pour tests)
        // return new FlatTerrainGenerator(blockRegistry);

        // Terrain Simplex standard
        return new SimplexTerrainGenerator(blockRegistry);

        // Terrain Simplex personnalisé
        // return new SimplexTerrainGenerator(blockRegistry, 12345, 0.007f, 40, 220, 7);
    }

    // ==================== Getters ====================

    /**
     * Retourne le monde généré.
     * <p>
     * Disponible après l'appel à {@link #initialize()}.
     * </p>
     */
    public World getWorld() {
        if (world == null) {
            throw new IllegalStateException("World not initialized - call initialize() first");
        }
        return world;
    }

    /**
     * Retourne le registre de blocs.
     * <p>
     * Disponible dès la construction.
     * </p>
     */
    public BlockRegistry getBlockRegistry() {
        return blockRegistry;
    }
}
