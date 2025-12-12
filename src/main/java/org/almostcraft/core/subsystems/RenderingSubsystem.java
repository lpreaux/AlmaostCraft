package org.almostcraft.core.subsystems;

import org.almostcraft.camera.Camera;
import org.almostcraft.render.ChunkRenderer;
import org.almostcraft.render.Shader;
import org.almostcraft.render.TextureArray;
import org.almostcraft.world.World;
import org.almostcraft.world.block.BlockRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class RenderingSubsystem implements Subsystem {
    private static final Logger logger = LoggerFactory.getLogger(RenderingSubsystem.class);

    private final Shader shader;
    private final TextureArray textureArray;
    private final ChunkRenderer chunkRenderer;

    // ==================== Constructeur ====================

    public RenderingSubsystem(World world, BlockRegistry blockRegistry) {
        logger.info("Creating rendering subsystem");

        // Créer les objets (pas encore d'upload GPU)
        this.shader = new Shader("shaders/block.vert", "shaders/block.frag");
        this.textureArray = createTextureArray(); // Charge les images
        this.chunkRenderer = new ChunkRenderer(world, blockRegistry, shader, textureArray);

        logger.debug("Rendering subsystem created (not yet initialized)");
    }

    // ==================== Lifecycle ====================

    @Override
    public void initialize() {
        logger.info("Initializing rendering subsystem");

        textureArray.build(); // Upload GPU

        logger.info("Rendering subsystem initialized");
    }

    @Override
    public void update(float deltaTime) {
        chunkRenderer.update();
    }

    public void render(Camera camera) {
        chunkRenderer.render(camera);
    }

    @Override
    public void cleanup() {
        logger.info("Cleaning up rendering subsystem");
        chunkRenderer.cleanup();
        textureArray.cleanup();
        shader.cleanup();
    }

    // ==================== Helper ====================

    private TextureArray createTextureArray() {
        try {
            TextureArray array = new TextureArray();

            // Charger les images (pas encore d'upload GPU)
            array.addTexture("textures/blocks/stone.png");
            array.addTexture("textures/blocks/dirt.png");
            array.addTexture("textures/blocks/cobblestone.png");
            array.addTexture("textures/blocks/sand.png");
            array.addTexture("textures/blocks/oak_planks.png");
            array.addTexture("textures/blocks/glass.png");
            array.addTexture("textures/blocks/grass_block_top.png");
            array.addTexture("textures/blocks/grass_block_side.png");

            // PAS de build() ici ! Fait dans initialize()
            return array;

        } catch (IOException e) {
            logger.error("Failed to create texture array", e);
            throw new RuntimeException("Failed to create texture array", e);
        }
    }

    // ==================== Getters ====================

    public ChunkRenderer getChunkRenderer() {
        return chunkRenderer;
    }
}