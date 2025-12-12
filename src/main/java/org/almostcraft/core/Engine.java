package org.almostcraft.core;

import org.almostcraft.core.subsystems.CameraSubsystem;
import org.almostcraft.core.subsystems.RenderingSubsystem;
import org.almostcraft.core.subsystems.WorldSubsystem;
import org.almostcraft.input.InputManager;
import org.almostcraft.world.ChunkLoader;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * Moteur principal du jeu AlmostCraft.
 * <p>
 * Cette classe orchestre le cycle de vie de l'application en coordonnant
 * les différents subsystems (World, Camera, Rendering).
 * </p>
 * <p>
 * Architecture :
 * <ul>
 *   <li>Core : GLFW, Window, Input</li>
 *   <li>Subsystems : World, Camera, Rendering</li>
 *   <li>ChunkLoader : Pont entre World, Camera et Rendering</li>
 * </ul>
 * </p>
 *
 * @author Lucas Préaux
 * @version 2.0
 * @see WorldSubsystem
 * @see CameraSubsystem
 * @see RenderingSubsystem
 */
public class Engine {

    // ==================== Logger ====================

    private static final Logger logger = LoggerFactory.getLogger(Engine.class);

    // ==================== Constantes ====================

    private static final int DEFAULT_WINDOW_WIDTH = 1280;
    private static final int DEFAULT_WINDOW_HEIGHT = 720;
    private static final String WINDOW_TITLE = "AlmostCraft";
    private static final String CONFIG_FILE = "/application.properties";
    private static final int DEFAULT_RENDER_DISTANCE = 8;

    // ==================== Core Components ====================

    private Window window;
    private InputManager inputManager;

    // ==================== Subsystems ====================

    private WorldSubsystem worldSubsystem;
    private CameraSubsystem cameraSubsystem;
    private RenderingSubsystem renderingSubsystem;

    // ==================== Chunk Loading ====================

    private ChunkLoader chunkLoader;

    // ==================== Stats ====================

    private int frameCount = 0;
    private double lastFpsTime = 0;

    // ==================== Point d'entrée ====================

    /**
     * Démarre le moteur de jeu.
     * <p>
     * Cycle de vie complet :
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
     * Affiche les informations de démarrage.
     */
    private void logStartup() throws IOException {
        Properties props = loadProperties();
        String version = props.getProperty("app.version", "unknown");
        logger.info("Starting AlmostCraft v{}", version);
    }

    /**
     * Charge les propriétés de configuration.
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
     */
    private void init() {
        logger.info("Initializing engine systems...");

        // 1. Systèmes de base (GLFW, Window, OpenGL, Input)
        initCore();

        // 2. Créer les subsystems
        createSubsystems();

        // 3. Initialiser les subsystems
        initializeSubsystems();

        // 4. Créer le chunk loader (dépend de tous les subsystems)
        initChunkLoader();

        logger.info("All engine systems initialized");
    }

    /**
     * Initialise les systèmes de base (GLFW, Window, OpenGL, Input).
     */
    private void initCore() {
        logger.debug("Initializing core systems");

        // GLFW
        initGLFW();

        // Window
        initWindow();

        // OpenGL
        initOpenGL();

        // Input
        initInput();

        logger.debug("Core systems initialized");
    }

    /**
     * Initialise GLFW et configure le callback d'erreur.
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
     */
    private void initWindow() {
        logger.info("Creating window ({}x{})", DEFAULT_WINDOW_WIDTH, DEFAULT_WINDOW_HEIGHT);

        window = new Window(DEFAULT_WINDOW_WIDTH, DEFAULT_WINDOW_HEIGHT, WINDOW_TITLE);
        window.centerOnScreen();
        window.makeContextCurrent();
        window.enableVsync();

        // Callback de resize (sera utilisé après l'initialisation de CameraSubsystem)
        glfwSetFramebufferSizeCallback(window.getHandle(), (win, w, h) -> {
            if (cameraSubsystem != null) {
                cameraSubsystem.handleWindowResize(w, h);
            }
        });

        window.show();
        logger.debug("Window created with handle: {}", window.getHandle());
    }

    /**
     * Initialise OpenGL.
     */
    private void initOpenGL() {
        logger.info("Creating OpenGL capabilities");
        GL.createCapabilities();

        // Configuration OpenGL
        glClearColor(0.53f, 0.81f, 0.92f, 1.0f); // Bleu ciel
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glFrontFace(GL_CCW);

        logger.info("OpenGL initialized");
    }

    /**
     * Initialise le gestionnaire d'entrées.
     */
    private void initInput() {
        logger.debug("Initializing input manager");
        inputManager = new InputManager(window.getHandle());
        inputManager.captureCursor();
        logger.debug("Input manager initialized");
    }

    /**
     * Crée tous les subsystems.
     */
    private void createSubsystems() {
        logger.debug("Creating subsystems");

        // World subsystem (pas de dépendances externes)
        worldSubsystem = new WorldSubsystem();

        // Camera subsystem (dépend de InputManager et Window)
        cameraSubsystem = new CameraSubsystem(inputManager, window);

        // Rendering subsystem (dépend de World et BlockRegistry)
        renderingSubsystem = new RenderingSubsystem(
                worldSubsystem.getWorld(),
                worldSubsystem.getBlockRegistry()
        );

        logger.debug("Subsystems created");
    }

