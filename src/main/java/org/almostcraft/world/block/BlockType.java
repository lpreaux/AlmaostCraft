package org.almostcraft.world.block;

import java.util.Map;

/**
 * Représente un type de bloc dans le jeu.
 * <p>
 * Un BlockType est une définition immuable des propriétés d'un bloc.
 * Chaque type de bloc est enregistré dans le {@link BlockRegistry} et
 * identifié par un ID unique au format "namespace:path".
 * </p>
 * <p>
 * Exemple avec texture unique :
 * <pre>{@code
 * BlockType stone = new BlockType(
 *     "almostcraft:stone",
 *     true,   // solid
 *     false,  // transparent
 *     "textures/blocks/stone.png"
 * );
 * }</pre>
 * </p>
 * <p>
 * Exemple avec textures par face :
 * <pre>{@code
 * BlockType grass = BlockType.builder("almostcraft:grass_block")
 *     .solid(true)
 *     .textureTop("textures/blocks/grass_block_top.png")
 *     .textureSide("textures/blocks/grass_block_side.png")
 *     .textureBottom("textures/blocks/dirt.png")
 *     .tinted(true)
 *     .build();
 * }</pre>
 * </p>
 *
 * @author Lucas Préaux
 * @version 2.0
 */
public record BlockType(
        String id,
        boolean isSolid,
        boolean isTransparent,
        Map<BlockFace, String> texturesByFace,
        boolean isTinted
) {

    // ==================== Enum des faces ====================

    /**
     * Représente les différentes faces d'un bloc.
     */
    public enum BlockFace {
        TOP,
        BOTTOM,
        NORTH,
        SOUTH,
        EAST,
        WEST;

        /**
         * Retourne la face SIDE générique (utilisée quand toutes les faces latérales sont identiques).
         */
        public static BlockFace SIDE = NORTH;
    }

    // ==================== Constructeur compact avec validation ====================

    /**
     * Constructeur compact qui valide les paramètres.
     */
    public BlockType {
        validateId(id);
        validateTextures(texturesByFace);
        // Rendre la map immuable
        texturesByFace = Map.copyOf(texturesByFace);
    }

    // ==================== Constructeurs de commodité ====================

    /**
     * Constructeur avec texture unique pour toutes les faces.
     */
    public BlockType(String id, boolean isSolid, boolean isTransparent, String texturePath, boolean isTinted) {
        this(id, isSolid, isTransparent, createUniformTextureMap(texturePath), isTinted);
    }

    /**
     * Constructeur avec texture unique, sans tint.
     */
    public BlockType(String id, boolean isSolid, boolean isTransparent, String texturePath) {
        this(id, isSolid, isTransparent, texturePath, false);
    }

    /**
     * Crée une map avec la même texture pour toutes les faces.
     */
    private static Map<BlockFace, String> createUniformTextureMap(String texturePath) {
        return Map.of(
                BlockFace.TOP, texturePath,
                BlockFace.BOTTOM, texturePath,
                BlockFace.NORTH, texturePath,
                BlockFace.SOUTH, texturePath,
                BlockFace.EAST, texturePath,
                BlockFace.WEST, texturePath
        );
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

    private static void validateTextures(Map<BlockFace, String> texturesByFace) {
        if (texturesByFace == null || texturesByFace.isEmpty()) {
            throw new IllegalArgumentException("Textures map cannot be null or empty");
        }

        // Vérifier que toutes les faces ont une texture
        for (BlockFace face : BlockFace.values()) {
            String texturePath = texturesByFace.get(face);
            if (texturePath == null || texturePath.isEmpty()) {
                throw new IllegalArgumentException(
                        String.format("Missing texture for face %s", face)
                );
            }
        }
    }

    // ==================== Méthodes d'accès aux textures ====================

    /**
     * Retourne le chemin de texture pour une face spécifique.
     *
     * @param face la face du bloc
     * @return le chemin de la texture
     */
    public String getTexturePath(BlockFace face) {
        return texturesByFace.get(face);
    }

    /**
     * Retourne le chemin de texture pour le dessus du bloc.
     */
    public String getTextureTop() {
        return texturesByFace.get(BlockFace.TOP);
    }

    /**
     * Retourne le chemin de texture pour le dessous du bloc.
     */
    public String getTextureBottom() {
        return texturesByFace.get(BlockFace.BOTTOM);
    }

    /**
     * Retourne le chemin de texture pour une face latérale.
     * Par défaut, retourne la texture NORTH.
     */
    public String getTextureSide() {
        return texturesByFace.get(BlockFace.NORTH);
    }

    /**
     * Retourne le chemin de texture par défaut (compatibilité).
     * Retourne la texture du dessus par défaut.
     */
    public String getTexturePath() {
        return getTextureTop();
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

    // ==================== Builder ====================

    /**
     * Crée un builder pour construire un BlockType avec textures personnalisées par face.
     */
    public static Builder builder(String id) {
        return new Builder(id);
    }

    /**
     * Builder pour créer des BlockType avec textures par face.
     */
    public static class Builder {
        private final String id;
        private boolean isSolid = true;
        private boolean isTransparent = false;
        private boolean isTinted = false;
        private String textureTop;
        private String textureBottom;
        private String textureNorth;
        private String textureSouth;
        private String textureEast;
        private String textureWest;

        private Builder(String id) {
            this.id = id;
        }

        public Builder solid(boolean solid) {
            this.isSolid = solid;
            return this;
        }

        public Builder transparent(boolean transparent) {
            this.isTransparent = transparent;
            return this;
        }

        public Builder tinted(boolean tinted) {
            this.isTinted = tinted;
            return this;
        }

        public Builder textureTop(String path) {
            this.textureTop = path;
            return this;
        }

        public Builder textureBottom(String path) {
            this.textureBottom = path;
            return this;
        }

        public Builder textureNorth(String path) {
            this.textureNorth = path;
            return this;
        }

        public Builder textureSouth(String path) {
            this.textureSouth = path;
            return this;
        }

        public Builder textureEast(String path) {
            this.textureEast = path;
            return this;
        }

        public Builder textureWest(String path) {
            this.textureWest = path;
            return this;
        }

        /**
         * Définit la même texture pour toutes les faces latérales (N, S, E, W).
         */
        public Builder textureSide(String path) {
            this.textureNorth = path;
            this.textureSouth = path;
            this.textureEast = path;
            this.textureWest = path;
            return this;
        }

        /**
         * Définit la même texture pour toutes les faces.
         */
        public Builder textureAll(String path) {
            this.textureTop = path;
            this.textureBottom = path;
            this.textureNorth = path;
            this.textureSouth = path;
            this.textureEast = path;
            this.textureWest = path;
            return this;
        }

        public BlockType build() {
            // Remplir les textures manquantes avec des valeurs par défaut
            if (textureTop == null) textureTop = textureNorth;
            if (textureBottom == null) textureBottom = textureNorth;
            if (textureSouth == null) textureSouth = textureNorth;
            if (textureEast == null) textureEast = textureNorth;
            if (textureWest == null) textureWest = textureNorth;

            Map<BlockFace, String> textureMap = Map.of(
                    BlockFace.TOP, textureTop,
                    BlockFace.BOTTOM, textureBottom,
                    BlockFace.NORTH, textureNorth,
                    BlockFace.SOUTH, textureSouth,
                    BlockFace.EAST, textureEast,
                    BlockFace.WEST, textureWest
            );

            return new BlockType(id, isSolid, isTransparent, textureMap, isTinted);
        }
    }
}
