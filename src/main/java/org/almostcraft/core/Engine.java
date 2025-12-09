package org.almostcraft.core;

import org.almostcraft.camera.Camera;
import org.almostcraft.camera.CameraController;
import org.almostcraft.input.InputManager;
import org.almostcraft.world.ChunkLoader;
import org.almostcraft.world.World;
import org.almostcraft.world.block.BlockRegistry;
import org.almostcraft.world.block.Blocks;
import org.almostcraft.world.generation.FlatTerrainGenerator;
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
        initInput();
        initBlockRegistry();
        initWorld();
        initCamera();
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
        TerrainGenerator generator = new FlatTerrainGenerator(blockRegistry);

        // Créer le monde""
        world = new World(generator, blockRegistry);

        logger.info("World initialized with FlatTerrainGenerator");
    }

    /**
     * Initialise la caméra et son contrôleur.
     * <p>
     * La caméra est placée à une position initiale (0, 5, 10) pour voir la scène,
     * et la matrice de projection est configurée avec le FOV et l'aspect ratio.
     * </p>
     */
    private void initCamera() {
        logger.debug("Initializing camera");
        camera = new Camera();

        // Configurer la matrice de projection
        float aspectRatio = (float) window.getWidth() / window.getHeight();
        camera.updateProjectionMatrix(FOV, aspectRatio, Z_NEAR, Z_FAR);

        // Créer le contrôleur de caméra
        cameraController = new CameraController(camera, inputManager);

        // Optionnel : ajuster la sensibilité si besoin
        // cameraController.setMouseSensitivity(0.15f);
        // cameraController.setMoveSpeed(10.0f);
        logger.debug("Camera initialized");
    }

    /**
     * Initialise le système de chargement des chunks.
     */
    private void initChunkLoader() {
        logger.info("Initializing chunk loader");

        // Créer le chunk loader avec render distance de 8
        chunkLoader = new ChunkLoader(world, 12);

        // Charger les chunks initiaux autour de la position de spawn
        Vector3f spawnPosition = camera.getPosition();
        chunkLoader.loadInitialChunks(spawnPosition);

        logger.info("Chunk loader initialized with {} chunks loaded",
                world.getLoadedChunkCount());
    }

    // ==================== Boucle de jeu ====================

    /**
     * Boucle principale du jeu.
     * <p>
     * Crée le contexte OpenGL et exécute la boucle de jeu jusqu'à ce que
     * la fenêtre doive se fermer. Chaque itération :
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
        logger.info("Creating OpenGL capabilities");
        GL.createCapabilities();

        // Couleur de fond par défaut (bleu ciel)
        glClearColor(0.53f, 0.81f, 0.92f, 1.0f);

        logger.info("Entering game loop");

        // TEST TEMPORAIRE : Générer quelques chunks au démarrage
        logger.info("Testing chunk generation...");
        world.getChunk(0, 0);
        world.getChunk(1, 0);
        world.getChunk(0, 1);
        logger.info("Generated {} chunks", world.getLoadedChunkCount());

        // TEST : Vérifier un bloc
        int blockAtSurface = world.getBlockAt(0, 64, 0);
        logger.info("Block at (0, 64, 0): ID={}", blockAtSurface);

        double lastTime = glfwGetTime();

        // Boucle principale
        while (!window.shouldClose()) {
            double currentTime = glfwGetTime();
            float deltaTime = (float) (currentTime - lastTime);
            lastTime = currentTime;

            // Mise à jour des entrées
            inputManager.update();

            // Traitement des événements
            glfwPollEvents();

            // Gestion des inputs (temporaire - à déplacer dans une classe dédiée)
            handleInput();

            // Mise à jour de la caméra
            cameraController.update(deltaTime);

            // Mise à jour du chargement des chunks
            chunkLoader.update(camera.getPosition());

            // Rendu
            render();

            // Affichage
            window.swapBuffers();

            if (glfwGetTime() - lastStatsLog > 5.0) {
                logPerformanceStats();
                lastStatsLog = (long) glfwGetTime();
            }
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

    private void logPerformanceStats() {
        logger.info("=== Performance Stats ===");
        logger.info("Loaded chunks: {}", world.getLoadedChunkCount());
        logger.info("Load queue: {}", chunkLoader.getLoadQueueSize());
        logger.info("Unload queue: {}", chunkLoader.getUnloadQueueSize());
        logger.info("Camera pos: {}", camera.getPosition());
    }
}

