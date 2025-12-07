package org.almostcraft.input;

import java.util.Arrays;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LAST;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

/**
 * Gère l'état de la souris et les événements d'entrée souris.
 * <p>
 * Cette classe suit la position du curseur, les mouvements (delta), l'état des boutons
 * de la souris et le défilement de la molette. Elle fournit également un système de
 * sensibilité pour ajuster la vitesse du mouvement de la souris.
 * </p>
 * <p>
 * Utilisation typique :
 * <pre>{@code
 * MouseInput mouse = new MouseInput();
 *
 * // Dans la boucle de jeu
 * double dx = mouse.getDeltaX(); // Mouvement horizontal
 * double dy = mouse.getDeltaY(); // Mouvement vertical
 *
 * if (mouse.isButtonPressed(GLFW_MOUSE_BUTTON_LEFT)) {
 *     // Bouton gauche maintenu
 * }
 *
 * if (mouse.isButtonJustPressed(GLFW_MOUSE_BUTTON_RIGHT)) {
 *     // Bouton droit vient d'être cliqué
 * }
 *
 * mouse.update(); // À appeler à chaque frame
 * }</pre>
 * </p>
 *
 * @author Lucas Préaux
 * @version 1.0
 * @see InputManager
 */
public class MouseInput {

    // ==================== Constantes ====================

    /**
     * Nombre total de boutons de souris gérés par GLFW.
     */
    private static final int BUTTON_COUNT = GLFW_MOUSE_BUTTON_LAST + 1;

    // ==================== Attributs de position ====================

    /**
     * Position horizontale actuelle du curseur (coordonnée X).
     */
    private double mouseX;

    /**
     * Position verticale actuelle du curseur (coordonnée Y).
     */
    private double mouseY;

    /**
     * Déplacement horizontal depuis la dernière frame (delta X).
     */
    private double deltaX;

    /**
     * Déplacement vertical depuis la dernière frame (delta Y).
     */
    private double deltaY;

    /**
     * Facteur de sensibilité appliqué aux mouvements de la souris.
     * Valeur par défaut : 1.0
     */
    private float sensitivity = 1.0f;

    /**
     * Indique si c'est le premier mouvement de souris détecté.
     * Utilisé pour éviter un saut initial de la caméra lors de la capture du curseur.
     */
    private boolean firstMove = true;

    // ==================== Attributs de boutons ====================

    /**
     * Tableau d'état des boutons de souris (true = pressé, false = relâché).
     * L'index correspond au code du bouton GLFW.
     */
    private final boolean[] buttons = new boolean[BUTTON_COUNT];

    /**
     * Tableau d'état des boutons venant d'être pressés (événement unique par frame).
     * Réinitialisé à chaque frame.
     */
    private final boolean[] buttonsJustPressed = new boolean[BUTTON_COUNT];

    // ==================== Attributs de défilement ====================

    /**
     * Défilement horizontal de la molette depuis la dernière frame.
     */
    private double scrollX;

    /**
     * Défilement vertical de la molette depuis la dernière frame.
     */
    private double scrollY;

    // ==================== Méthodes de gestion des événements ====================

    /**
     * Traite un événement de déplacement du curseur provenant de GLFW.
     * <p>
     * Cette méthode calcule le delta de mouvement en comparant la nouvelle position
     * avec la position précédente. Lors du premier mouvement, elle initialise
     * simplement la position sans calculer de delta.
     * </p>
     *
     * @param x la nouvelle position horizontale du curseur
     * @param y la nouvelle position verticale du curseur
     */
    void onCursorPosEvent(double x, double y) {
        if (firstMove) {
            mouseX = x;
            mouseY = y;
            firstMove = false;
            return;
        }

        deltaX = x - mouseX;
        deltaY = y - mouseY;

        mouseX = x;
        mouseY = y;
    }

    /**
     * Traite un événement de bouton de souris provenant de GLFW.
     * <p>
     * Cette méthode met à jour l'état du bouton spécifié selon l'action reçue.
     * </p>
     *
     * @param button le code du bouton (constante GLFW_MOUSE_BUTTON_*)
     * @param action l'action effectuée (GLFW_PRESS, GLFW_RELEASE, etc.)
     */
    void onMouseButtonEvent(int button, int action) {
        if (!isValidMouseButton(button)) {
            return;
        }

        if (action == GLFW_PRESS) {
            buttons[button] = true;
            buttonsJustPressed[button] = true;
        } else if (action == GLFW_RELEASE) {
            buttons[button] = false;
        }
    }

