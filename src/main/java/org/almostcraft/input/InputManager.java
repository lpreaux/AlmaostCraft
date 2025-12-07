package org.almostcraft.input;

import static org.lwjgl.glfw.GLFW.*;

public class InputManager {
    private final KeyboardInput keyboardInput;
    private final MouseInput mouseInput;
    private final long windowHandle;

    public InputManager(long windowHandle) {
        this.windowHandle = windowHandle;
        this.keyboardInput = new KeyboardInput();
        this.mouseInput = new MouseInput();

        registerCallback();
    }

    private void registerCallback() {
        glfwSetKeyCallback(windowHandle, (window, key, scancode, action, mods) -> {
            keyboardInput.onKeyEvent(key, action);
        });
        glfwSetMouseButtonCallback(windowHandle, (window, button, action, mods) -> {
            mouseInput.onMouseButtonEvent(button, action);
        });
        glfwSetCursorPosCallback(windowHandle, (window, x, y) -> {
            mouseInput.onCursorPosEvent(x, y);
        });
        glfwSetScrollCallback(windowHandle, (window, x, y) -> {
            mouseInput.onScrollEvent(x, y);
        });
    }

    public void update() {
        keyboardInput.update();
        mouseInput.update();
    }

    public boolean isKeyDown(int keyCode) {
        return keyboardInput.isKeyDown(keyCode);
    }

    public boolean isKeyJustPressed(int keyCode) {
        return keyboardInput.isKeyJustPressed(keyCode);
    }

    public double getMouseX() {
        return mouseInput.getMouseX();
    }

    public double getMouseY() {
        return mouseInput.getMouseY();
    }

    public double getMouseDeltaX() {
        return mouseInput.getDeltaX();
    }

    public double getMouseDeltaY() {
        return mouseInput.getDeltaY();
    }

    public boolean isMouseButtonPressed(int button) {
        return mouseInput.isButtonPressed(button);
    }

    public boolean isMouseButtonJustPressed(int button) {
        return mouseInput.isButtonJustPressed(button);
    }

    public double getScrollX() {
        return mouseInput.getScrollX();
    }

    public double getScrollY() {
        return mouseInput.getScrollY();
    }

    public boolean isCursorCaptured() {
        return glfwGetInputMode(windowHandle, GLFW_CURSOR) == GLFW_CURSOR_DISABLED;
    }

    public void setMouseSensitivity(float sensitivity) {
        mouseInput.setSensitivity(sensitivity);
    }

    public void hideCursor() {
        glfwSetInputMode(windowHandle, GLFW_CURSOR, GLFW_CURSOR_HIDDEN);
    }

    public void captureCursor() {
        glfwSetInputMode(windowHandle, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        mouseInput.resetDelta();
    }

    public void releaseCursor() {
        glfwSetInputMode(windowHandle, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
    }
}
