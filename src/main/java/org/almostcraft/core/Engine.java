package org.almostcraft.core;

import org.almostcraft.input.InputManager;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;

/**
 * Moteur principal du jeu AlmostCraft.
 * <p>
 * Cette classe gère le cycle de vie complet de l'application :
 * <ul>
 *   <li>Initialisation de GLFW et OpenGL</li>
 *   <li>Création de la fenêtre et des systèmes d'entrée</li>
 *   <li>Boucle de jeu (game loop)</li>
 *   <li>Nettoyage des ressources</li>
 * </ul>
 * </p>
 * <p>
 * Le moteur suit une architecture classique avec séparation claire entre
 * l'initialisation, la boucle de jeu (update/render) et le nettoyage.
 * </p>
 *
 * @author Lucas Préaux
 * @version 1.0
 * @see Window
 * @see InputManager
 */
public class Engine {

    // ==================== Logger ====================

    /**
     * Logger SLF4J pour cette classe.
     */
    private static final Logger logger = LoggerFactory.getLogger(Engine.class);

    // ==================== Constantes ====================

    /**
     * Largeur par défaut de la fenêtre en pixels.
     */
    private static final int DEFAULT_WINDOW_WIDTH = 300;

    /**
     * Hauteur par défaut de la fenêtre en pixels.
     */
    private static final int DEFAULT_WINDOW_HEIGHT = 300;

    /**
     * Titre de la fenêtre.
     */
    private static final String WINDOW_TITLE = "AlmostCraft";

    /**
     * Chemin vers le fichier de configuration.
     */
    private static final String CONFIG_FILE = "/application.properties";

    // ==================== Attributs ====================

    /**
     * Fenêtre principale de l'application.
     */
    private Window window;

    /**
     * Gestionnaire des entrées utilisateur (clavier et souris).
     */
    private InputManager inputManager;

    // ==================== Point d'entrée ====================

    /**
     * Démarre le moteur de jeu.
     * <p>
     * Cette méthode orchestre le cycle de vie complet de l'application :
     * <ol>
     *   <li>Charge la configuration</li>
     *   <li>Initialise les systèmes</li>
     *   <li>Lance la boucle de jeu</li>
     *   <li>Nettoie les ressources</li>
     * </ol>
     * </p>
     *
     * @throws IOException si le chargement de la configuration échoue
     */
    public void run() throws IOException {
        logStartup();
        init();
        logger.info("AlmostCraft has started successfully");

        loop();

        logger.info("AlmostCraft shutting down...");
        cleanup();
        logger.info("AlmostCraft closed");
    }

    // ==================== Initialisation ====================

    /**
     * Affiche les informations de démarrage dans la console.
     * <p>
     * Charge et affiche la version de l'application depuis le fichier de configuration.
     * </p>
     *
     * @throws IOException si le fichier de configuration ne peut pas être lu
     */
    private void logStartup() throws IOException {
        Properties props = loadProperties();
        String version = props.getProperty("app.version", "unknown");
        logger.info("Starting AlmostCraft v{}", version);
    }

    /**
     * Charge les propriétés de configuration depuis le classpath.
     *
     * @return les propriétés chargées
     * @throws IOException si le fichier ne peut pas être lu
     * @throws IllegalStateException si le fichier de configuration n'existe pas
     */
    private Properties loadProperties() throws IOException {
        Properties props = new Properties();

        try (InputStream input = getClass().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                logger.error("Configuration file not found: {}", CONFIG_FILE);
                throw new IllegalStateException("Configuration file not found: " + CONFIG_FILE);
            }
            props.load(input);
            logger.debug("Configuration loaded from {}", CONFIG_FILE);
        }

