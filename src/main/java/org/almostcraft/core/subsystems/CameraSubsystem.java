package org.almostcraft.core.subsystems;

import org.almostcraft.camera.Camera;
import org.almostcraft.camera.CameraController;
import org.almostcraft.core.Window;
import org.almostcraft.input.InputManager;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CameraSubsystem implements Subsystem {
    private static final Logger logger = LoggerFactory.getLogger(CameraSubsystem.class);

    // ==================== Constantes ====================

    private static final float FOV = 70.0f;
    private static final float Z_NEAR = 0.1f;
    private static final float Z_FAR = 1000.0f;
    private static final float DEFAULT_MOVE_SPEED = 20.0f;

    // Position de spawn par défaut
    private static final Vector3f SPAWN_POSITION = new Vector3f(8, 70, 8);

    // ==================== Attributs ====================

    private final Camera camera;
    private final CameraController controller;
    private final Window window;

    // ==================== Constructeur ====================

    public CameraSubsystem(InputManager inputManager, Window window) {
        logger.debug("Creating camera subsystem");

        this.window = window;
        this.camera = new Camera(SPAWN_POSITION, 0, 0);
        this.controller = new CameraController(camera, inputManager);

        logger.debug("Camera subsystem created");
    }

    // ==================== Lifecycle ====================

    @Override
    public void initialize() {
        logger.info("Initializing camera subsystem");

        // Configuration initiale de la projection
        handleWindowResize(window.getWidth(), window.getHeight());

        // Configuration du contrôleur
        controller.setMoveSpeed(DEFAULT_MOVE_SPEED);

        logger.info("Camera initialized at position {}", camera.getPosition());
    }

    @Override
    public void update(float deltaTime) {
        controller.update(deltaTime);
        camera.updateViewMatrix();
    }

    @Override
    public void cleanup() {
        // Rien à nettoyer pour le moment
        logger.debug("Camera subsystem cleaned up");
    }

    // ==================== Window Events ====================

    /**
     * Gère le redimensionnement de la fenêtre.
     * <p>
     * Met à jour la matrice de projection pour maintenir le bon aspect ratio.
     * </p>
     */
    public void handleWindowResize(int width, int height) {
        logger.debug("Handling window resize: {}x{}", width, height);
        float aspectRatio = (float) width / height;
        camera.updateProjectionMatrix(FOV, aspectRatio, Z_NEAR, Z_FAR);
    }

    // ==================== Getters ====================

    public Camera getCamera() {
        return camera;
    }

    public CameraController getController() {
        return controller;
    }
}