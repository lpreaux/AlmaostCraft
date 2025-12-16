package org.almostcraft.graphics.culling;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Représente un frustum de vue (pyramide tronquée) pour le frustum culling.
 * <p>
 * Le frustum est défini par 6 plans (gauche, droite, haut, bas, proche, loin)
 * extraits de la matrice view-projection de la caméra. Il permet de tester
 * efficacement si des objets (chunks, bounding boxes) sont visibles à l'écran.
 * </p>
 * <p>
 * <strong>Utilisation :</strong>
 * <pre>
 * Frustum frustum = new Frustum();
 * frustum.update(camera.getViewProjectionMatrix());
 *
 * if (frustum.intersects(chunk.getBoundingBox())) {
 *     // Rendre le chunk
 * }
 * </pre>
 * </p>
 *
 * @author Lucas Préaux
 * @version 1.1
 * @see Plane
 * @see BoundingBox
 */
public class Frustum {

    // ==================== Constantes ====================

    /** Index du plan gauche dans le tableau de plans */
    private static final int LEFT = 0;

    /** Index du plan droit dans le tableau de plans */
    private static final int RIGHT = 1;

    /** Index du plan bas dans le tableau de plans */
    private static final int BOTTOM = 2;

    /** Index du plan haut dans le tableau de plans */
    private static final int TOP = 3;

    /** Index du plan proche dans le tableau de plans */
    private static final int NEAR = 4;

    /** Index du plan loin dans le tableau de plans */
    private static final int FAR = 5;

    // ==================== Attributs ====================

    /** Les 6 plans définissant le frustum */
    private final org.almostcraft.graphics.culling.Plane[] planes = new org.almostcraft.graphics.culling.Plane[6];

    /** Vecteurs temporaires pour éviter les allocations répétées */
    private final Vector4f[] rows = new Vector4f[4];

    // ==================== Constructeur ====================

    /**
     * Crée un nouveau frustum.
     * <p>
     * Le frustum doit être mis à jour via {@link #update(Matrix4f)}
     * avant toute utilisation.
     * </p>
     */
    public Frustum() {
        for (int i = 0; i < 4; i++) {
            rows[i] = new Vector4f();
        }
    }

    // ==================== Mise à jour ====================

    /**
     * Met à jour les plans du frustum depuis une matrice view-projection.
     * <p>
     * Cette méthode extrait les 6 plans du frustum en utilisant la méthode
     * de Gribb-Hartmann qui décompose la matrice VP en plans normalisés.
     * </p>
     * <p>
     * <strong>Important :</strong> La matrice doit être le produit
     * Projection × View, <strong>pas</strong> uniquement la matrice de projection.
     * </p>
     *
     * @param viewProjectionMatrix la matrice view-projection combinée (non null)
     * @throws NullPointerException si viewProjectionMatrix est null
     */
    public void update(Matrix4f viewProjectionMatrix) {
        // Extraire les 4 lignes de la matrice VP
        for (int i = 0; i < 4; i++) {
            viewProjectionMatrix.getRow(i, rows[i]);
        }

        // Extraire les plans selon l'algorithme de Gribb-Hartmann
        // Chaque plan est une combinaison linéaire des lignes de la matrice

        // Plan gauche : row[3] + row[0]
        planes[LEFT] = createPlane(
                rows[3].x + rows[0].x,
                rows[3].y + rows[0].y,
                rows[3].z + rows[0].z,
                rows[3].w + rows[0].w
        );

        // Plan droit : row[3] - row[0]
        planes[RIGHT] = createPlane(
                rows[3].x - rows[0].x,
                rows[3].y - rows[0].y,
                rows[3].z - rows[0].z,
                rows[3].w - rows[0].w
        );

        // Plan bas : row[3] + row[1]
        planes[BOTTOM] = createPlane(
                rows[3].x + rows[1].x,
                rows[3].y + rows[1].y,
                rows[3].z + rows[1].z,
                rows[3].w + rows[1].w
        );

        // Plan haut : row[3] - row[1]
        planes[TOP] = createPlane(
                rows[3].x - rows[1].x,
                rows[3].y - rows[1].y,
                rows[3].z - rows[1].z,
                rows[3].w - rows[1].w
        );

        // Plan proche : row[3] + row[2]
        planes[NEAR] = createPlane(
                rows[3].x + rows[2].x,
                rows[3].y + rows[2].y,
                rows[3].z + rows[2].z,
                rows[3].w + rows[2].w
        );

        // Plan loin : row[3] - row[2]
        planes[FAR] = createPlane(
                rows[3].x - rows[2].x,
                rows[3].y - rows[2].y,
                rows[3].z - rows[2].z,
                rows[3].w - rows[2].w
        );
    }

    /**
     * Alias pour {@link #update(Matrix4f)} pour rétrocompatibilité.
     *
     * @param viewProjectionMatrix la matrice view-projection combinée
     * @deprecated Utilisez {@link #update(Matrix4f)} à la place
     */
    @Deprecated
    public void updateFrustum(Matrix4f viewProjectionMatrix) {
        update(viewProjectionMatrix);
    }

    /**
     * Crée un plan normalisé à partir de ses coefficients.
     *
     * @param a coefficient x de la normale
     * @param b coefficient y de la normale
     * @param c coefficient z de la normale
     * @param d distance à l'origine
     * @return un nouveau plan normalisé
     */
    private org.almostcraft.graphics.culling.Plane createPlane(float a, float b, float c, float d) {
        return new org.almostcraft.graphics.culling.Plane(a, b, c, d);
    }

    // ==================== Tests de visibilité ====================

