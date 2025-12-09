package org.almostcraft.core;

import org.almostcraft.camera.Camera;
import org.almostcraft.camera.CameraController;
import org.almostcraft.input.InputManager;
import org.almostcraft.render.ChunkRenderer;
import org.almostcraft.render.Shader;
import org.almostcraft.world.ChunkLoader;
import org.almostcraft.world.World;
import org.almostcraft.world.block.BlockRegistry;
import org.almostcraft.world.block.Blocks;
import org.almostcraft.world.generation.FlatTerrainGenerator;
import org.almostcraft.world.generation.SimplexTerrainGenerator;
import org.almostcraft.world.generation.TerrainGenerator;
import org.joml.Vector3f;
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
    private static final int DEFAULT_WINDOW_WIDTH = 1280;

    /**
     * Hauteur par défaut de la fenêtre en pixels.
     */
    private static final int DEFAULT_WINDOW_HEIGHT = 720;

    /**
     * Titre de la fenêtre.
     */
    private static final String WINDOW_TITLE = "AlmostCraft";

    /**
     * Champ de vision (Field of View) de la caméra en degrés.
     */
    private static final float FOV = 70.0f;

    /**
     * Distance du plan de clipping proche.
     */
    private static final float Z_NEAR = 0.1f;

    /**
     * Distance du plan de clipping lointain.
     */
    private static final float Z_FAR = 1000.0f;

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

    private BlockRegistry blockRegistry;

    /**
     * Le monde du jeu.
     */
    private World world;

    /**
     * Gestionnaire de chargement des chunks.
     */
    private ChunkLoader chunkLoader;

    /**
     * Caméra FPS du joueur.
     */
    private Camera camera;

    /**
     * Contrôleur de la caméra (gère les inputs).
     */
    private CameraController cameraController;

    /**
     * Shader pour le rendu des chunks.
     */
    private Shader shader;

    /**
     * Renderer pour les chunks.
     */
    private ChunkRenderer chunkRenderer;

    private long lastStatsLog = 0;

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
        initOpenGL();
        initInput();
        initBlockRegistry();
        initWorld();
        initCamera();
        initShader();
        initChunkRenderer();
        initChunkLoader();
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
     * Initialise le registre de blocs et enregistre tous les blocs vanilla.
     */
    private void initBlockRegistry() {
        logger.info("Initializing block registry");

        blockRegistry = new BlockRegistry();

        // Enregistrer tous les blocs de base
        Blocks.register(blockRegistry);

        // Figer le registre (plus d'enregistrements autorisés)
        blockRegistry.freeze();

        logger.info("Block registry initialized with {} blocks", blockRegistry.size());
    }

    /**
     * Initialise le monde du jeu avec son générateur de terrain.
     */
    private void initWorld() {
        logger.info("Initializing world");

        // Créer le générateur de terrain
        // TerrainGenerator generator = new FlatTerrainGenerator(blockRegistry);
        TerrainGenerator generator = new SimplexTerrainGenerator(blockRegistry);

        // Créer le monde
        world = new World(generator, blockRegistry);

        logger.info("World initialized with FlatTerrainGenerator");
    }

    /**
     * Initialise la caméra et son contrôleur.
     * <p>
     * La caméra est placée à une position initiale (8, 70, 8) pour voir la scène,
     * et la matrice de projection est configurée avec le FOV et l'aspect ratio.
     * </p>
     */
    private void initCamera() {
        logger.debug("Initializing camera");

        // Position de spawn initiale
        camera = new Camera(new Vector3f(8, 70, 8), 0, 0);

        // Configurer la matrice de projection
        float aspectRatio = (float) window.getWidth() / window.getHeight();
        camera.updateProjectionMatrix(FOV, aspectRatio, Z_NEAR, Z_FAR);

        // Créer le contrôleur de caméra
        cameraController = new CameraController(camera, inputManager);
        cameraController.setMoveSpeed(20.0f);

        logger.debug("Camera initialized at position {}", camera.getPosition());
    }

    /**
     * Initialise le système de chargement des chunks.
     */
    private void initChunkLoader() {
        logger.info("Initializing chunk loader");

        // Créer le chunk loader avec render distance de 8
        chunkLoader = new ChunkLoader(world, 8, chunkRenderer);

        // Charger les chunks initiaux autour de la position de spawn
        Vector3f spawnPosition = camera.getPosition();
        chunkLoader.loadInitialChunks(spawnPosition);

        logger.info("Chunk loader initialized with {} chunks loaded",
                world.getLoadedChunkCount());
    }

    /**
     * Initialise le shader de rendu.
     */
    private void initShader() {
        logger.info("Initializing shader");
        shader = new Shader("shaders/cube.vert", "shaders/cube.frag");
        logger.info("Shader initialized");
    }

    /**
     * Initialise le renderer de chunks.
     */
    private void initChunkRenderer() {
        logger.info("Initializing chunk renderer");
        chunkRenderer = new ChunkRenderer(world, blockRegistry, shader);
        logger.info("Chunk renderer initialized");
    }

    // ==================== Boucle de jeu ====================

    /**
     * Boucle principale du jeu.
     * <p>
     * Exécute la boucle de jeu jusqu'à ce que la fenêtre doive se fermer.
     * Chaque itération :
     * <ol>
     *   <li>Calcule le deltaTime</li>
     *   <li>Met à jour les entrées</li>
     *   <li>Traite les événements GLFW</li>
     *   <li>Gère les inputs globaux (ESC pour quitter)</li>
     *   <li>Met à jour la caméra</li>
     *   <li>Effectue le rendu</li>
     *   <li>Échange les buffers</li>
     * </ol>
     * </p>
     */
    private void loop() {
        logger.info("Entering game loop");

        double lastTime = glfwGetTime();
        int frameCount = 0;
        double lastFpsTime = lastTime;

        // Boucle principale
        while (!window.shouldClose()) {
            double currentTime = glfwGetTime();
            float deltaTime = (float) (currentTime - lastTime);
            lastTime = currentTime;

            // FPS counter
            frameCount++;
            if (currentTime - lastFpsTime >= 1.0) {
                logger.info("FPS: {}, Chunks: {}, Meshes: {}, Triangles: {}K, Pos: ({:.1f}, {:.1f}, {:.1f})",
                        frameCount,
                        world.getLoadedChunkCount(),
                        chunkRenderer.getCachedMeshCount(),
                        chunkRenderer.getTotalTriangleCount() / 1000,
                        camera.getPosition().x,
                        camera.getPosition().y,
                        camera.getPosition().z
                );
                frameCount = 0;
                lastFpsTime = currentTime;
            }

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
        }

        logger.info("Game loop ended");
    }

    /**
     * Gère les entrées utilisateur.
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
        // Mise à jour de la caméra
        cameraController.update(deltaTime);
        camera.updateViewMatrix();

        // Mise à jour du chargement des chunks
        chunkLoader.update(camera.getPosition());

        // Mise à jour du renderer
        chunkRenderer.update();
    }

    /**
     * Effectue le rendu de la frame actuelle.
     */
    private void render() {
        // Nettoyage du framebuffer
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // Rendre tous les chunks
        chunkRenderer.render(camera);
    }

    // ==================== Nettoyage ====================

    /**
     * Nettoie toutes les ressources avant la fermeture de l'application.
     */
    private void cleanup() {
        logger.debug("Cleaning up chunk renderer");
        chunkRenderer.cleanup();

        logger.debug("Cleaning up shader");
        shader.cleanup();

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
