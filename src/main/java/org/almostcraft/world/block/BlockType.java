package org.almostcraft.world.block;

/**
 * Représente un type de bloc dans le jeu.
 * <p>
 * Un BlockType est une définition immuable des propriétés d'un bloc.
 * Chaque type de bloc est enregistré dans le {@link BlockRegistry} et
 * identifié par un ID unique au format "namespace:path".
 * </p>
 * <p>
 * Exemple :
 * <pre>{@code
 * BlockType stone = new BlockType(
 *     "almostcraft:stone",
 *     true,   // solid
 *     false,  // transparent
 *     "textures/blocks/stone.png"
 * );
 * }</pre>
 * </p>
 *
 * @param id            l'identifiant unique du bloc (format: "namespace:path")
 * @param isSolid       si le bloc est solide (collision physique)
 * @param isTransparent si le bloc est transparent (laisse passer la lumière)
 * @param texturePath   le chemin vers la texture du bloc
 * @author Lucas Préaux
 * @version 1.0
 */
public record BlockType(
        String id,
        boolean isSolid,
        boolean isTransparent,
        String texturePath,
        boolean isTinted
) {

    // ==================== Constructeur compact avec validation ====================

    /**
     * Constructeur compact qui valide les paramètres.
     */
    public BlockType {
        validateId(id);
        validateTexturePath(texturePath);
    }

    /**
     * Constructeur sans tint (par défaut = false).
     */
    public BlockType(String id, boolean isSolid, boolean isTransparent, String texturePath) {
        this(id, isSolid, isTransparent, texturePath, false);
    }

    // ==================== Validation ====================

    private static void validateId(String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Block ID cannot be null or empty");
        }

        if (!id.contains(":")) {
            throw new IllegalArgumentException(
                    String.format("Block ID must be namespaced (format: 'namespace:path'), got: '%s'", id)
            );
        }

        String[] parts = id.split(":");
        if (parts.length != 2 || parts[0].isEmpty() || parts[1].isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("Invalid block ID format: '%s'", id)
            );
        }

        // Validation des caractères (lowercase, underscores, chiffres)
        if (!parts[0].matches("[a-z0-9_]+")) {
            throw new IllegalArgumentException(
                    String.format("Invalid namespace in block ID '%s': must contain only [a-z0-9_]", id)
            );
        }

        if (!parts[1].matches("[a-z0-9_/]+")) {
            throw new IllegalArgumentException(
                    String.format("Invalid path in block ID '%s': must contain only [a-z0-9_/]", id)
            );
        }
    }

    private static void validateTexturePath(String texturePath) {
        if (texturePath == null || texturePath.isEmpty()) {
            throw new IllegalArgumentException("Texture path cannot be null or empty");
        }
    }

    // ==================== Méthodes utilitaires ====================

    /**
     * Extrait le namespace de l'ID du bloc.
     *
     * @return le namespace (ex: "almostcraft" pour "almostcraft:stone")
     */
    public String getNamespace() {
        return id.substring(0, id.indexOf(':'));
    }

    /**
     * Extrait le path de l'ID du bloc.
     *
     * @return le path (ex: "stone" pour "almostcraft:stone")
     */
    public String getPath() {
        return id.substring(id.indexOf(':') + 1);
    }

    /**
     * Vérifie si le bloc est opaque (ne laisse pas passer la lumière).
     *
     * @return true si le bloc est opaque, false sinon
     */
    public boolean isOpaque() {
        return !isTransparent;
    }

    /**
     * Vérifie si le bloc peut être traversé par le joueur.
     *
     * @return true si le bloc est passable, false sinon
     */
    public boolean isPassable() {
        return !isSolid;
    }

    public String getTexturePath() {
        return texturePath;
    }
}