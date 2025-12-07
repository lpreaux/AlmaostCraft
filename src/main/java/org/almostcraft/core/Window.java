package org.almostcraft.core;

import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;
import static org.lwjgl.glfw.GLFW.glfwGetVideoMode;
import static org.lwjgl.glfw.GLFW.glfwGetWindowSize;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwSetFramebufferSizeCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowPos;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Gère la fenêtre principale de l'application via GLFW.
 * <p>
 * Cette classe encapsule la création, la configuration et la gestion d'une fenêtre GLFW.
 * Elle gère automatiquement le redimensionnement de la fenêtre et met à jour le viewport
 * OpenGL en conséquence.
 * </p>
 * <p>
 * Utilisation typique :
 * <pre>{@code
 * // Création et configuration
 * Window window = new Window(1280, 720, "My Game");
 * window.centerOnScreen();
 * window.makeContextCurrent();
 * window.enableVsync();
 * window.show();
 *
 * // Dans la boucle de jeu
 * while (!window.shouldClose()) {
 *     // Rendu...
 *     window.swapBuffers();
 * }
 *
 * // Nettoyage
 * window.destroy();
 * }</pre>
 * </p>
 *
 * @author Lucas Préaux
 * @version 1.0
 */
public class Window {

    // ==================== Logger ====================

    /**
     * Logger SLF4J pour cette classe.
     */
    private static final Logger logger = LoggerFactory.getLogger(Window.class);

    // ==================== Attributs ====================

    /**
     * Handle natif de la fenêtre GLFW.
     */
    private long handle;

    /**
     * Largeur actuelle de la fenêtre en pixels.
     */
    private int width;

    /**
     * Hauteur actuelle de la fenêtre en pixels.
     */
    private int height;

    /**
     * Titre de la fenêtre affiché dans la barre de titre.
     */
    private final String title;

    // ==================== Constructeur ====================

    /**
     * Crée une nouvelle fenêtre GLFW avec les dimensions et le titre spécifiés.
     * <p>
     * La fenêtre est créée mais reste invisible jusqu'à l'appel de {@link #show()}.
     * Elle est redimensionnable par défaut.
     * </p>
     *
     * @param width  la largeur initiale de la fenêtre en pixels (doit être > 0)
     * @param height la hauteur initiale de la fenêtre en pixels (doit être > 0)
     * @param title  le titre de la fenêtre (ne peut pas être null ou vide)
     * @throws IllegalArgumentException si les dimensions sont invalides ou si le titre est null/vide
     * @throws RuntimeException si la création de la fenêtre GLFW échoue
     */
    public Window(int width, int height, String title) {
        validateDimensions(width, height);
        validateTitle(title);

        this.width = width;
        this.height = height;
        this.title = title;

        create();
    }

    // ==================== Validation ====================

    /**
     * Valide les dimensions de la fenêtre.
     *
     * @param width  la largeur à valider
     * @param height la hauteur à valider
     * @throws IllegalArgumentException si l'une des dimensions est <= 0
     */
    private void validateDimensions(int width, int height) {
        if (width <= 0 || height <= 0) {
            logger.error("Invalid window dimensions: {}x{}. Both must be greater than 0", width, height);
            throw new IllegalArgumentException(
                    String.format("Invalid window dimensions: %dx%d. Both width and height must be greater than 0.",
                            width, height)
            );
        }
    }

    /**
     * Valide le titre de la fenêtre.
     *
     * @param title le titre à valider
     * @throws IllegalArgumentException si le titre est null ou vide
     */
    private void validateTitle(String title) {
        if (title == null || title.isEmpty()) {
            logger.error("Window title cannot be null or empty");
            throw new IllegalArgumentException("Window title cannot be null or empty");
        }
    }

    // ==================== Création et configuration ====================

    /**
     * Crée la fenêtre GLFW et configure les callbacks.
     * <p>
     * Configure la fenêtre pour être :
     * <ul>
     *   <li>Initialement invisible</li>
     *   <li>Redimensionnable</li>
     * </ul>
     * Enregistre également un callback pour mettre à jour le viewport OpenGL
     * lors du redimensionnement.
     * </p>
     *
     * @throws RuntimeException si la création de la fenêtre échoue
     */
    private void create() {
        logger.debug("Setting up window hints");
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        logger.debug("Creating GLFW window: {}x{} - '{}'", width, height, title);
        handle = glfwCreateWindow(width, height, title, NULL, NULL);
        if (handle == NULL) {
            logger.error("Failed to create GLFW window");
            throw new RuntimeException("Failed to create the GLFW window");
        }
        logger.debug("GLFW window created successfully (handle: {})", handle);

        // Callback de redimensionnement
        glfwSetFramebufferSizeCallback(handle, (window, w, h) -> {
            logger.debug("Window resized: {}x{} -> {}x{}", this.width, this.height, w, h);
            this.width = w;
            this.height = h;
            glViewport(0, 0, w, h);
        });
    }

