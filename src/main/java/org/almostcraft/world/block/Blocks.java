package org.almostcraft.world.block;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Définition et enregistrement des blocs vanilla d'AlmostCraft.
 * <p>
 * Cette classe fournit des constantes publiques pour tous les types de blocs
 * de base du jeu. Les blocs doivent être enregistrés dans un {@link BlockRegistry}
 * via la méthode {@link #register(BlockRegistry)} avant utilisation.
 * </p>
 * <p>
 * Utilisation typique :
 * <pre>{@code
 * BlockRegistry registry = new BlockRegistry();
 * Blocks.register(registry);
 * registry.freeze();
 *
 * // Accès aux blocs
 * BlockType stone = Blocks.STONE;
 * int stoneId = registry.getNumericId(Blocks.STONE.id());
 * }</pre>
 * </p>
 *
 * @author Lucas Préaux
 * @version 1.0
 */
public class Blocks {

    private static final Logger logger = LoggerFactory.getLogger(Blocks.class);

    // ==================== Constantes de blocs ====================

    /**
     * Bloc d'air (vide, invisible, non solide).
     * <p>
     * L'air est le bloc par défaut utilisé pour les espaces vides.
     * Il doit toujours avoir l'ID numérique 0.
     * </p>
     */
    public static final BlockType AIR = new BlockType(
            "almostcraft:air",
            false,  // non solide (passable)
            true,   // transparent
            "textures/blocks/air.png"
    );

    /**
     * Bloc de pierre.
     * <p>
     * Bloc solide et opaque, constituant la roche de base du monde.
     * </p>
     */
    public static final BlockType STONE = new BlockType(
            "almostcraft:stone",
            true,   // solide
            false,  // opaque
            "textures/blocks/stone.png"
    );

    /**
     * Bloc de terre.
     * <p>
     * Bloc solide et opaque, couche intermédiaire du terrain.
     * </p>
     */
    public static final BlockType DIRT = new BlockType(
            "almostcraft:dirt",
            true,   // solide
            false,  // opaque
            "textures/blocks/dirt.png"
    );

    /**
     * Bloc d'herbe.
     * <p>
     * Bloc solide et opaque, couche supérieure du terrain.
     * Texture avec herbe sur le dessus.
     * </p>
     */
    public static final BlockType GRASS = new BlockType(
            "almostcraft:grass_block",
            true,   // solide
            false,  // opaque
            "textures/blocks/grass_block_top.png",
            true
    );

    /**
     * Bloc de roche.
     * <p>
     * Version brute de la pierre, plus rugueuse visuellement.
     * </p>
     */
    public static final BlockType COBBLESTONE = new BlockType(
            "almostcraft:cobblestone",
            true,   // solide
            false,  // opaque
            "textures/blocks/cobblestone.png"
    );

    /**
     * Bloc de sable.
     * <p>
     * Bloc solide qui devrait tomber sous l'effet de la gravité (à implémenter).
     * </p>
     */
    public static final BlockType SAND = new BlockType(
            "almostcraft:sand",
            true,   // solide
            false,  // opaque
            "textures/blocks/sand.png"
    );

    /**
     * Bloc de bois (planches).
     * <p>
     * Matériau de construction de base.
     * </p>
     */
    public static final BlockType PLANKS = new BlockType(
            "almostcraft:oak_planks",
            true,   // solide
            false,  // opaque
            "textures/blocks/oak_planks.png"
    );

    /**
     * Bloc de verre.
     * <p>
     * Bloc solide mais transparent, laisse passer la lumière.
     * </p>
     */
    public static final BlockType GLASS = new BlockType(
            "almostcraft:glass",
            true,   // solide (collision)
            true,   // transparent (lumière)
            "textures/blocks/glass.png"
    );

    // ==================== Enregistrement ====================

    /**
     * Indique si les blocs ont déjà été enregistrés.
     */
    private static boolean registered = false;

    /**
     * Enregistre tous les blocs vanilla dans le registre fourni.
     * <p>
     * Cette méthode doit être appelée UNE SEULE FOIS au démarrage du jeu,
     * avant de figer le registre avec {@link BlockRegistry#freeze()}.
     * </p>
     * <p>
     * <strong>IMPORTANT :</strong> AIR est toujours enregistré en premier
     * pour garantir qu'il reçoive l'ID numérique 0.
     * </p>
     *
     * @param registry le registre dans lequel enregistrer les blocs
     * @throws IllegalStateException si les blocs ont déjà été enregistrés
     */
    public static void register(BlockRegistry registry) {
        if (registered) {
            logger.warn("Attempted to register blocks multiple times, ignoring");
            return;
        }

        logger.info("Registering vanilla blocks...");

        // ORDRE CRITIQUE: AIR doit être enregistré en premier (ID = 0)
        registry.register(AIR);
        logger.debug("AIR registered with ID 0");

        // Enregistrer les autres blocs
        registry.register(STONE);
        registry.register(DIRT);
        registry.register(GRASS);
        registry.register(COBBLESTONE);
        registry.register(SAND);
        registry.register(PLANKS);
        registry.register(GLASS);

        registered = true;
        logger.info("Registered {} vanilla blocks", registry.size());
    }

    // ==================== Utilitaires ====================

    /**
     * Vérifie si les blocs ont été enregistrés.
     *
     * @return true si {@link #register(BlockRegistry)} a été appelé
     */
    public static boolean isRegistered() {
        return registered;
    }

    /**
     * Constructeur privé pour empêcher l'instanciation.
     * <p>
     * Cette classe ne contient que des méthodes et constantes statiques.
     * </p>
     */
    private Blocks() {
        throw new AssertionError("Blocks class cannot be instantiated");
    }
}