        return props;
    }

    /**
     * Initialise tous les systèmes du moteur.
     * <p>
     * Initialise dans l'ordre :
     * <ol>
     *   <li>Le système d'erreurs GLFW</li>
     *   <li>La bibliothèque GLFW</li>
     *   <li>La fenêtre principale</li>
     *   <li>Le gestionnaire d'entrées</li>
     * </ol>
     * </p>
     *
     * @throws IllegalStateException si l'initialisation de GLFW échoue
     */
    private void init() {
        logger.info("Initializing engine systems...");
        initGLFW();
        initWindow();
        initInput();
        logger.info("All engine systems initialized");
    }

    /**
     * Initialise GLFW et configure le callback d'erreur.
     *
     * @throws IllegalStateException si GLFW ne peut pas être initialisé
     */
    private void initGLFW() {
        logger.debug("Configuring GLFW error callback");
        GLFWErrorCallback.createPrint(System.err).set();

        logger.info("Initializing GLFW");
        if (!glfwInit()) {
            logger.error("Failed to initialize GLFW");
            throw new IllegalStateException("Unable to initialize GLFW");
        }
        logger.debug("GLFW initialized successfully");
    }

    /**
     * Crée et configure la fenêtre principale.
     * <p>
     * Configure la fenêtre avec :
     * <ul>
     *   <li>Centrage sur l'écran</li>
     *   <li>Contexte OpenGL actif</li>
     *   <li>VSync activé</li>
     * </ul>
     * </p>
     */
    private void initWindow() {
        logger.info("Creating window ({}x{})", DEFAULT_WINDOW_WIDTH, DEFAULT_WINDOW_HEIGHT);
        window = new Window(DEFAULT_WINDOW_WIDTH, DEFAULT_WINDOW_HEIGHT, WINDOW_TITLE);
        window.centerOnScreen();
        window.makeContextCurrent();
        window.enableVsync();
        window.show();
        logger.debug("Window created with handle: {}", window.getHandle());
    }

    /**
     * Initialise le gestionnaire d'entrées.
     */
    private void initInput() {
        logger.debug("Initializing input manager");
        inputManager = new InputManager(window.getHandle());
        logger.debug("Input manager initialized");
    }

    // ==================== Boucle de jeu ====================

    /**
     * Boucle principale du jeu.
     * <p>
     * Crée le contexte OpenGL et exécute la boucle de jeu jusqu'à ce que
     * la fenêtre doive se fermer. Chaque itération :
     * <ol>
     *   <li>Met à jour les entrées</li>
     *   <li>Traite les événements GLFW</li>
     *   <li>Gère les inputs (temporaire : ESC pour quitter, SPACE pour changer la couleur)</li>
     *   <li>Effectue le rendu</li>
     *   <li>Échange les buffers</li>
     * </ol>
     * </p>
     */
    private void loop() {
        logger.info("Creating OpenGL capabilities");
        GL.createCapabilities();

        // Couleur de fond par défaut (rouge)
        glClearColor(1.0f, 0.0f, 0.0f, 0.0f);

        logger.info("Entering game loop");

        // Boucle principale
        while (!window.shouldClose()) {
            // Mise à jour des entrées
            inputManager.update();

            // Traitement des événements
            glfwPollEvents();

            // Gestion des inputs (temporaire - à déplacer dans une classe dédiée)
            handleInput();

            // Rendu
            render();

            // Affichage
            window.swapBuffers();
        }

        logger.info("Game loop ended");
    }

    /**
     * Gère les entrées utilisateur.
     * <p>
     * Temporaire : cette logique sera déplacée dans une classe de gestion d'état.
     * Actuellement :
     * <ul>
     *   <li>ESC : ferme la fenêtre</li>
     *   <li>SPACE : change la couleur de fond en vert</li>
     * </ul>
     * </p>
     */
    private void handleInput() {
        // ESC pour quitter
        if (inputManager.isKeyDown(GLFW_KEY_ESCAPE)) {
            logger.debug("ESC key pressed, closing window");
            glfwSetWindowShouldClose(window.getHandle(), true);
        }

        // SPACE pour changer la couleur (test temporaire)
        if (inputManager.isKeyDown(GLFW_KEY_SPACE)) {
            glClearColor(0.0f, 1.0f, 0.0f, 0.0f);
        } else {
            glClearColor(1.0f, 0.0f, 0.0f, 0.0f);
        }
    }

    /**
     * Effectue le rendu de la frame actuelle.
     * <p>
     * Temporaire : nettoie simplement le framebuffer avec la couleur définie.
     * À terme, cette méthode déléguera le rendu à un système de rendu dédié.
     * </p>
     */
    private void render() {
        // Nettoyage du framebuffer
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // TODO: Rendu de la scène
    }

    /**
     * Met à jour la logique du jeu.
     * <p>
     * Non utilisée pour le moment. À terme, cette méthode gérera :
     * <ul>
     *   <li>La physique</li>
     *   <li>L'IA</li>
     *   <li>Les systèmes de jeu</li>
     * </ul>
     * </p>
     *
     * @param deltaTime le temps écoulé depuis la dernière frame en secondes
     */
    @SuppressWarnings("unused")
    private void update(float deltaTime) {
        // TODO: Implémenter la logique de mise à jour
    }

    // ==================== Nettoyage ====================

    /**
     * Nettoie toutes les ressources avant la fermeture de l'application.
     * <p>
     * Libère dans l'ordre :
     * <ol>
     *   <li>La fenêtre</li>
     *   <li>GLFW et son callback d'erreur</li>
     * </ol>
     * </p>
     */
    private void cleanup() {
        logger.debug("Destroying window");
        window.destroy();

        logger.debug("Terminating GLFW");
        glfwTerminate();

        logger.debug("Freeing GLFW error callback");
        GLFWErrorCallback errorCallback = glfwSetErrorCallback(null);
        if (errorCallback != null) {
            errorCallback.free();
        }

        logger.debug("Cleanup complete");
    }
}