    /**
     * Centre la fenêtre sur l'écran principal.
     * <p>
     * Calcule la position nécessaire pour centrer la fenêtre sur le moniteur principal
     * et déplace la fenêtre à cette position.
     * </p>
     */
    public void centerOnScreen() {
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);

            // Récupérer la taille de la fenêtre
            glfwGetWindowSize(handle, pWidth, pHeight);

            // Récupérer la résolution du moniteur principal
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            // Centrer la fenêtre
            int x = (vidmode.width() - pWidth.get(0)) / 2;
            int y = (vidmode.height() - pHeight.get(0)) / 2;

            logger.debug("Centering window at position ({}, {})", x, y);
            glfwSetWindowPos(handle, x, y);
        }
    }

    /**
     * Rend le contexte OpenGL de cette fenêtre actif sur le thread courant.
     * <p>
     * Cette méthode doit être appelée avant toute opération OpenGL.
     * </p>
     */
    public void makeContextCurrent() {
        logger.debug("Making OpenGL context current");
        glfwMakeContextCurrent(handle);
    }

    /**
     * Active la synchronisation verticale (VSync).
     * <p>
     * Limite le framerate au taux de rafraîchissement de l'écran (généralement 60 FPS).
     * Réduit le screen tearing mais peut introduire de la latence.
     * </p>
     */
    public void enableVsync() {
        logger.debug("Enabling VSync");
        glfwSwapInterval(1);
    }

    /**
     * Rend la fenêtre visible.
     * <p>
     * La fenêtre est créée invisible par défaut. Cette méthode doit être appelée
     * après toute la configuration initiale pour afficher la fenêtre à l'utilisateur.
     * </p>
     */
    public void show() {
        logger.debug("Showing window");
        glfwShowWindow(handle);
    }

    // ==================== Boucle de jeu ====================

    /**
     * Vérifie si la fenêtre doit se fermer.
     * <p>
     * Retourne {@code true} si l'utilisateur a demandé la fermeture de la fenêtre
     * (par exemple en cliquant sur le bouton de fermeture) ou si
     * {@code glfwSetWindowShouldClose} a été appelé avec {@code true}.
     * </p>
     *
     * @return {@code true} si la fenêtre doit se fermer, {@code false} sinon
     */
    public boolean shouldClose() {
        return glfwWindowShouldClose(handle);
    }

    /**
     * Échange les buffers de rendu (swap).
     * <p>
     * Affiche le contenu du buffer de rendu à l'écran. Cette méthode doit être
     * appelée à la fin de chaque frame, après tout le rendu OpenGL.
     * </p>
     */
    public void swapBuffers() {
        glfwSwapBuffers(handle);
    }

    // ==================== Nettoyage ====================

    /**
     * Détruit la fenêtre et libère les ressources associées.
     * <p>
     * Libère les callbacks GLFW et détruit la fenêtre. Cette méthode doit être
     * appelée avant la terminaison de GLFW.
     * </p>
     */
    public void destroy() {
        logger.debug("Destroying window (handle: {})", handle);
        glfwFreeCallbacks(handle);
        glfwDestroyWindow(handle);
    }

    // ==================== Getters ====================

    /**
     * Retourne le handle natif de la fenêtre GLFW.
     * <p>
     * Ce handle est nécessaire pour certaines opérations GLFW comme
     * l'enregistrement de callbacks ou la configuration d'inputs.
     * </p>
     *
     * @return le handle de la fenêtre GLFW
     */
    public long getHandle() {
        return handle;
    }

    /**
     * Retourne la largeur actuelle de la fenêtre.
     * <p>
     * Cette valeur est automatiquement mise à jour lors du redimensionnement.
     * </p>
     *
     * @return la largeur en pixels
     */
    public int getWidth() {
        return width;
    }

    /**
     * Retourne la hauteur actuelle de la fenêtre.
     * <p>
     * Cette valeur est automatiquement mise à jour lors du redimensionnement.
     * </p>
     *
     * @return la hauteur en pixels
     */
    public int getHeight() {
        return height;
    }
}
