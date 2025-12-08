package org.almostcraft.world.block;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Registre central des types de blocs.
 * <p>
 * Gère le mapping bidirectionnel entre les IDs string (ex: "almostcraft:stone")
 * et les IDs numériques utilisés en interne pour le stockage dans les chunks.
 * </p>
 * <p>
 * Thread-safe après l'appel à {@link #freeze()}.
 * </p>
 *
 * @author Lucas Préaux
 * @version 1.0
 */
public class BlockRegistry {

    private static final Logger logger = LoggerFactory.getLogger(BlockRegistry.class);

    // ==================== Attributs ====================

    /**
     * Mapping String → Numeric ID.
     */
    private final Map<String, Integer> stringToNumeric = new HashMap<>();

    /**
     * Mapping Numeric ID → String (inverse).
     */
    private final Map<Integer, String> numericToString = new HashMap<>();

    /**
     * Mapping String → BlockType.
     */
    private final Map<String, BlockType> blocks = new HashMap<>();

    /**
     * Compteur pour l'attribution automatique des IDs numériques.
     */
    private final AtomicInteger nextId = new AtomicInteger(0);

    /**
     * Indique si le registre est figé (plus d'enregistrements autorisés).
     */
    private volatile boolean frozen = false;

    // ==================== Enregistrement ====================

    /**
     * Enregistre un nouveau type de bloc dans le registre.
     * <p>
     * Attribue automatiquement un ID numérique unique au bloc.
     * </p>
     *
     * @param block le type de bloc à enregistrer
     * @throws IllegalStateException si le registre est déjà figé
     * @throws IllegalArgumentException si un bloc avec cet ID existe déjà
     */
    public void register(BlockType block) {
        if (frozen) {
            throw new IllegalStateException(
                    "Cannot register block '" + block.id() + "': registry is frozen"
            );
        }

        String id = block.id();

        if (stringToNumeric.containsKey(id)) {
            throw new IllegalArgumentException(
                    "Block with ID '" + id + "' is already registered"
            );
        }

        int numericId = nextId.getAndIncrement();

        stringToNumeric.put(id, numericId);
        numericToString.put(numericId, id);
        blocks.put(id, block);

        logger.debug("Registered block: '{}' → numeric ID {}", id, numericId);
    }

    /**
     * Fige le registre pour empêcher de nouveaux enregistrements.
     * <p>
     * Après l'appel à cette méthode, toute tentative d'enregistrement
     * lancera une {@link IllegalStateException}.
     * </p>
     */
    public void freeze() {
        if (!frozen) {
            frozen = true;
            logger.info("Block registry frozen with {} blocks registered", blocks.size());
        }
    }

    // ==================== Getters - String ↔ Numeric ====================

    /**
     * Retourne l'ID numérique associé à un ID string.
     *
     * @param stringId l'ID string (ex: "almostcraft:stone")
     * @return l'ID numérique, ou null si non trouvé
     */
    public Integer getNumericId(String stringId) {
        return stringToNumeric.get(stringId);
    }

    /**
     * Retourne l'ID string associé à un ID numérique.
     *
     * @param numericId l'ID numérique
     * @return l'ID string, ou null si non trouvé
     */
    public String getStringId(int numericId) {
        return numericToString.get(numericId);
    }

    /**
     * Retourne le BlockType associé à un ID string.
     *
     * @param stringId l'ID string
     * @return le BlockType, ou null si non trouvé
     */
    public BlockType getBlock(String stringId) {
        return blocks.get(stringId);
    }

    /**
     * Retourne le BlockType associé à un ID numérique.
     *
     * @param numericId l'ID numérique
     * @return le BlockType, ou null si non trouvé
     */
    public BlockType getBlockByNumericId(int numericId) {
        String stringId = numericToString.get(numericId);
        return stringId != null ? blocks.get(stringId) : null;
    }

    // ==================== Vérifications ====================

    /**
     * Vérifie si un bloc est enregistré.
     *
     * @param stringId l'ID string à vérifier
     * @return true si le bloc existe, false sinon
     */
    public boolean exists(String stringId) {
        return stringToNumeric.containsKey(stringId);
    }

    /**
     * Vérifie si un ID numérique est utilisé.
     *
     * @param numericId l'ID numérique à vérifier
     * @return true si l'ID est utilisé, false sinon
     */
    public boolean existsNumericId(int numericId) {
        return numericToString.containsKey(numericId);
    }

    // ==================== Utilitaires ====================

    /**
     * Retourne le nombre de blocs enregistrés.
     *
     * @return le nombre de blocs
     */
    public int size() {
        return blocks.size();
    }

    /**
     * Retourne une copie immuable de tous les mappings String → Numeric.
     * <p>
     * Utile pour créer des snapshots lors de la sauvegarde.
     * </p>
     *
     * @return une map immuable des mappings
     */
    public Map<String, Integer> getAllMappings() {
        return Collections.unmodifiableMap(new HashMap<>(stringToNumeric));
    }

    /**
     * Retourne une copie immuable de tous les blocs enregistrés.
     *
     * @return une collection immuable de BlockType
     */
    public Collection<BlockType> getAllBlocks() {
        return Collections.unmodifiableCollection(blocks.values());
    }
}