package org.almostcraft.render.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Représente un mesh (maillage) 3D avec ses vertices et indices.
 * <p>
 * Format des vertices pour texture array :
 * Position (3) + TextureIndex (1) + TexCoord (2) + TintColor (3) = 9 floats
 * </p>
 *
 * @author Lucas Préaux
 * @version 2.0 (avec TextureArray)
 */
public class Mesh {

    private static final Logger logger = LoggerFactory.getLogger(Mesh.class);

    // ==================== Constantes ====================

    /**
     * Nombre de floats par vertex.
     * Position (3) + TextureIndex (1) + TexCoord (2) + TintColor (3) = 9
     */
    private static final int VERTEX_SIZE = 9;

    // ==================== Attributs OpenGL ====================

    /**
     * Vertex Array Object - Contient la configuration des attributs.
     */
    private int vao;

    /**
     * Vertex Buffer Object - Contient les données des vertices.
     */
    private int vbo;

    /**
     * Element Buffer Object - Contient les indices des triangles.
     */
    private int ebo;

    /**
     * Nombre d'indices à rendre (= nombre de triangles × 3).
     */
    private int indexCount;

    /**
     * Indique si le mesh a été uploadé sur le GPU.
     */
    private boolean uploaded;

    // ==================== Constructeur ====================

    /**
     * Crée un nouveau mesh vide.
     * <p>
     * Les buffers OpenGL ne sont créés que lors du premier upload.
     * </p>
     */
    public Mesh() {
        this.vao = 0;
        this.vbo = 0;
        this.ebo = 0;
        this.indexCount = 0;
        this.uploaded = false;
    }

    // ==================== Upload des données ====================

