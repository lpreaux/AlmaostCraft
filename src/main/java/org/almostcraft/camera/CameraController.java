package org.almostcraft.camera;

import org.almostcraft.input.InputManager;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Contrôleur pour une caméra FPS utilisant les inputs clavier et souris.
 * <p>
 * Gère les déplacements ZQSD et les rotations à la souris pour une caméra
 * en vue première personne. Inclut des paramètres configurables pour la
 * vitesse de déplacement et la sensibilité de la souris.
 * </p>
 * <p>
 * Utilisation typique :
 * <pre>{@code
 * Camera camera = new Camera();
 * CameraController controller = new CameraController(camera, inputManager);
 *
 * // Dans la boucle de jeu
 * controller.update(deltaTime);
 * }</pre>
 * </p>
 *
 * @author Lucas Préaux
 * @version 1.0
 * @see Camera
 * @see InputManager
 */
public class CameraController {

    // ==================== Constantes ====================

    /**
     * Vitesse de déplacement par défaut (unités par seconde).
     */
    private static final float DEFAULT_MOVE_SPEED = 5.0f;

    /**
     * Sensibilité de la souris par défaut (degrés par pixel).
     */
    private static final float DEFAULT_MOUSE_SENSITIVITY = 0.1f;

    // ==================== Attributs ====================

    /**
     * Caméra contrôlée par ce contrôleur.
     */
    private final Camera camera;

    /**
     * Gestionnaire d'inputs pour lire clavier et souris.
     */
    private final InputManager inputManager;

    /**
     * Vitesse de déplacement de la caméra (unités par seconde).
     */
    private float moveSpeed;

    /**
     * Sensibilité de la souris (degrés par pixel de mouvement).
     */
    private float mouseSensitivity;

    // ==================== Constructeur ====================

    /**
     * Crée un nouveau contrôleur de caméra avec les paramètres par défaut.
     *
     * @param camera       la caméra à contrôler
     * @param inputManager le gestionnaire d'inputs
     */
    public CameraController(Camera camera, InputManager inputManager) {
        this.camera = camera;
        this.inputManager = inputManager;
        this.moveSpeed = DEFAULT_MOVE_SPEED;
        this.mouseSensitivity = DEFAULT_MOUSE_SENSITIVITY;
    }

    // ==================== Update ====================

    /**
     * Met à jour la caméra en fonction des inputs utilisateur.
     * <p>
     * À appeler chaque frame avec le deltaTime pour des mouvements
     * fluides et indépendants du framerate.
     * </p>
     *
     * @param deltaTime le temps écoulé depuis la dernière frame (en secondes)
     */
    public void update(float deltaTime) {
        if (!inputManager.isCursorCaptured()) {
            return;
        }

        handleKeyboardInput(deltaTime);
        handleMouseInput(deltaTime);

        // Mettre à jour la matrice de vue après les transformations
        camera.updateViewMatrix();
    }

    // ==================== Gestion des inputs ====================

    /**
     * Traite les inputs clavier pour le déplacement.
     * <p>
     * Touches gérées :
     * - Z/W : Avancer
     * - S : Reculer
     * - Q/A : Aller à gauche
     * - D : Aller à droite
     * - SPACE : Monter
     * - LEFT_SHIFT : Descendre
     * </p>
     *
     * @param deltaTime le temps écoulé depuis la dernière frame
     */
    private void handleKeyboardInput(float deltaTime) {
        float forward = 0;
        float right = 0;
        float up = 0;


        if (inputManager.isKeyDown(GLFW_KEY_W)) {
            forward += 1;
        }
        if (inputManager.isKeyDown(GLFW_KEY_S)) {
            forward -= 1;
        }

        if (inputManager.isKeyDown(GLFW_KEY_A)) {
            right -= 1;
        }
        if (inputManager.isKeyDown(GLFW_KEY_D)) {
            right += 1;
        }

        if (inputManager.isKeyDown(GLFW_KEY_SPACE)) {
            up += 1;
        }
        if (inputManager.isKeyDown(GLFW_KEY_LEFT_SHIFT)) {
            up -= 1;
        }

        camera.move(
                forward * deltaTime * moveSpeed,
                right * deltaTime * moveSpeed,
                up * deltaTime * moveSpeed
        );
    }

    /**
     * Traite les inputs souris pour la rotation de la caméra.
     * <p>
     * Le delta X de la souris contrôle le yaw (rotation horizontale).
     * Le delta Y de la souris contrôle le pitch (rotation verticale).
     * </p>
     *
     * @param deltaTime le temps écoulé depuis la dernière frame
     */
    private void handleMouseInput(float deltaTime) {
        double deltaX = inputManager.getMouseDeltaX();
        double deltaY = inputManager.getMouseDeltaY();

        deltaX *= mouseSensitivity;
        deltaY *= mouseSensitivity * -1;

        camera.rotate((float) deltaX, (float) deltaY);
    }

    // ==================== Setters ====================

    /**
     * Définit la vitesse de déplacement de la caméra.
     *
     * @param moveSpeed la nouvelle vitesse (unités par seconde, > 0)
     */
    public void setMoveSpeed(float moveSpeed) {
        if (moveSpeed <= 0) {
            throw new IllegalArgumentException("Move speed must be positive");
        }
        this.moveSpeed = moveSpeed;
    }

    /**
     * Définit la sensibilité de la souris.
     *
     * @param mouseSensitivity la nouvelle sensibilité (degrés par pixel, > 0)
     */
    public void setMouseSensitivity(float mouseSensitivity) {
        if (mouseSensitivity <= 0) {
            throw new IllegalArgumentException("Mouse sensitivity must be positive");
        }
        this.mouseSensitivity = mouseSensitivity;
    }

    // ==================== Getters ====================

    /**
     * Retourne la vitesse de déplacement actuelle.
     *
     * @return la vitesse en unités par seconde
     */
    public float getMoveSpeed() {
        return moveSpeed;
    }

    /**
     * Retourne la sensibilité de la souris actuelle.
     *
     * @return la sensibilité en degrés par pixel
     */
    public float getMouseSensitivity() {
        return mouseSensitivity;
    }
}