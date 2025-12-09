package org.almostcraft.camera;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Caméra FPS (First Person) avec contrôles yaw/pitch.
 * <p>
 * Gère la position, l'orientation et les matrices de transformation
 * nécessaires pour le rendu 3D.
 * </p>
 *
 * @author Lucas Préaux
 * @version 1.0
 */
public class Camera {

    // ==================== Constantes ====================

    /**
     * Angle de pitch minimum (regarder vers le bas).
     */
    private static final float MIN_PITCH = -89.0f;

    /**
     * Angle de pitch maximum (regarder vers le haut).
     */
    private static final float MAX_PITCH = 89.0f;

    // ==================== Attributs ====================

    /**
     * Position de la caméra dans le monde.
     */
    private final Vector3f position;

    /**
     * Rotation horizontale (en degrés).
     * 0° = regarde vers +Z, 90° = regarde vers +X.
     */
    private float yaw;

    /**
     * Rotation verticale (en degrés).
     * 0° = horizon, +90° = ciel, -90° = sol.
     */
    private float pitch;

    /**
     * Matrice de vue (transforme world space → view space).
     */
    private final Matrix4f viewMatrix;

    /**
     * Matrice de projection (transforme view space → clip space).
     */
    private final Matrix4f projectionMatrix;

    // ==================== Constructeur ====================

    /**
     * Crée une nouvelle caméra avec une position et orientation initiales.
     *
     * @param position la position initiale dans le monde
     * @param yaw      l'angle de rotation horizontal initial (en degrés)
     * @param pitch    l'angle de rotation vertical initial (en degrés)
     */
    public Camera(Vector3f position, float yaw, float pitch) {
        this.position = position;
        this.yaw = yaw;
        this.pitch = Math.max(MIN_PITCH, Math.min(MAX_PITCH, pitch));

        this.viewMatrix = new Matrix4f();
        this.projectionMatrix = new Matrix4f();
    }

    /**
     * Constructeur simplifié avec position par défaut à l'origine.
     */
    public Camera() {
        this(new Vector3f(0, 0, 0), 0, 0);
    }

    // ==================== Matrices ====================

    /**
     * Met à jour la matrice de vue en fonction de la position et orientation actuelles.
     * <p>
     * Doit être appelé après chaque modification de position/orientation.
     * </p>
     */
    public void updateViewMatrix() {
        viewMatrix.identity()
                .rotateX((float) Math.toRadians(pitch))
                .rotateY((float) Math.toRadians(yaw))
                .translate(-position.x, -position.y, -position.z);
    }

    /**
     * Met à jour la matrice de projection.
     * <p>
     * À appeler au démarrage et lors du redimensionnement de la fenêtre.
     * </p>
     *
     * @param fov         le champ de vision vertical (en degrés, typiquement 70-90)
     * @param aspectRatio le ratio largeur/hauteur de la fenêtre
     * @param near        la distance du plan de clipping proche (typiquement 0.1)
     * @param far         la distance du plan de clipping lointain (typiquement 1000)
     */
    public void updateProjectionMatrix(float fov, float aspectRatio, float near, float far) {
        projectionMatrix.identity()
                .perspective((float) Math.toRadians(fov), aspectRatio, near, far);
    }

    // ==================== Mouvement ====================

    /**
     * Déplace la caméra relativement à son orientation actuelle.
     * <p>
     * Les mouvements sont relatifs à la direction de la caméra :
     * - forward positif = avancer dans la direction du regard
     * - right positif = aller à droite
     * - up positif = monter verticalement
     * </p>
     *
     * @param forward distance de déplacement avant/arrière
     * @param right   distance de déplacement gauche/droite
     * @param up      distance de déplacement haut/bas
     */
    public void move(float forward, float right, float up) {
        float yawRad = (float) Math.toRadians(yaw);

        // Calculer les vecteurs de direction
        float forwardX = (float) Math.sin(yawRad);
        float forwardZ = -(float) Math.cos(yawRad);

        float rightX = (float) Math.sin(yawRad + Math.PI / 2);
        float rightZ = -(float) Math.cos(yawRad + Math.PI / 2);

        // Appliquer le mouvement
        position.x += (forwardX * forward) + (rightX * right);
        position.y += up;
        position.z += (forwardZ * forward) + (rightZ * right);
    }

    // ==================== Rotation ====================

    /**
     * Applique une rotation à la caméra.
     * <p>
     * Le pitch est automatiquement contraint entre -89° et +89° pour éviter
     * le gimbal lock et les comportements étranges.
     * </p>
     *
     * @param yawDelta   variation de l'angle horizontal (en degrés)
     * @param pitchDelta variation de l'angle vertical (en degrés)
     */
    public void rotate(float yawDelta, float pitchDelta) {
        this.yaw += yawDelta;
        this.pitch += pitchDelta;

        // Contraindre le pitch
        this.pitch = Math.max(MIN_PITCH, Math.min(MAX_PITCH, this.pitch));
    }

    // ==================== Getters ====================

    /**
     * Retourne la matrice de vue.
     *
     * @return la matrice de vue (world → view)
     */
    public Matrix4f getViewMatrix() {
        return viewMatrix;
    }

    /**
     * Retourne la matrice de projection.
     *
     * @return la matrice de projection (view → clip)
     */
    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }

    /**
     * Retourne la position actuelle de la caméra.
     *
     * @return un vecteur 3D représentant la position
     */
    public Vector3f getPosition() {
        return position;
    }

    /**
     * Retourne l'angle de rotation horizontal.
     *
     * @return le yaw en degrés
     */
    public float getYaw() {
        return yaw;
    }

    /**
     * Retourne l'angle de rotation vertical.
     *
     * @return le pitch en degrés
     */
    public float getPitch() {
        return pitch;
    }
}
