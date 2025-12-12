package org.almostcraft.render.chunk.frustum;

/**
 * Représente un plan dans l'espace 3D sous forme d'équation implicite.
 * <p>
 * Un plan est défini par l'équation : <strong>ax + by + cz + d = 0</strong>
 * où (a, b, c) forme la normale du plan et d est la distance signée
 * à l'origine (après normalisation).
 * </p>
 * <p>
 * <strong>Propriétés importantes :</strong>
 * <ul>
 *   <li>La normale (a, b, c) est automatiquement normalisée à la construction</li>
 *   <li>distance(point) > 0 : le point est devant le plan (côté normal)</li>
 *   <li>distance(point) < 0 : le point est derrière le plan</li>
 *   <li>distance(point) = 0 : le point est sur le plan</li>
 * </ul>
 * </p>
 * <p>
 * Cette classe est immutable et thread-safe.
 * </p>
 *
 * @author Lucas Préaux
 * @version 1.0
 * @see Frustum
 */
public final class Plane {

    // ==================== Attributs ====================

    /** Coefficient x de la normale du plan (normalisée) */
    final float a;

    /** Coefficient y de la normale du plan (normalisée) */
    final float b;

    /** Coefficient z de la normale du plan (normalisée) */
    final float c;

    /** Distance signée à l'origine (après normalisation) */
    final float d;

    // ==================== Constructeur ====================

    /**
     * Crée un plan à partir de ses coefficients et le normalise.
     * <p>
     * La normale (a, b, c) est automatiquement normalisée pour que
     * les calculs de distance soient corrects. Si la normale fournie
     * est de longueur zéro, le comportement est indéfini.
     * </p>
     *
     * @param a coefficient x de la normale (avant normalisation)
     * @param b coefficient y de la normale (avant normalisation)
     * @param c coefficient z de la normale (avant normalisation)
     * @param d distance à l'origine (avant normalisation)
     */
    public Plane(float a, float b, float c, float d) {
        // Calculer la longueur de la normale
        float length = (float) Math.sqrt(a * a + b * b + c * c);

        // Normaliser tous les coefficients
        this.a = a / length;
        this.b = b / length;
        this.c = c / length;
        this.d = d / length;
    }

    // ==================== Méthodes ====================

    /**
     * Calcule la distance signée d'un point au plan.
     * <p>
     * <strong>Interprétation du résultat :</strong>
     * <ul>
     *   <li>distance > 0 : le point est devant le plan (côté normal)</li>
     *   <li>distance < 0 : le point est derrière le plan</li>
     *   <li>distance = 0 : le point est sur le plan</li>
     * </ul>
     * </p>
     * <p>
     * La valeur absolue de la distance représente la distance réelle
     * (perpendiculaire) entre le point et le plan.
     * </p>
     *
     * @param x coordonnée X du point
     * @param y coordonnée Y du point
     * @param z coordonnée Z du point
     * @return la distance signée du point au plan
     */
    public float distance(float x, float y, float z) {
        return a * x + b * y + c * z + d;
    }

    // ==================== Méthodes utilitaires ====================

    /**
     * Retourne une représentation textuelle du plan pour debug.
     *
     * @return une chaîne décrivant le plan
     */
    @Override
    public String toString() {
        return String.format("Plane[%.3fx + %.3fy + %.3fz + %.3f = 0]", a, b, c, d);
    }

    /**
     * Récupère le vecteur normal du plan.
     * <p>
     * Le vecteur retourné est un tableau de 3 floats [a, b, c].
     * Attention : ce tableau est une nouvelle allocation, pas une vue.
     * </p>
     *
     * @return un tableau contenant les composantes [a, b, c] de la normale
     */
    public float[] getNormal() {
        return new float[] { a, b, c };
    }

    /**
     * Récupère la distance signée du plan à l'origine.
     *
     * @return la distance d
     */
    public float getDistance() {
        return d;
    }
}
