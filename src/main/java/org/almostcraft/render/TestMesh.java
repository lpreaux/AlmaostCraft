package org.almostcraft.render;

import org.almostcraft.core.Window;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * Test de la classe Mesh avec plusieurs cubes à différentes positions.
 * <p>
 * Démontre :
 * <ul>
 *   <li>L'utilisation de la classe Mesh</li>
 *   <li>Le rendu de plusieurs instances du même mesh</li>
 *   <li>Les transformations Model différentes pour chaque cube</li>
 * </ul>
 * </p>
 *
 * @author Lucas Préaux
 * @version 1.0
 */
public class TestMesh {

    public static void main(String[] args) {
        new TestMesh().run();
    }

    /**
     * Représente un cube avec sa position et sa rotation.
     */
    private static class CubeInstance {
        Vector3f position;
        Vector3f rotation;
        float rotationSpeed;

        CubeInstance(Vector3f position, Vector3f rotation, float rotationSpeed) {
            this.position = position;
            this.rotation = rotation;
            this.rotationSpeed = rotationSpeed;
        }
    }

    private Window window;
    private Shader shader;
    private Mesh cubeMesh;
    private List<CubeInstance> cubes;

    private Matrix4f projectionMatrix;
    private Matrix4f viewMatrix;

    public void run() {
        System.out.println("=== Test Mesh - Plusieurs cubes ===");
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
        window = new Window(1024, 768, "Test Mesh - Multiple Cubes");
        window.centerOnScreen();
        window.makeContextCurrent();
        window.enableVsync();
        window.show();

        // Créer les capacités OpenGL
        GL.createCapabilities();

        // Configuration OpenGL
        glClearColor(0.05f, 0.05f, 0.1f, 1.0f);
        glEnable(GL_DEPTH_TEST);
        // glEnable(GL_CULL_FACE);

        // Créer le shader
        shader = new Shader("shaders/cube.vert", "shaders/cube.frag");

        // Créer le mesh du cube
        cubeMesh = createCubeMesh();

        // Créer plusieurs instances de cubes
        createCubeInstances();

        // Configurer les matrices
        setupMatrices();

        System.out.println("Initialisation terminée !");
        System.out.println("Contrôles : ESC pour quitter");
        System.out.println(cubeMesh);
    }

    /**
     * Crée le mesh d'un cube unitaire.
     */
    private Mesh createCubeMesh() {
        // Vertices : position (x,y,z) + couleur (r,g,b)
        float[] vertices = {
                // Face arrière (-Z) - Rouge
                -0.5f, -0.5f, -0.5f,    1.0f, 0.2f, 0.2f,
                0.5f, -0.5f, -0.5f,    1.0f, 0.2f, 0.2f,
                0.5f,  0.5f, -0.5f,    1.0f, 0.2f, 0.2f,
                -0.5f,  0.5f, -0.5f,    1.0f, 0.2f, 0.2f,

                // Face avant (+Z) - Vert
                -0.5f, -0.5f,  0.5f,    0.2f, 1.0f, 0.2f,
                0.5f, -0.5f,  0.5f,    0.2f, 1.0f, 0.2f,
                0.5f,  0.5f,  0.5f,    0.2f, 1.0f, 0.2f,
                -0.5f,  0.5f,  0.5f,    0.2f, 1.0f, 0.2f,

                // Face gauche (-X) - Bleu
                -0.5f, -0.5f, -0.5f,    0.2f, 0.2f, 1.0f,
                -0.5f,  0.5f, -0.5f,    0.2f, 0.2f, 1.0f,
                -0.5f,  0.5f,  0.5f,    0.2f, 0.2f, 1.0f,
                -0.5f, -0.5f,  0.5f,    0.2f, 0.2f, 1.0f,

                // Face droite (+X) - Jaune
                0.5f, -0.5f, -0.5f,    1.0f, 1.0f, 0.2f,
                0.5f,  0.5f, -0.5f,    1.0f, 1.0f, 0.2f,
                0.5f,  0.5f,  0.5f,    1.0f, 1.0f, 0.2f,
                0.5f, -0.5f,  0.5f,    1.0f, 1.0f, 0.2f,

                // Face du bas (-Y) - Magenta
                -0.5f, -0.5f, -0.5f,    1.0f, 0.2f, 1.0f,
                0.5f, -0.5f, -0.5f,    1.0f, 0.2f, 1.0f,
                0.5f, -0.5f,  0.5f,    1.0f, 0.2f, 1.0f,
                -0.5f, -0.5f,  0.5f,    1.0f, 0.2f, 1.0f,

                // Face du haut (+Y) - Cyan
                -0.5f,  0.5f, -0.5f,    0.2f, 1.0f, 1.0f,
                0.5f,  0.5f, -0.5f,    0.2f, 1.0f, 1.0f,
                0.5f,  0.5f,  0.5f,    0.2f, 1.0f, 1.0f,
                -0.5f,  0.5f,  0.5f,    0.2f, 1.0f, 1.0f
        };

        // Indices (6 faces × 2 triangles × 3 sommets)
        int[] indices = {
                // Arrière
                0, 1, 2,  2, 3, 0,
                // Avant
                4, 6, 5,  6, 4, 7,
                // Gauche
                8, 9, 10,  10, 11, 8,
                // Droite
                12, 14, 13,  14, 12, 15,
                // Bas
                16, 17, 18,  18, 19, 16,
                // Haut
                20, 22, 21,  22, 20, 23
        };

        Mesh mesh = new Mesh();
        mesh.uploadData(vertices, indices);
        return mesh;
    }

