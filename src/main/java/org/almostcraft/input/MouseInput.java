package org.almostcraft.input;

import java.util.Arrays;

import static org.lwjgl.glfw.GLFW.*;

public class MouseInput {
    private static final int BUTTON_COUNT = GLFW_MOUSE_BUTTON_LAST + 1;

    private double mouseX, mouseY;
    private double deltaX, deltaY;
    private float sensitivity = 1.0f;
    private boolean firstMove = true;

    private final boolean[] buttons = new boolean[BUTTON_COUNT];
    private final boolean[] buttonsJustPressed = new boolean[BUTTON_COUNT];

    private double scrollX, scrollY;


    void onCursorPosEvent(double x, double y) {
        if (firstMove) {
            mouseX = x;
            mouseY = y;
            firstMove = false;
            return;
        }

        deltaX = x - mouseX;
        deltaY = y - mouseY;

        mouseX = x;
        mouseY = y;
    }

    void onMouseButtonEvent(int button, int action) {
        if (!isValidMouseButton(button)) {
            return;
        }

        if (action == GLFW_PRESS) {
            buttons[button] = true;
            buttonsJustPressed[button] = true;
        } else if (action == GLFW_RELEASE) {
            buttons[button] = false;
        }
    }

    void onScrollEvent(double x, double y) {
        scrollX = x;
        scrollY = y;
    }

    void update() {
        deltaX = 0;
        deltaY = 0;

        Arrays.fill(buttonsJustPressed, false);

        scrollX = 0;
        scrollY = 0;
    }

    public double getMouseX() {
        return mouseX;
    }

    public double getMouseY() {
        return mouseY;
    }

    public double getDeltaX() {
        return deltaX * sensitivity;
    }

    public double getDeltaY() {
        return deltaY * sensitivity;
    }

    public boolean isButtonPressed(int button) {
        if (!isValidMouseButton(button)) {
            return false;
        }
        return buttons[button];
    }

    public boolean isButtonJustPressed(int button) {
        if (!isValidMouseButton(button)) {
            return false;
        }
        return buttonsJustPressed[button];
    }

    public double getScrollX() {
        return scrollX;
    }

    public double getScrollY() {
        return scrollY;
    }

    private boolean isValidMouseButton(int button) {
        return button >= 0 && button < BUTTON_COUNT;
    }

    public void setSensitivity(float sensitivity) {
        this.sensitivity = sensitivity;
    }

    public void resetDelta() {
        firstMove = true;
    }
}
