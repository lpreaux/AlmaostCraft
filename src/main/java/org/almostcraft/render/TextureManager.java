package org.almostcraft.render;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestionnaire de textures avec cache pour éviter les chargements multiples.
 * <p>
 * Suit le pattern Singleton pour un accès global, et maintient un cache
 * des textures déjà chargées identifiées par leur chemin.
 * </p>
 */
public class TextureManager {

    private static final Logger logger = LoggerFactory.getLogger(TextureManager.class);

    /**
     * Cache des textures chargées (chemin → Texture).
     */
    private final Map<String, Texture> textureCache;

    /**
     * Nombre total de textures chargées (pour stats).
     */
    private int totalTexturesLoaded;

    // ==================== Constructeur ====================

    public TextureManager() {
        this.textureCache = new HashMap<>();
        this.totalTexturesLoaded = 0;
        logger.info("TextureManager initialized");
    }

    // ==================== Chargement ====================

    /**
     * Charge une texture depuis un chemin (avec cache).
     * <p>
     * Si la texture est déjà chargée, retourne l'instance en cache.
     * Sinon, charge la texture et la met en cache.
     * </p>
     *
     * @param path chemin vers la texture (ex: "textures/blocks/stone.png")
     * @return la texture chargée
     * @throws RuntimeException si le chargement échoue
     */
    public Texture getTexture(String path) {
        // Vérifier le cache
        if (textureCache.containsKey(path)) {
            logger.trace("Texture '{}' found in cache", path);
            return textureCache.get(path);
        }

        // Charger la texture
        logger.debug("Loading texture: {}", path);
        try {
            Texture texture = new Texture(path);
            textureCache.put(path, texture);
            totalTexturesLoaded++;

            logger.debug("Texture loaded successfully: {} (total: {})",
                    path, totalTexturesLoaded);

            return texture;
        } catch (IOException e) {
            logger.error("Failed to load texture: {}", path, e);
            throw new RuntimeException("Failed to load texture: " + path, e);
        }
    }

    /**
     * Précharge une texture sans la retourner.
     * <p>
     * Utile pour charger les textures au démarrage.
     * </p>
     */
    public void preloadTexture(String path) {
        getTexture(path); // Appelle getTexture qui gère le cache
    }

    // ==================== Nettoyage ====================

    /**
     * Libère toutes les textures chargées.
     */
    public void cleanup() {
        logger.info("Cleaning up {} textures", textureCache.size());

        for (Texture texture : textureCache.values()) {
            texture.cleanup();
        }

        textureCache.clear();
        logger.info("TextureManager cleaned up");
    }

    // ==================== Getters ====================

    public int getCachedTextureCount() {
        return textureCache.size();
    }

    public int getTotalTexturesLoaded() {
        return totalTexturesLoaded;
    }
}
