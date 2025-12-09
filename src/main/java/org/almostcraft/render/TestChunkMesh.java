package org.almostcraft.render;

import org.almostcraft.camera.Camera;
import org.almostcraft.camera.CameraController;
import org.almostcraft.core.Window;
import org.almostcraft.input.InputManager;
import org.almostcraft.world.World;
import org.almostcraft.world.block.BlockRegistry;
import org.almostcraft.world.block.Blocks;
import org.almostcraft.world.chunk.Chunk;
import org.almostcraft.world.generation.FlatTerrainGenerator;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * Test du rendu d'un chunk complet avec ChunkMesh.
 * <p>
 * Affiche un seul chunk généré avec FlatTerrainGenerator.
 * Utilise la caméra FPS pour naviguer autour du chunk.
 * </p>
 *
 * @author Lucas Préaux
 * @version 1.0
 */
public class TestChunkMesh {

    public static void main(String[] args) {
        new TestChunkMesh().run();
    }

    private Window window;
    private Shader shader;
    private InputManager inputManager;
    private Camera camera;
    private CameraController cameraController;

    private BlockRegistry blockRegistry;
    private World world;
    private Mesh chunkMesh;

    public void run() {
        System.out.println("=== Test ChunkMesh - Rendu d'un chunk ===");
        init();
        loop();
        cleanup();
        System.out.println("=== Test terminé ===");
    }

    private void init() {
        // Initialiser GLFW
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        // Créer la fenêtre
        window = new Window(1280, 720, "Test ChunkMesh - Flat Terrain");
        window.centerOnScreen();
        window.makeContextCurrent();
        window.enableVsync();
        window.show();

        // Créer les capacités OpenGL
        GL.createCapabilities();

        // Configuration OpenGL
        glClearColor(0.53f, 0.81f, 0.92f, 1.0f); // Bleu ciel
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);

        // Créer le shader
        shader = new Shader("shaders/cube.vert", "shaders/cube.frag");

        // Créer l'input manager
        inputManager = new InputManager(window.getHandle());
        inputManager.captureCursor();

        // Créer la caméra
        camera = new Camera(new Vector3f(8, 70, 8), 0, 0);
        float aspectRatio = (float) window.getWidth() / window.getHeight();
        camera.updateProjectionMatrix(70.0f, aspectRatio, 0.1f, 1000.0f);

        // Créer le contrôleur de caméra
        cameraController = new CameraController(camera, inputManager);
        cameraController.setMoveSpeed(20.0f);

        // Initialiser le registre de blocs
        blockRegistry = new BlockRegistry();
        Blocks.register(blockRegistry);
        blockRegistry.freeze();

        // Créer le monde
        FlatTerrainGenerator generator = new FlatTerrainGenerator(blockRegistry);
        world = new World(generator, blockRegistry);

        // Générer un chunk
        System.out.println("Generating chunk...");
        Chunk chunk = world.getChunk(0, 0);

        // Créer le mesh du chunk
        System.out.println("Building chunk mesh...");
        ChunkMesh chunkMeshBuilder = new ChunkMesh(chunk, blockRegistry);
        chunkMesh = chunkMeshBuilder.build();

        System.out.println("Initialisation terminée !");
        System.out.println("Contrôles:");
        System.out.println("  - ZQSD : Déplacement");
        System.out.println("  - Souris : Regarder");
        System.out.println("  - Espace : Monter");
        System.out.println("  - Shift : Descendre");
        System.out.println("  - ESC : Quitter");
        System.out.println("Chunk mesh: " + chunkMesh);
    }

    private void loop() {
        System.out.println("Boucle de rendu démarrée");

        double lastTime = glfwGetTime();
        int frameCount = 0;
        double lastFpsTime = lastTime;

        while (!window.shouldClose()) {
            double currentTime = glfwGetTime();
            float deltaTime = (float) (currentTime - lastTime);
            lastTime = currentTime;

            // FPS counter
            frameCount++;
            if (currentTime - lastFpsTime >= 1.0) {
                System.out.printf("FPS: %d, Triangles: %d, Pos: (%.1f, %.1f, %.1f)%n",
                        frameCount, chunkMesh.getTriangleCount(),
                        camera.getPosition().x, camera.getPosition().y, camera.getPosition().z);
                frameCount = 0;
                lastFpsTime = currentTime;
            }

            // Mise à jour des entrées
            inputManager.update();
            glfwPollEvents();

            // Gestion des inputs
            handleInput();

            // Mise à jour de la caméra
            cameraController.update(deltaTime);
            camera.updateViewMatrix();

            // Rendu
            render();

            // Swap
            window.swapBuffers();
        }
    }

    private void handleInput() {
        if (inputManager.isKeyDown(GLFW_KEY_ESCAPE)) {
            glfwSetWindowShouldClose(window.getHandle(), true);
        }

        // Toggle cursor capture avec F1
        if (inputManager.isKeyJustPressed(GLFW_KEY_F1)) {
            if (inputManager.isCursorCaptured()) {
                inputManager.releaseCursor();
            } else {
                inputManager.captureCursor();
            }
        }
    }

    private void render() {
        // Clear
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // Binder le shader
        shader.bind();

        // Calculer MVP
        Matrix4f mvp = new Matrix4f(camera.getProjectionMatrix())
                .mul(camera.getViewMatrix());

        // Envoyer au shader
        shader.setUniform("uMVP", mvp);

        // Rendre le chunk
        chunkMesh.render();

        shader.unbind();
    }

    private void cleanup() {
        System.out.println("Nettoyage...");

        chunkMesh.cleanup();
        shader.cleanup();
        window.destroy();
        glfwTerminate();

        GLFWErrorCallback errorCallback = glfwSetErrorCallback(null);
        if (errorCallback != null) {
            errorCallback.free();
        }
    }
}