    /**
     * Upload les vertices et indices sur le GPU.
     * <p>
     * Format attendu des vertices : [x, y, z, texIndex, u, v, r, g, b, ...]
     * <ul>
     *   <li>Position (x, y, z) : 3 floats</li>
     *   <li>Texture Index : 1 float</li>
     *   <li>Coordonnées UV (u, v) : 2 floats</li>
     *   <li>Couleur de teinte (r, g, b) : 3 floats</li>
     * </ul>
     * Soit 9 floats par vertex.
     * </p>
     * <p>
     * Les indices définissent les triangles (3 indices par triangle).
     * </p>
     *
     * @param vertices tableau des vertices (format: x,y,z,texIndex,u,v,r,g,b)
     * @param indices  tableau des indices des triangles
     * @throws IllegalArgumentException si les tableaux sont vides ou null
     */
    public void uploadData(float[] vertices, int[] indices) {
        if (vertices == null || vertices.length == 0) {
            throw new IllegalArgumentException("Vertices array cannot be null or empty");
        }
        if (indices == null || indices.length == 0) {
            throw new IllegalArgumentException("Indices array cannot be null or empty");
        }
        if (vertices.length % VERTEX_SIZE != 0) {
            throw new IllegalArgumentException(
                    String.format("Vertices array length must be multiple of %d " +
                                    "(x,y,z,texIndex,u,v,r,g,b), got: %d",
                            VERTEX_SIZE, vertices.length)
            );
        }

        logger.debug("Uploading mesh data: {} vertices, {} indices",
                vertices.length / VERTEX_SIZE, indices.length);

        // Créer les buffers si c'est le premier upload
        if (!uploaded) {
            createBuffers();
        }

        // Binder le VAO
        glBindVertexArray(vao);

        // ==================== VBO : Vertices ====================
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        // Configurer les attributs
        int stride = VERTEX_SIZE * Float.BYTES;

        // Attribut 0 : Position (x, y, z)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);

        // Attribut 1 : Texture Index (float, sera casté en int dans le shader)
        glVertexAttribPointer(1, 1, GL_FLOAT, false, stride, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        // Attribut 2 : Coordonnées UV (u, v)
        glVertexAttribPointer(2, 2, GL_FLOAT, false, stride, 4 * Float.BYTES);
        glEnableVertexAttribArray(2);

        // Attribut 3 : Couleur de teinte (r, g, b)
        glVertexAttribPointer(3, 3, GL_FLOAT, false, stride, 6 * Float.BYTES);
        glEnableVertexAttribArray(3);

        // ==================== EBO : Indices ====================
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

        // Sauvegarder le nombre d'indices
        this.indexCount = indices.length;

        // Débinder
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        uploaded = true;
        logger.debug("Mesh uploaded successfully: VAO={}, VBO={}, EBO={}, {} triangles",
                vao, vbo, ebo, getTriangleCount());
    }

    /**
     * Variante avec FloatBuffer et IntBuffer (plus efficace pour de grandes données).
     *
     * @param vertices buffer des vertices
     * @param indices  buffer des indices
     */
    public void uploadData(FloatBuffer vertices, IntBuffer indices) {
        if (vertices == null || vertices.remaining() == 0) {
            throw new IllegalArgumentException("Vertices buffer cannot be null or empty");
        }
        if (indices == null || indices.remaining() == 0) {
            throw new IllegalArgumentException("Indices buffer cannot be null or empty");
        }
        if (vertices.remaining() % VERTEX_SIZE != 0) {
            throw new IllegalArgumentException(
                    String.format("Vertices buffer size must be multiple of %d, got: %d",
                            VERTEX_SIZE, vertices.remaining())
            );
        }

        logger.debug("Uploading mesh data: {} vertices, {} indices",
                vertices.remaining() / VERTEX_SIZE, indices.remaining());

        // Créer les buffers si nécessaire
        if (!uploaded) {
            createBuffers();
        }

        glBindVertexArray(vao);

        // VBO
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        int stride = VERTEX_SIZE * Float.BYTES;

        // Attribut 0 : Position
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);

        // Attribut 1 : Texture Index
        glVertexAttribPointer(1, 1, GL_FLOAT, false, stride, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        // Attribut 2 : UV
        glVertexAttribPointer(2, 2, GL_FLOAT, false, stride, 4 * Float.BYTES);
        glEnableVertexAttribArray(2);

        // Attribut 3 : Tint Color
        glVertexAttribPointer(3, 3, GL_FLOAT, false, stride, 6 * Float.BYTES);
        glEnableVertexAttribArray(3);

        // EBO
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

        this.indexCount = indices.remaining();

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        uploaded = true;
        logger.debug("Mesh uploaded successfully (buffer variant), {} triangles",
                getTriangleCount());
    }

    /**
     * Crée les buffers OpenGL (VAO, VBO, EBO).
     */
    private void createBuffers() {
        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        ebo = glGenBuffers();

        logger.trace("Created OpenGL buffers: VAO={}, VBO={}, EBO={}", vao, vbo, ebo);
    }

    // ==================== Rendu ====================

    /**
     * Rend le mesh.
     * <p>
     * À appeler dans la boucle de rendu, après avoir bindé le shader
     * et configuré les uniforms.
     * </p>
     *
     * @throws IllegalStateException si le mesh n'a pas été uploadé
     */
    public void render() {
        if (!uploaded) {
            throw new IllegalStateException("Cannot render mesh: no data has been uploaded");
        }

        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }

    // ==================== Nettoyage ====================

    /**
     * Libère toutes les ressources OpenGL.
     * <p>
     * À appeler avant la fermeture de l'application ou quand le mesh
     * n'est plus utilisé.
     * </p>
     */
    public void cleanup() {
        if (uploaded) {
            logger.debug("Cleaning up mesh: VAO={}, VBO={}, EBO={}", vao, vbo, ebo);

            glDeleteBuffers(vbo);
            glDeleteBuffers(ebo);
            glDeleteVertexArrays(vao);

            vao = 0;
            vbo = 0;
            ebo = 0;
            indexCount = 0;
            uploaded = false;
        }
    }

    // ==================== Getters ====================

    /**
     * Retourne le VAO OpenGL.
     *
     * @return l'ID du VAO
     */
    public int getVao() {
        return vao;
    }

    /**
     * Retourne le nombre d'indices (triangles × 3).
     *
     * @return le nombre d'indices
     */
    public int getIndexCount() {
        return indexCount;
    }

    /**
     * Vérifie si le mesh a été uploadé sur le GPU.
     *
     * @return true si uploadé, false sinon
     */
    public boolean isUploaded() {
        return uploaded;
    }

    /**
     * Retourne le nombre de triangles dans ce mesh.
     *
     * @return le nombre de triangles
     */
    public int getTriangleCount() {
        return indexCount / 3;
    }

    /**
     * Retourne le nombre de vertices dans ce mesh.
     *
     * @return le nombre de vertices
     */
    public int getVertexCount() {
        return indexCount; // Approximation, en réalité peut être moins
    }

    /**
     * Retourne une représentation textuelle du mesh.
     *
     * @return description du mesh
     */
    @Override
    public String toString() {
        return String.format("Mesh[vao=%d, triangles=%d, uploaded=%b]",
                vao, getTriangleCount(), uploaded);
    }
}
