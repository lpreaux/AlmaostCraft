package org.almostcraft.render;

import org.almostcraft.core.Window;
import org.almostcraft.render.core.Shader;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Test de rendu d'un cube 3D rotatif avec couleurs par face.
 * <p>
 * Démontre :
 * <ul>
 *   <li>Le rendu 3D avec depth testing</li>
 *   <li>Les transformations 3D (rotation sur X et Y)</li>
 *   <li>L'utilisation de matrices Model/View/Projection</li>
 *   <li>Les couleurs RGB par sommet</li>
 * </ul>
 * </p>
 *
 * @author Lucas Préaux
 * @version 1.0
 */
public class TestCube {

    public static void main(String[] args) {
        new TestCube().run();
    }

    private Window window;
    private Shader shader;
    private int vao;
    private int vbo;
    private int ebo; // Element Buffer Object pour les indices

    private Matrix4f projectionMatrix;
    private Matrix4f viewMatrix;

    public void run() {
        System.out.println("=== Test Cube 3D ===");
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
        window = new Window(800, 600, "Test Cube 3D - Rotation");
        window.centerOnScreen();
        window.makeContextCurrent();
        window.enableVsync();
        window.show();

        // Créer les capacités OpenGL
        GL.createCapabilities();

        // Configuration OpenGL
        glClearColor(0.1f, 0.1f, 0.15f, 1.0f);
        glEnable(GL_DEPTH_TEST); // ⚠️ CRUCIAL pour le rendu 3D !
        glDepthFunc(GL_LESS);

        // Optionnel : Activer le backface culling (optimisation)
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glFrontFace(GL_CCW); // Counter-clockwise = face avant

        // Créer le shader
        shader = new Shader("shaders/cube.vert", "shaders/cube.frag");

        // Créer le cube
        createCube();

        // Configurer les matrices de projection et vue
        setupMatrices();

        System.out.println("Initialisation terminée !");
        System.out.println("Contrôles : ESC pour quitter");
    }

    /**
     * Configure les matrices de projection (perspective) et de vue (caméra).
     */
    private void setupMatrices() {
        float aspectRatio = (float) window.getWidth() / window.getHeight();

        // Matrice de projection : perspective 3D
        projectionMatrix = new Matrix4f().perspective(
                (float) Math.toRadians(70.0f), // FOV
                aspectRatio,                    // Aspect ratio
                0.1f,                           // Near plane
                100.0f                          // Far plane
        );

        // Matrice de vue : caméra reculée de 5 unités sur l'axe Z
        viewMatrix = new Matrix4f().lookAt(
                new Vector3f(0, 0, 5),    // Position de la caméra
                new Vector3f(0, 0, 0),    // Point regardé (origine)
                new Vector3f(0, 1, 0)     // Vecteur "up"
        );
    }

