package org.almostcraft.render.chunk.meshing;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder pour construire progressivement un mesh.
 * <p>
 * Gère l'accumulation des vertices et indices, avec support pour le format :
 * Position (3) + TextureIndex (1) + TexCoord (2) + TintColor (3) = 9 floats par vertex
 * </p>
 *
 * @author Lucas Préaux
 * @version 1.0
 */
public class MeshBuilder {

    // ==================== Constantes ====================

    private static final float BLOCK_SIZE = 1.0f;

    // ==================== Attributs ====================

    private final List<Float> vertices;
    private final List<Integer> indices;
    private int vertexCount;

    // ==================== Constructeur ====================

    public MeshBuilder() {
        this.vertices = new ArrayList<>();
        this.indices = new ArrayList<>();
        this.vertexCount = 0;
    }

    // ==================== Méthodes de construction ====================

    /**
     * Ajoute un quad (face rectangulaire) au mesh.
     *
     * @param x           position X du bloc
     * @param y           position Y du bloc
     * @param z           position Z du bloc
     * @param width       largeur du quad (en blocs)
     * @param height      hauteur du quad (en blocs)
     * @param direction   direction de la face
     * @param textureIndex index de la texture dans le texture array
     * @param tintColor   couleur de teinte à appliquer
     */
    public void addQuad(float x, float y, float z,
                        int width, int height,
                        FaceDirection direction,
                        float textureIndex,
                        Vector3f tintColor) {
        Vector3f[] quadVertices = getQuadVertices(x, y, z, width, height, direction);
        float[][] uvCoords = getUVCoords(width, height, direction);

        int startIndex = vertexCount;

        // Ajouter les 4 vertices du quad
        for (int i = 0; i < 4; i++) {
            addVertex(quadVertices[i], textureIndex, uvCoords[i], tintColor);
        }

        // Ajouter les indices pour former 2 triangles
        addQuadIndices(startIndex);
    }

    /**
     * Ajoute un vertex au mesh.
     */
    private void addVertex(Vector3f position, float textureIndex, float[] uv, Vector3f tintColor) {
        // Position (3 floats)
        vertices.add(position.x);
        vertices.add(position.y);
        vertices.add(position.z);

        // Texture index (1 float)
        vertices.add(textureIndex);

        // UV coordinates (2 floats)
        vertices.add(uv[0]);
        vertices.add(uv[1]);

        // Tint color (3 floats)
        vertices.add(tintColor.x);
        vertices.add(tintColor.y);
        vertices.add(tintColor.z);

        vertexCount++;
    }

    /**
     * Ajoute les indices pour former un quad (2 triangles).
     */
    private void addQuadIndices(int startIndex) {
        // Premier triangle (0, 1, 2)
        indices.add(startIndex);
        indices.add(startIndex + 1);
        indices.add(startIndex + 2);

        // Second triangle (2, 3, 0)
        indices.add(startIndex + 2);
        indices.add(startIndex + 3);
        indices.add(startIndex);
    }

    // ==================== Génération de géométrie ====================

    /**
     * Génère les 4 vertices d'un quad selon la direction.
     */
    private Vector3f[] getQuadVertices(float x, float y, float z,
                                       int width, int height,
                                       FaceDirection direction) {
        float w = width * BLOCK_SIZE;
        float h = height * BLOCK_SIZE;

        return switch (direction) {
            case NORTH -> new Vector3f[]{
                    new Vector3f(x, y, z),
                    new Vector3f(x, y + h, z),
                    new Vector3f(x + w, y + h, z),
                    new Vector3f(x + w, y, z)
            };

            case SOUTH -> new Vector3f[]{
                    new Vector3f(x + w, y, z + BLOCK_SIZE),
                    new Vector3f(x + w, y + h, z + BLOCK_SIZE),
                    new Vector3f(x, y + h, z + BLOCK_SIZE),
                    new Vector3f(x, y, z + BLOCK_SIZE)
            };

            case WEST -> new Vector3f[]{
                    new Vector3f(x, y, z + w),
                    new Vector3f(x, y + h, z + w),
                    new Vector3f(x, y + h, z),
                    new Vector3f(x, y, z)
            };

            case EAST -> new Vector3f[]{
                    new Vector3f(x + BLOCK_SIZE, y, z),
                    new Vector3f(x + BLOCK_SIZE, y + h, z),
                    new Vector3f(x + BLOCK_SIZE, y + h, z + w),
                    new Vector3f(x + BLOCK_SIZE, y, z + w)
            };

            case DOWN -> new Vector3f[]{
                    new Vector3f(x, y, z),
                    new Vector3f(x + w, y, z),
                    new Vector3f(x + w, y, z + h),
                    new Vector3f(x, y, z + h)
            };

            case UP -> new Vector3f[]{
                    new Vector3f(x, y + BLOCK_SIZE, z),
                    new Vector3f(x, y + BLOCK_SIZE, z + h),
                    new Vector3f(x + w, y + BLOCK_SIZE, z + h),
                    new Vector3f(x + w, y + BLOCK_SIZE, z)
            };
        };
    }

    /**
     * Génère les coordonnées UV pour un quad avec répétition de texture.
     */
    private float[][] getUVCoords(int width, int height, FaceDirection direction) {
        if (direction == FaceDirection.UP || direction == FaceDirection.DOWN) {
            return new float[][]{
                    {0, 0},
                    {0, height},
                    {width, height},
                    {width, 0}
            };
        } else {
            return new float[][]{
                    {0, height},
                    {0, 0},
                    {width, 0},
                    {width, height}
            };
        }
    }

    // ==================== Réinitialisation ====================

    /**
     * Réinitialise le builder pour construire un nouveau mesh.
     */
    public void clear() {
        vertices.clear();
        indices.clear();
        vertexCount = 0;
    }

    // ==================== Export ====================

    /**
     * Exporte les vertices sous forme de tableau.
     *
     * @return tableau de floats contenant tous les vertices
     */
    public float[] toVertexArray() {
        float[] array = new float[vertices.size()];
        for (int i = 0; i < vertices.size(); i++) {
            array[i] = vertices.get(i);
        }
        return array;
    }

    /**
     * Exporte les indices sous forme de tableau.
     *
     * @return tableau d'ints contenant tous les indices
     */
    public int[] toIndexArray() {
        int[] array = new int[indices.size()];
        for (int i = 0; i < indices.size(); i++) {
            array[i] = indices.get(i);
        }
        return array;
    }

    // ==================== Getters ====================

    /**
     * Retourne le nombre de vertices ajoutés.
     *
     * @return le nombre de vertices
     */
    public int getVertexCount() {
        return vertexCount;
    }

    /**
     * Retourne le nombre de triangles.
     *
     * @return le nombre de triangles
     */
    public int getTriangleCount() {
        return indices.size() / 3;
    }

    /**
     * Vérifie si le builder est vide.
     *
     * @return true si aucun vertex n'a été ajouté
     */
    public boolean isEmpty() {
        return vertexCount == 0;
    }
}