    /**
     * Crée plusieurs instances de cubes à différentes positions.
     */
    private void createCubeInstances() {
        cubes = new ArrayList<>();

        // Grille 3×3 de cubes
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    Vector3f position = new Vector3f(x * 2.5f, y * 2.5f, z * 2.5f);
                    Vector3f rotation = new Vector3f(x * 15, y * 20, z * 25);
                    float rotationSpeed = 20.0f + (x + y + z) * 5.0f;

                    cubes.add(new CubeInstance(position, rotation, rotationSpeed));
                }
            }
        }

        System.out.println("Créé " + cubes.size() + " instances de cubes");
    }

    /**
     * Configure les matrices de projection et vue.
     */
    private void setupMatrices() {
        float aspectRatio = (float) window.getWidth() / window.getHeight();

        projectionMatrix = new Matrix4f().perspective(
                (float) Math.toRadians(70.0f),
                aspectRatio,
                0.1f,
                100.0f
        );

        // Caméra plus loin pour voir tous les cubes
        viewMatrix = new Matrix4f().lookAt(
                new Vector3f(8, 6, 10),
                new Vector3f(0, 0, 0),
                new Vector3f(0, 1, 0)
        );
    }

    private void loop() {
        System.out.println("Boucle de rendu démarrée");

        double lastTime = glfwGetTime();

        while (!window.shouldClose()) {
            double currentTime = glfwGetTime();
            float deltaTime = (float) (currentTime - lastTime);
            lastTime = currentTime;

            // Mettre à jour les rotations
            for (CubeInstance cube : cubes) {
                cube.rotation.x += deltaTime * cube.rotationSpeed;
                cube.rotation.y += deltaTime * cube.rotationSpeed * 0.7f;
            }

            // Clear
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            // Binder le shader
            shader.bind();

            // Rendre chaque cube
            for (CubeInstance cube : cubes) {
                // Matrice Model pour ce cube
                Matrix4f modelMatrix = new Matrix4f()
                        .identity()
                        .translate(cube.position)
                        .rotateX((float) Math.toRadians(cube.rotation.x))
                        .rotateY((float) Math.toRadians(cube.rotation.y))
                        .rotateZ((float) Math.toRadians(cube.rotation.z));

                // MVP
                Matrix4f mvp = new Matrix4f(projectionMatrix)
                        .mul(viewMatrix)
                        .mul(modelMatrix);

                // Envoyer au shader
                shader.setUniform("uMVP", mvp);

                // Rendre le mesh
                cubeMesh.render();
            }

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

        cubeMesh.cleanup();
        shader.cleanup();
        window.destroy();
        glfwTerminate();

        GLFWErrorCallback errorCallback = glfwSetErrorCallback(null);
        if (errorCallback != null) {
            errorCallback.free();
        }
    }
}