    /**
     * Crée un cube avec 8 sommets, 36 indices et couleurs RGB.
     * <p>
     * Utilise un Element Buffer Object (EBO) pour éviter la duplication
     * des vertices (8 sommets au lieu de 36).
     * </p>
     */
    private void createCube() {
        // ==================== Vertices ====================
        // Format : x, y, z, r, g, b
        // 8 sommets du cube (de -0.5 à +0.5)
        float[] vertices = {
                // Position             // Couleur RGB
                // Sommets de la face arrière (z = -0.5)
                -0.5f, -0.5f, -0.5f,    1.0f, 0.0f, 0.0f,  // 0: Rouge
                0.5f, -0.5f, -0.5f,    0.0f, 1.0f, 0.0f,  // 1: Vert
                0.5f,  0.5f, -0.5f,    0.0f, 0.0f, 1.0f,  // 2: Bleu
                -0.5f,  0.5f, -0.5f,    1.0f, 1.0f, 0.0f,  // 3: Jaune

                // Sommets de la face avant (z = +0.5)
                -0.5f, -0.5f,  0.5f,    1.0f, 0.0f, 1.0f,  // 4: Magenta
                0.5f, -0.5f,  0.5f,    0.0f, 1.0f, 1.0f,  // 5: Cyan
                0.5f,  0.5f,  0.5f,    1.0f, 1.0f, 1.0f,  // 6: Blanc
                -0.5f,  0.5f,  0.5f,    0.5f, 0.5f, 0.5f   // 7: Gris
        };

        // ==================== Indices ====================
        // 6 faces × 2 triangles × 3 sommets = 36 indices
        // Ordre : Counter-Clockwise (CCW) quand on regarde la face de l'extérieur
        int[] indices = {
                // Face arrière (-Z)
                0, 1, 2,  2, 3, 0,
                // Face avant (+Z)
                4, 6, 5,  6, 4, 7,
                // Face gauche (-X)
                0, 3, 7,  7, 4, 0,
                // Face droite (+X)
                1, 5, 6,  6, 2, 1,
                // Face du bas (-Y)
                0, 4, 5,  5, 1, 0,
                // Face du haut (+Y)
                3, 2, 6,  6, 7, 3
        };

        // ==================== Créer VAO ====================
        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        // ==================== Créer VBO ====================
        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        // Attribut 0 : Position (x, y, z)
        int stride = 6 * Float.BYTES; // 6 floats par vertex (3 pos + 3 couleur)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);

        // Attribut 1 : Couleur (r, g, b)
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        // ==================== Créer EBO ====================
        ebo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

        // Débinder
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        System.out.println("Cube créé : VAO=" + vao + ", VBO=" + vbo + ", EBO=" + ebo);
        System.out.println("  - 8 vertices, 36 indices (12 triangles)");
    }

    private void loop() {
        System.out.println("Boucle de rendu démarrée");

        double lastTime = glfwGetTime();
        float rotationX = 0.0f;
        float rotationY = 0.0f;

        while (!window.shouldClose()) {
            double currentTime = glfwGetTime();
            float deltaTime = (float) (currentTime - lastTime);
            lastTime = currentTime;

            // Rotations progressives
            rotationX += deltaTime * 30.0f; // 30 degrés/sec sur X
            rotationY += deltaTime * 50.0f; // 50 degrés/sec sur Y

            // Clear (couleur + profondeur !)
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            // Utiliser le shader
            shader.bind();

            // ==================== Matrices ====================
            // 1. Model : Rotation du cube
            Matrix4f modelMatrix = new Matrix4f()
                    .identity()
                    .rotateX((float) Math.toRadians(rotationX))
                    .rotateY((float) Math.toRadians(rotationY));

            // 2. Model-View-Projection
            Matrix4f mvp = new Matrix4f(projectionMatrix)
                    .mul(viewMatrix)
                    .mul(modelMatrix);

            // Envoyer au shader
            shader.setUniform("uMVP", mvp);

            // ==================== Dessiner ====================
            glBindVertexArray(vao);
            glDrawElements(GL_TRIANGLES, 36, GL_UNSIGNED_INT, 0);
            glBindVertexArray(0);

            shader.unbind();

            // Swap et events
            window.swapBuffers();
            glfwPollEvents();

            // ESC pour quitter
            if (glfwGetKey(window.getHandle(), GLFW_KEY_ESCAPE) == GLFW_PRESS) {
                glfwSetWindowShouldClose(window.getHandle(), true);
            }
        }
    }

    private void cleanup() {
        System.out.println("Nettoyage...");

        // Nettoyer le cube
        glDeleteBuffers(vbo);
        glDeleteBuffers(ebo);
        glDeleteVertexArrays(vao);

        // Nettoyer le shader
        shader.cleanup();

        // Nettoyer la fenêtre
        window.destroy();
        glfwTerminate();

        GLFWErrorCallback errorCallback = glfwSetErrorCallback(null);
        if (errorCallback != null) {
            errorCallback.free();
        }
    }
}
