package org.almostcraft.render;

import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL32.GL_GEOMETRY_SHADER;

/**
 * Types de shaders supportés par OpenGL.
 *
 * @author Lucas Préaux
 * @version 1.0
 */
public enum ShaderType {
    /**
     * Shader de vertex (traite chaque sommet).
     */
    VERTEX(GL_VERTEX_SHADER, ".vert"),

    /**
     * Shader de fragment (traite chaque pixel).
     */
    FRAGMENT(GL_FRAGMENT_SHADER, ".frag"),

    /**
     * Shader de géométrie (optionnel, génère des primitives).
     */
    GEOMETRY(GL_GEOMETRY_SHADER, ".geom");

    /**
     * Constante OpenGL correspondante.
     */
    private final int glType;

    /**
     * Extension de fichier standard.
     */
    private final String extension;

    ShaderType(int glType, String extension) {
        this.glType = glType;
        this.extension = extension;
    }

    /**
     * Retourne le type OpenGL (ex: GL_VERTEX_SHADER).
     */
    public int getGlType() {
        return glType;
    }

    /**
     * Retourne l'extension de fichier (ex: ".vert").
     */
    public String getExtension() {
        return extension;
    }
}