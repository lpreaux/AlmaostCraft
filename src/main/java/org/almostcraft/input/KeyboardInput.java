package org.almostcraft.input;

import java.util.Arrays;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_LAST;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

/**
 * Gère l'état du clavier et les événements d'entrée clavier.
 * <p>
 * Cette classe suit l'état de toutes les touches du clavier et fournit des méthodes
 * pour vérifier si une touche est actuellement pressée ou vient d'être pressée
 * (événement unique au moment de l'appui).
 * </p>
 * <p>
 * Utilisation typique :
 * <pre>{@code
 * KeyboardInput keyboard = new KeyboardInput();
 *
 * // Dans la boucle de jeu
 * if (keyboard.isKeyDown(GLFW_KEY_W)) {
 *     // La touche W est maintenue enfoncée
 * }
 *
 * if (keyboard.isKeyJustPressed(GLFW_KEY_SPACE)) {
 *     // La touche Espace vient d'être pressée (événement unique)
 * }
 *
 * keyboard.update(); // À appeler à chaque frame
 * }</pre>
 * </p>
 *
 * @author Lucas Préaux
 * @version 1.0
 * @see InputManager
 */
public class KeyboardInput {

    // ==================== Constantes ====================

    /**
     * Nombre total de touches gérées par GLFW.
     */
    private static final int KEY_COUNT = GLFW_KEY_LAST + 1;

    // ==================== Attributs ====================

    /**
     * Tableau d'état des touches (true = pressée, false = relâchée).
     * L'index correspond au code de la touche GLFW.
     */
    private final boolean[] keys = new boolean[KEY_COUNT];

    /**
     * Tableau d'état des touches venant d'être pressées (événement unique par frame).
     * Réinitialisé à chaque frame.
     */
    private final boolean[] keysJustPressed = new boolean[KEY_COUNT];

    // ==================== Méthodes de gestion des événements ====================

    /**
     * Traite un événement clavier provenant de GLFW.
     * <p>
     * Cette méthode est appelée par le callback GLFW lorsqu'une touche change d'état.
     * Elle met à jour les tableaux d'état en conséquence.
     * </p>
     *
     * @param keyCode le code de la touche (constante GLFW_KEY_*)
     * @param action  l'action effectuée (GLFW_PRESS, GLFW_RELEASE, etc.)
     */
    void onKeyEvent(int keyCode, int action) {
        if (!isValidKeyCode(keyCode)) {
            return;
        }

        if (action == GLFW_PRESS) {
            keys[keyCode] = true;
            keysJustPressed[keyCode] = true;
        } else if (action == GLFW_RELEASE) {
            keys[keyCode] = false;
        }
    }

    // ==================== Méthodes de cycle de vie ====================

    /**
     * Met à jour l'état du clavier pour la prochaine frame.
     * <p>
     * Cette méthode doit être appelée une fois par frame, typiquement au début
     * de la boucle de jeu. Elle réinitialise les événements "just pressed" pour
     * qu'ils ne soient détectés qu'une seule fois.
     * </p>
     */
    void update() {
        Arrays.fill(keysJustPressed, false);
    }

    // ==================== Méthodes publiques d'interrogation ====================

    /**
     * Vérifie si une touche est actuellement pressée.
     * <p>
     * Cette méthode retourne {@code true} tant que la touche reste enfoncée,
     * et {@code false} dès qu'elle est relâchée.
     * </p>
     *
     * @param keyCode le code de la touche à vérifier (constante GLFW_KEY_*)
     * @return {@code true} si la touche est pressée, {@code false} sinon
     */
    public boolean isKeyDown(int keyCode) {
        if (!isValidKeyCode(keyCode)) {
            return false;
        }
        return keys[keyCode];
    }

    /**
     * Vérifie si une touche vient d'être pressée (événement unique).
     * <p>
     * Cette méthode retourne {@code true} uniquement lors de la première frame
     * où la touche est pressée. Les frames suivantes retourneront {@code false}
     * tant que la touche n'est pas relâchée puis pressée à nouveau.
     * </p>
     *
     * @param keyCode le code de la touche à vérifier (constante GLFW_KEY_*)
     * @return {@code true} si la touche vient d'être pressée cette frame, {@code false} sinon
     */
    public boolean isKeyJustPressed(int keyCode) {
        if (!isValidKeyCode(keyCode)) {
            return false;
        }
        return keysJustPressed[keyCode];
    }

    // ==================== Méthodes utilitaires privées ====================

    /**
     * Valide qu'un code de touche est dans la plage autorisée.
     *
     * @param keyCode le code de touche à valider
     * @return {@code true} si le code est valide, {@code false} sinon
     */
    private boolean isValidKeyCode(int keyCode) {
        return keyCode >= 0 && keyCode < KEY_COUNT;
    }
}

