package org.almostcraft.render;

import org.almostcraft.core.Window;
import org.almostcraft.render.core.Shader;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Classe de test pour valider le système de shaders.
 * <p>
 * Affiche un triangle coloré rotatif pour vérifier que :
 * <ul>
 *   <li>Les shaders se compilent correctement</li>
 *   <li>Les uniforms fonctionnent</li>
 *   <li>Le rendu OpenGL est opérationnel</li>
 * </ul>
 * </p>
 * <p>
 * Pour tester, exécute cette classe directement (elle a son propre main).
 * </p>
 *
 * @author Lucas Préaux
 * @version 1.0
 */
public class TestShader {

    public static void main(String[] args) {
        new TestShader().run();
    }

    private Window window;
    private Shader shader;
    private int vao;
    private int vbo;

    public void run() {
        System.out.println("=== Test Shader ===");
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

        // ⚠️ CRUCIAL pour macOS : Configurer les hints OpenGL AVANT glfwCreateWindow
        glfwDefaultWindowHints(); // Réinitialiser les hints
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE); // Requis sur macOS

        // Créer la fenêtre
        window = new Window(800, 600, "Test Shader - Triangle");
        window.centerOnScreen();
        window.makeContextCurrent();
        window.enableVsync();
        window.show();

        // Créer les capacités OpenGL
        GL.createCapabilities();

        // Couleur de fond
        glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        // Créer le shader
        shader = new Shader("shaders/basic.vert", "shaders/basic.frag");

        // Créer un triangle
        createTriangle();

        System.out.println("Initialisation terminée !");
    }

    /**
     * Crée un triangle simple pour tester le rendu.
     */
    private void createTriangle() {
        // Vertices du triangle (position x, y, z)
        float[] vertices = {
                // Sommet 1 (en haut, centre)
                0.0f, 0.5f, 0.0f,
                // Sommet 2 (en bas à gauche)
                -0.5f, -0.5f, 0.0f,
                // Sommet 3 (en bas à droite)
                0.5f, -0.5f, 0.0f
        };

        // Créer le VAO (Vertex Array Object)
        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        // Créer le VBO (Vertex Buffer Object)
        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        // Configurer l'attribut de position (location 0)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        // Débinder
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        System.out.println("Triangle créé (VAO: " + vao + ", VBO: " + vbo + ")");
    }

    private void loop() {
        System.out.println("Boucle de rendu démarrée (appuie sur ESC pour quitter)");

        double lastTime = glfwGetTime();
        float rotation = 0.0f;

        while (!window.shouldClose()) {
            double currentTime = glfwGetTime();
            float deltaTime = (float) (currentTime - lastTime);
            lastTime = currentTime;

            // Rotation progressive
            rotation += deltaTime * 50.0f; // 50 degrés par seconde

            // Clear
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            // Utiliser le shader
            shader.bind();

            // Créer une matrice MVP simple (juste une rotation autour de Z)
            Matrix4f mvp = new Matrix4f()
                    .identity()
                    .rotateZ((float) Math.toRadians(rotation));

            // Envoyer la matrice au shader
            shader.setUniform("uMVP", mvp);

            // Dessiner le triangle
            glBindVertexArray(vao);
            glDrawArrays(GL_TRIANGLES, 0, 3);
            glBindVertexArray(0);

            // Débinder le shader
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

        // Nettoyer le triangle
        glDeleteBuffers(vbo);
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