    /**
     * Initialise tous les subsystems dans l'ordre.
     */
    private void initializeSubsystems() {
        logger.info("Initializing subsystems");

        worldSubsystem.initialize();
        cameraSubsystem.initialize();
        renderingSubsystem.initialize();

        logger.info("Subsystems initialized");
    }

    /**
     * Initialise le système de chargement des chunks.
     * <p>
     * Le ChunkLoader fait le pont entre World, Camera et Rendering.
     * Il doit être créé après l'initialisation de tous les subsystems.
     * </p>
     */
    private void initChunkLoader() {
        logger.info("Initializing chunk loader");

        chunkLoader = new ChunkLoader(
                worldSubsystem.getWorld(),
                DEFAULT_RENDER_DISTANCE,
                renderingSubsystem.getChunkRenderer()
        );

        // Charger les chunks initiaux autour de la position de spawn
        chunkLoader.loadInitialChunks(cameraSubsystem.getCamera().getPosition());

        logger.info("Chunk loader initialized with {} chunks loaded",
                worldSubsystem.getWorld().getLoadedChunkCount());
    }

    // ==================== Boucle de jeu ====================

    /**
     * Boucle principale du jeu.
     */
    private void loop() {
        logger.info("Entering game loop");

        double lastTime = glfwGetTime();
        lastFpsTime = lastTime;

        while (!window.shouldClose()) {
            double currentTime = glfwGetTime();
            float deltaTime = (float) (currentTime - lastTime);
            lastTime = currentTime;

            // Mise à jour des entrées
            inputManager.update();

            // Traitement des événements
            glfwPollEvents();

            // Gestion des inputs
            handleInput();

            // Mise à jour de la logique
            update(deltaTime);

            // Rendu
            render();

            // Affichage
            window.swapBuffers();

            // Stats
            updateStats(currentTime);
        }

        logger.info("Game loop ended");
    }

    /**
     * Gère les entrées utilisateur globales.
     */
    private void handleInput() {
        // ESC pour quitter
        if (inputManager.isKeyDown(GLFW_KEY_ESCAPE)) {
            logger.debug("ESC key pressed, closing window");
            glfwSetWindowShouldClose(window.getHandle(), true);
        }

        // F1 pour toggle cursor
        if (inputManager.isKeyJustPressed(GLFW_KEY_F1)) {
            if (inputManager.isCursorCaptured()) {
                inputManager.releaseCursor();
                logger.debug("Cursor released");
            } else {
                inputManager.captureCursor();
                logger.debug("Cursor captured");
            }
        }
    }

    /**
     * Met à jour la logique du jeu.
     *
     * @param deltaTime le temps écoulé depuis la dernière frame en secondes
     */
    private void update(float deltaTime) {
        // Mise à jour des subsystems
        cameraSubsystem.update(deltaTime);
        worldSubsystem.update(deltaTime);
        renderingSubsystem.update(deltaTime);

        // Mise à jour du chargement des chunks
        chunkLoader.update(cameraSubsystem.getCamera().getPosition());
    }

    /**
     * Effectue le rendu de la frame actuelle.
     */
    private void render() {
        // Nettoyage du framebuffer
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // Rendre via le subsystem de rendering
        renderingSubsystem.render(cameraSubsystem.getCamera());
    }

    /**
     * Met à jour et affiche les statistiques (FPS, chunks, etc.).
     */
    private void updateStats(double currentTime) {
        frameCount++;

        if (currentTime - lastFpsTime >= 1.0) {
            logger.info("FPS: {}, Chunks: {}, Meshes: {}, Triangles: {}K, Pos: ({:.1f}, {:.1f}, {:.1f}), Direction: {}",
                    frameCount,
                    worldSubsystem.getWorld().getLoadedChunkCount(),
                    renderingSubsystem.getChunkRenderer().getCachedMeshCount(),
                    renderingSubsystem.getChunkRenderer().getTotalTriangleCount() / 1000,
                    cameraSubsystem.getCamera().getPosition().x,
                    cameraSubsystem.getCamera().getPosition().y,
                    cameraSubsystem.getCamera().getPosition().z,
                    cameraSubsystem.getCamera().getCardinalDirection()
            );

            frameCount = 0;
            lastFpsTime = currentTime;
        }
    }

    // ==================== Nettoyage ====================

    /**
     * Nettoie toutes les ressources avant la fermeture de l'application.
     */
    private void cleanup() {
        logger.debug("Cleaning up engine systems");

        // Nettoyer les subsystems dans l'ordre inverse de création
        if (renderingSubsystem != null) {
            renderingSubsystem.cleanup();
        }

        if (cameraSubsystem != null) {
            cameraSubsystem.cleanup();
        }

        if (worldSubsystem != null) {
            worldSubsystem.cleanup();
        }

        // Nettoyer les systèmes de base
        if (window != null) {
            window.destroy();
        }

        glfwTerminate();

        GLFWErrorCallback errorCallback = glfwSetErrorCallback(null);
        if (errorCallback != null) {
            errorCallback.free();
        }

        logger.debug("Cleanup complete");
    }
}
