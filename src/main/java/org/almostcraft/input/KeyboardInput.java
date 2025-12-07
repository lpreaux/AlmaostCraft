package org.almostcraft.input;

import java.util.Arrays;

import static org.lwjgl.glfw.GLFW.*;

public class KeyboardInput {
    private static final int KEY_COUNT = GLFW_KEY_LAST + 1;

    private final boolean[] keys = new boolean[KEY_COUNT];
    private final boolean[] keysJustPressed = new boolean[KEY_COUNT];

    void onKeyEvent(int keyCode, int action) {
        if (!isValidKeyCode(keyCode)) {
            return;
        }

        if (action == GLFW_PRESS) {
            keys[keyCode] = true;
            keysJustPressed[keyCode] = true;
        } else if (action == GLFW_RELEASE) {
            keys[keyCode] = false;
        }
    }

    void update() {
        Arrays.fill(keysJustPressed, false);
    }

    public boolean isKeyDown(int keyCode) {
        if (!isValidKeyCode(keyCode)) {
            return false;
        }
        return keys[keyCode];
    }

    public boolean isKeyJustPressed(int keyCode) {
        if (!isValidKeyCode(keyCode)) {
            return false;
        }
        return keysJustPressed[keyCode];
    }

    private boolean isValidKeyCode(int key) {
        return key >= 0 && key < KEY_COUNT;
    }
}