    /**
     * Teste si une bounding box est visible dans le frustum.
     * <p>
     * Cette méthode est utilisée par le CullingManager pour déterminer
     * si un chunk doit être rendu.
     * </p>
     *
     * @param boundingBox la bounding box à tester
     * @return true si la bounding box intersecte le frustum, false sinon
     */
    public boolean intersects(BoundingBox boundingBox) {
        return isBoxInFrustum(
                boundingBox.getMinX(), boundingBox.getMinY(), boundingBox.getMinZ(),
                boundingBox.getMaxX(), boundingBox.getMaxY(), boundingBox.getMaxZ()
        );
    }

    /**
     * Teste si une boîte englobante alignée sur les axes (AABB) est visible
     * dans le frustum.
     * <p>
     * Utilise l'algorithme du P-vertex pour une performance optimale :
     * pour chaque plan, on teste uniquement le coin de la boîte le plus
     * proche du plan. Si ce coin est derrière le plan, la boîte entière
     * est en dehors du frustum.
     * </p>
     *
     * @param minX coordonnée X minimale de la boîte
     * @param minY coordonnée Y minimale de la boîte
     * @param minZ coordonnée Z minimale de la boîte
     * @param maxX coordonnée X maximale de la boîte
     * @param maxY coordonnée Y maximale de la boîte
     * @param maxZ coordonnée Z maximale de la boîte
     * @return true si la boîte est au moins partiellement visible, false sinon
     */
    public boolean isBoxInFrustum(
            float minX, float minY, float minZ,
            float maxX, float maxY, float maxZ
    ) {
        for (org.almostcraft.graphics.culling.Plane plane : planes) {
            // Trouver le P-vertex (coin le plus proche du plan)
            // Si a > 0, le point max est plus proche, sinon c'est min
            float px = plane.a > 0 ? maxX : minX;
            float py = plane.b > 0 ? maxY : minY;
            float pz = plane.c > 0 ? maxZ : minZ;

            // Si le P-vertex est derrière le plan, la boîte est complètement dehors
            if (plane.distance(px, py, pz) < 0) {
                return false;
            }
        }

        // Aucun plan n'exclut la boîte : elle est au moins partiellement visible
        return true;
    }

    /**
     * Teste si un point est visible dans le frustum.
     *
     * @param x coordonnée X du point
     * @param y coordonnée Y du point
     * @param z coordonnée Z du point
     * @return true si le point est dans le frustum, false sinon
     */
    public boolean isPointInFrustum(float x, float y, float z) {
        for (org.almostcraft.graphics.culling.Plane plane : planes) {
            if (plane.distance(x, y, z) < 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Teste si un point est visible dans le frustum.
     *
     * @param point le point à tester
     * @return true si le point est dans le frustum, false sinon
     */
    public boolean isPointInFrustum(Vector3f point) {
        return isPointInFrustum(point.x, point.y, point.z);
    }

    /**
     * Teste si une sphère est visible dans le frustum.
     *
     * @param center centre de la sphère
     * @param radius rayon de la sphère
     * @return true si la sphère intersecte le frustum, false sinon
     */
    public boolean isSphereInFrustum(Vector3f center, float radius) {
        for (org.almostcraft.graphics.culling.Plane plane : planes) {
            if (plane.distance(center) < -radius) {
                return false;
            }
        }
        return true;
    }

    /**
     * Teste si un chunk de voxels est visible dans le frustum.
     * <p>
     * Version optimisée pour les chunks qui calcule automatiquement
     * la bounding box en coordonnées monde.
     * </p>
     *
     * @param chunkX position X du chunk (en coordonnées chunk, pas bloc)
     * @param chunkZ position Z du chunk (en coordonnées chunk, pas bloc)
     * @param chunkWidth largeur du chunk en blocs (généralement 16)
     * @param chunkHeight hauteur du chunk en blocs (généralement 256)
     * @param chunkDepth profondeur du chunk en blocs (généralement 16)
     * @param worldMinY coordonnée Y minimale du monde (généralement 0)
     * @return true si le chunk est visible, false sinon
     */
    public boolean isChunkVisible(
            int chunkX, int chunkZ,
            int chunkWidth, int chunkHeight, int chunkDepth,
            int worldMinY
    ) {
        // Convertir les coordonnées chunk en coordonnées monde
        float minX = chunkX * chunkWidth;
        float minY = worldMinY;
        float minZ = chunkZ * chunkDepth;

        float maxX = minX + chunkWidth;
        float maxY = worldMinY + chunkHeight;
        float maxZ = minZ + chunkDepth;

        return isBoxInFrustum(minX, minY, minZ, maxX, maxY, maxZ);
    }

    // ==================== Getters (debug) ====================

    /**
     * Récupère un plan du frustum pour debug/tests.
     *
     * @param index index du plan (0=LEFT, 1=RIGHT, 2=BOTTOM, 3=TOP, 4=NEAR, 5=FAR)
     * @return le plan à l'index spécifié
     * @throws ArrayIndexOutOfBoundsException si l'index est invalide
     */
    public org.almostcraft.graphics.culling.Plane getPlane(int index) {
        return planes[index];
    }

    /**
     * Récupère tous les plans du frustum.
     *
     * @return un tableau des 6 plans
     */
    public org.almostcraft.graphics.culling.Plane[] getPlanes() {
        return planes.clone();
    }

    // ==================== Debug ====================

    /**
     * Retourne une représentation textuelle du frustum pour debug.
     *
     * @return une chaîne décrivant le frustum
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Frustum[\n");
        String[] names = {"LEFT", "RIGHT", "BOTTOM", "TOP", "NEAR", "FAR"};
        for (int i = 0; i < 6; i++) {
            sb.append("  ").append(names[i]).append(": ").append(planes[i]).append("\n");
        }
        sb.append("]");
        return sb.toString();
    }
}
