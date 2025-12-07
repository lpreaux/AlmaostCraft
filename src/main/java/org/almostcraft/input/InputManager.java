package org.almostcraft.input;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.lwjgl.glfw.GLFW.GLFW_CURSOR;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_DISABLED;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_HIDDEN;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_NORMAL;
import static org.lwjgl.glfw.GLFW.glfwGetInputMode;
import static org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback;
import static org.lwjgl.glfw.GLFW.glfwSetInputMode;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetMouseButtonCallback;
import static org.lwjgl.glfw.GLFW.glfwSetScrollCallback;

/**
 * Gestionnaire principal des entrées utilisateur (clavier et souris).
 * <p>
 * Cette classe sert de point d'entrée unifié pour tous les systèmes d'entrée du jeu.
 * Elle gère les callbacks GLFW, délègue le traitement aux composants spécialisés
 * ({@link KeyboardInput} et {@link MouseInput}), et expose une API simple pour
 * interroger l'état des entrées.
 * </p>
 * <p>
 * Utilisation typique :
 * <pre>{@code
 * // Initialisation
 * InputManager inputManager = new InputManager(windowHandle);
 *
 * // Dans la boucle de jeu
 * inputManager.update(); // Au début de chaque frame
 *
 * if (inputManager.isKeyDown(GLFW_KEY_W)) {
 *     moveForward();
 * }
 *
 * double mouseDeltaX = inputManager.getMouseDeltaX();
 * rotateCamera(mouseDeltaX);
 * }</pre>
 * </p>
 *
 * @author Lucas Préaux
 * @version 1.0
 * @see KeyboardInput
 * @see MouseInput
 */
public class InputManager {

    // ==================== Logger ====================

    /**
     * Logger SLF4J pour cette classe.
     */
    private static final Logger logger = LoggerFactory.getLogger(InputManager.class);

    // ==================== Attributs ====================

    /**
     * Gestionnaire des entrées clavier.
     */
    private final KeyboardInput keyboardInput;

    /**
     * Gestionnaire des entrées souris.
     */
    private final MouseInput mouseInput;

    /**
     * Handle de la fenêtre GLFW associée.
     */
    private final long windowHandle;

    // ==================== Constructeur ====================

    /**
     * Crée un nouveau gestionnaire d'entrées pour la fenêtre spécifiée.
     * <p>
     * Cette méthode initialise les composants clavier et souris, puis enregistre
     * automatiquement tous les callbacks GLFW nécessaires.
     * </p>
     *
     * @param windowHandle le handle de la fenêtre GLFW
     */
    public InputManager(long windowHandle) {
        logger.debug("Creating InputManager for window handle: {}", windowHandle);
        this.windowHandle = windowHandle;
        this.keyboardInput = new KeyboardInput();
        this.mouseInput = new MouseInput();

        registerCallbacks();
        logger.debug("InputManager created and callbacks registered");
    }

    // ==================== Initialisation ====================

    /**
     * Enregistre tous les callbacks GLFW pour les événements d'entrée.
     * <p>
     * Configure les callbacks pour :
     * <ul>
     *   <li>Les événements clavier (touches pressées/relâchées)</li>
     *   <li>Les événements de boutons de souris</li>
     *   <li>Les mouvements du curseur</li>
     *   <li>Le défilement de la molette</li>
     * </ul>
     * </p>
     */
    private void registerCallbacks() {
        logger.trace("Registering GLFW input callbacks");

        glfwSetKeyCallback(windowHandle,
                (window, key, scancode, action, mods) -> keyboardInput.onKeyEvent(key, action));

        glfwSetMouseButtonCallback(windowHandle,
                (window, button, action, mods) -> mouseInput.onMouseButtonEvent(button, action));

        glfwSetCursorPosCallback(windowHandle,
                (window, x, y) -> mouseInput.onCursorPosEvent(x, y));

        glfwSetScrollCallback(windowHandle,
                (window, x, y) -> mouseInput.onScrollEvent(x, y));
    }

    // ==================== Méthodes de cycle de vie ====================

    /**
     * Met à jour l'état de tous les systèmes d'entrée.
     * <p>
     * Cette méthode doit être appelée une fois par frame, typiquement au début
     * de la boucle de jeu, avant de traiter les entrées. Elle réinitialise les
     * événements "just pressed" et les deltas de mouvement.
     * </p>
     */
    public void update() {
        keyboardInput.update();
        mouseInput.update();
    }

    // ==================== API Clavier ====================

    /**
     * Vérifie si une touche est actuellement pressée.
     *
     * @param keyCode le code de la touche à vérifier (constante GLFW_KEY_*)
     * @return {@code true} si la touche est pressée, {@code false} sinon
     * @see KeyboardInput#isKeyDown(int)
     */
    public boolean isKeyDown(int keyCode) {
        return keyboardInput.isKeyDown(keyCode);
    }

