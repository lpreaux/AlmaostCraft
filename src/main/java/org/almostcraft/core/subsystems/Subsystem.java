package org.almostcraft.core.subsystems;

public interface Subsystem {
    void initialize();
    void update(float deltaTime);
    void cleanup();
}
