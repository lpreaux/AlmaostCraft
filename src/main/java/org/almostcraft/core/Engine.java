package org.almostcraft.core;

import org.almostcraft.input.InputManager;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import java.io.IOException;
import java.util.Properties;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class Engine {
    private Window window;
    private InputManager inputManager;

    public void run() throws IOException {
        Properties props = new Properties();
        props.load(getClass().getResourceAsStream("/application.properties"));
        String version = props.getProperty("app.version");
        System.out.println("Starting AlmostCraft " + version);
        init();
        System.out.println("AlmostCraft has started");
        loop();
        System.out.println("AlmostCraft has finished. Cleaning up...");
        cleanup();
        System.out.println("AlmostCraft closed");
    }

    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        // Create the windows
        window = new Window(300, 300, "AlmostCraft");
        window.centerOnScreen();
        window.makeContextCurrent();
        window.enableVsync();
        window.show();

        inputManager = new InputManager(window.getHandle());
    }

    private void loop() {
        GL.createCapabilities();
        glClearColor(1.0f, 0.0f, 0.0f, 0.0f);

        while (!window.shouldClose()) {
            inputManager.update();

            glfwPollEvents();

           if (inputManager.isKeyDown(GLFW_KEY_ESCAPE)) {
               glfwSetWindowShouldClose(window.getHandle(), true);
           }
           if (inputManager.isKeyDown(GLFW_KEY_SPACE)) {
               glClearColor(0.0f, 1.0f, 0.0f, 0.0f);
           } else {
               glClearColor(1.0f, 0.0f, 0.0f, 0.0f);
           }
           glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer

            window.swapBuffers();
        }
    }

    private void update(float deltaTime) {

    }

    private void render() {

    }

    private void cleanup() {
        window.destroy();

        // Terminate GLFW and free the error callback
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }
}