    /**
     * Vérifie si une touche vient d'être pressée (événement unique).
     *
     * @param keyCode le code de la touche à vérifier (constante GLFW_KEY_*)
     * @return {@code true} si la touche vient d'être pressée cette frame, {@code false} sinon
     * @see KeyboardInput#isKeyJustPressed(int)
     */
    public boolean isKeyJustPressed(int keyCode) {
        return keyboardInput.isKeyJustPressed(keyCode);
    }

    // ==================== API Souris - Position et mouvement ====================

    /**
     * Retourne la position horizontale actuelle du curseur.
     *
     * @return la coordonnée X du curseur
     * @see MouseInput#getMouseX()
     */
    public double getMouseX() {
        return mouseInput.getMouseX();
    }

    /**
     * Retourne la position verticale actuelle du curseur.
     *
     * @return la coordonnée Y du curseur
     * @see MouseInput#getMouseY()
     */
    public double getMouseY() {
        return mouseInput.getMouseY();
    }

    /**
     * Retourne le déplacement horizontal de la souris depuis la dernière frame.
     *
     * @return le delta X ajusté par la sensibilité
     * @see MouseInput#getDeltaX()
     */
    public double getMouseDeltaX() {
        return mouseInput.getDeltaX();
    }

    /**
     * Retourne le déplacement vertical de la souris depuis la dernière frame.
     *
     * @return le delta Y ajusté par la sensibilité
     * @see MouseInput#getDeltaY()
     */
    public double getMouseDeltaY() {
        return mouseInput.getDeltaY();
    }

    // ==================== API Souris - Boutons ====================

    /**
     * Vérifie si un bouton de souris est actuellement pressé.
     *
     * @param button le code du bouton à vérifier (constante GLFW_MOUSE_BUTTON_*)
     * @return {@code true} si le bouton est pressé, {@code false} sinon
     * @see MouseInput#isButtonPressed(int)
     */
    public boolean isMouseButtonPressed(int button) {
        return mouseInput.isButtonPressed(button);
    }

    /**
     * Vérifie si un bouton de souris vient d'être pressé (événement unique).
     *
     * @param button le code du bouton à vérifier (constante GLFW_MOUSE_BUTTON_*)
     * @return {@code true} si le bouton vient d'être pressé cette frame, {@code false} sinon
     * @see MouseInput#isButtonJustPressed(int)
     */
    public boolean isMouseButtonJustPressed(int button) {
        return mouseInput.isButtonJustPressed(button);
    }

    // ==================== API Souris - Défilement ====================

    /**
     * Retourne le défilement horizontal de la molette depuis la dernière frame.
     *
     * @return la valeur de défilement horizontal
     * @see MouseInput#getScrollX()
     */
    public double getScrollX() {
        return mouseInput.getScrollX();
    }

    /**
     * Retourne le défilement vertical de la molette depuis la dernière frame.
     *
     * @return la valeur de défilement vertical
     * @see MouseInput#getScrollY()
     */
    public double getScrollY() {
        return mouseInput.getScrollY();
    }

    // ==================== Gestion du curseur ====================

    /**
     * Vérifie si le curseur est actuellement capturé (mode FPS).
     *
     * @return {@code true} si le curseur est en mode DISABLED (capturé), {@code false} sinon
     */
    public boolean isCursorCaptured() {
        return glfwGetInputMode(windowHandle, GLFW_CURSOR) == GLFW_CURSOR_DISABLED;
    }

    /**
     * Cache le curseur sans le capturer.
     * <p>
     * Le curseur reste visible dans sa position actuelle mais n'est pas affiché.
     * Utile pour des interfaces personnalisées.
     * </p>
     */
    public void hideCursor() {
        logger.debug("Hiding cursor");
        glfwSetInputMode(windowHandle, GLFW_CURSOR, GLFW_CURSOR_HIDDEN);
    }

    /**
     * Capture le curseur pour un contrôle de type FPS.
     * <p>
     * Le curseur devient invisible et ses mouvements ne sont plus limités par
     * les bords de la fenêtre. Le delta de mouvement est réinitialisé pour éviter
     * un saut initial de la caméra.
     * </p>
     */
    public void captureCursor() {
        logger.debug("Capturing cursor (FPS mode)");
        glfwSetInputMode(windowHandle, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        mouseInput.resetDelta();
    }

    /**
     * Libère le curseur et le rend visible.
     * <p>
     * Le curseur redevient normal et peut sortir de la fenêtre.
     * </p>
     */
    public void releaseCursor() {
        logger.debug("Releasing cursor");
        glfwSetInputMode(windowHandle, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
    }

    // ==================== Configuration ====================

    /**
     * Définit la sensibilité de la souris.
     * <p>
     * Affecte uniquement les deltas de mouvement retournés par {@link #getMouseDeltaX()}
     * et {@link #getMouseDeltaY()}.
     * </p>
     *
     * @param sensitivity le nouveau facteur de sensibilité (généralement entre 0.1 et 3.0)
     * @see MouseInput#setSensitivity(float)
     */
    public void setMouseSensitivity(float sensitivity) {
        logger.debug("Setting mouse sensitivity to {}", sensitivity);
        mouseInput.setSensitivity(sensitivity);
    }
}