    /**
     * Traite un événement de défilement de la molette provenant de GLFW.
     *
     * @param x le défilement horizontal
     * @param y le défilement vertical
     */
    void onScrollEvent(double x, double y) {
        scrollX = x;
        scrollY = y;
    }

    // ==================== Méthodes de cycle de vie ====================

    /**
     * Met à jour l'état de la souris pour la prochaine frame.
     * <p>
     * Cette méthode doit être appelée une fois par frame, typiquement au début
     * de la boucle de jeu. Elle réinitialise les deltas de mouvement, les événements
     * "just pressed" et les valeurs de défilement.
     * </p>
     */
    void update() {
        deltaX = 0;
        deltaY = 0;

        Arrays.fill(buttonsJustPressed, false);

        scrollX = 0;
        scrollY = 0;
    }

    // ==================== Getters - Position et mouvement ====================

    /**
     * Retourne la position horizontale actuelle du curseur.
     *
     * @return la coordonnée X du curseur
     */
    public double getMouseX() {
        return mouseX;
    }

    /**
     * Retourne la position verticale actuelle du curseur.
     *
     * @return la coordonnée Y du curseur
     */
    public double getMouseY() {
        return mouseY;
    }

    /**
     * Retourne le déplacement horizontal depuis la dernière frame, ajusté par la sensibilité.
     *
     * @return le delta X multiplié par la sensibilité
     */
    public double getDeltaX() {
        return deltaX * sensitivity;
    }

    /**
     * Retourne le déplacement vertical depuis la dernière frame, ajusté par la sensibilité.
     *
     * @return le delta Y multiplié par la sensibilité
     */
    public double getDeltaY() {
        return deltaY * sensitivity;
    }

    // ==================== Méthodes publiques - Boutons ====================

    /**
     * Vérifie si un bouton de souris est actuellement pressé.
     * <p>
     * Cette méthode retourne {@code true} tant que le bouton reste enfoncé.
     * </p>
     *
     * @param button le code du bouton à vérifier (constante GLFW_MOUSE_BUTTON_*)
     * @return {@code true} si le bouton est pressé, {@code false} sinon
     */
    public boolean isButtonPressed(int button) {
        if (!isValidMouseButton(button)) {
            return false;
        }
        return buttons[button];
    }

    /**
     * Vérifie si un bouton de souris vient d'être pressé (événement unique).
     * <p>
     * Cette méthode retourne {@code true} uniquement lors de la première frame
     * où le bouton est pressé.
     * </p>
     *
     * @param button le code du bouton à vérifier (constante GLFW_MOUSE_BUTTON_*)
     * @return {@code true} si le bouton vient d'être pressé cette frame, {@code false} sinon
     */
    public boolean isButtonJustPressed(int button) {
        if (!isValidMouseButton(button)) {
            return false;
        }
        return buttonsJustPressed[button];
    }

    // ==================== Getters - Défilement ====================

    /**
     * Retourne le défilement horizontal de la molette depuis la dernière frame.
     *
     * @return la valeur de défilement horizontal
     */
    public double getScrollX() {
        return scrollX;
    }

    /**
     * Retourne le défilement vertical de la molette depuis la dernière frame.
     *
     * @return la valeur de défilement vertical
     */
    public double getScrollY() {
        return scrollY;
    }

    // ==================== Setters - Configuration ====================

    /**
     * Définit le facteur de sensibilité de la souris.
     * <p>
     * Une valeur plus élevée augmente la vitesse de mouvement, une valeur plus faible
     * la diminue. La sensibilité est appliquée aux deltas X et Y.
     * </p>
     *
     * @param sensitivity le nouveau facteur de sensibilité (généralement entre 0.1 et 3.0)
     */
    public void setSensitivity(float sensitivity) {
        this.sensitivity = sensitivity;
    }

    /**
     * Réinitialise le delta de mouvement en marquant le prochain mouvement comme "premier".
     * <p>
     * Cette méthode est utile lors de la capture du curseur pour éviter un saut soudain
     * de la caméra causé par un grand delta entre la position précédente et la nouvelle
     * position au centre de l'écran.
     * </p>
     */
    public void resetDelta() {
        firstMove = true;
    }

    // ==================== Méthodes utilitaires privées ====================

    /**
     * Valide qu'un code de bouton est dans la plage autorisée.
     *
     * @param button le code de bouton à valider
     * @return {@code true} si le code est valide, {@code false} sinon
     */
    private boolean isValidMouseButton(int button) {
        return button >= 0 && button < BUTTON_COUNT;
    }
}
