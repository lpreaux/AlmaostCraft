package org.almostcraft.render;

import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_3D;
import static org.lwjgl.opengl.GL12.glTexImage3D;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.*;

/**
 * Texture array pour stocker plusieurs textures de même taille.
 * Compatible avec le greedy meshing et bien plus efficace qu'un atlas.
 *
 * @author Lucas Préaux
 * @version 1.0
 */
public class TextureArray {

    private static final Logger logger = LoggerFactory.getLogger(TextureArray.class);

    // ==================== Constantes ====================

    private static final int CHANNELS = 4; // RGBA
    private static final int MAX_LAYERS = 2048;

    // ==================== Attributs ====================

    private final int textureId;
    private final Map<String, Integer> textureIndices;
    private final List<String> texturePaths;
    private final List<TextureData> pendingTextures;

    private int textureSize = -1;
    private int layerCount = 0;
    private boolean built = false;

    // ==================== Classe interne TextureData ====================

    private static class TextureData {
        ByteBuffer pixels;
        int width;
        int height;
        boolean freed = false;

        TextureData(ByteBuffer pixels, int width, int height) {
            this.pixels = pixels;
            this.width = width;
            this.height = height;
        }

        void free() {
            if (pixels != null && !freed) {
                // TODO à vérifier, actuellement ça fait crash / Peut-être qu'on peut éconnomiser de la mémoire
                // STBImage.stbi_image_free(pixels);
                pixels = null;
                freed = true;
            }
        }
    }

    // ==================== Constructeur ====================

    public TextureArray() {
        this.textureId = glGenTextures();
        this.textureIndices = new HashMap<>();
        this.texturePaths = new ArrayList<>();
        this.pendingTextures = new ArrayList<>();

        logger.info("TextureArray created with ID {}", textureId);
    }

    // ==================== Construction ====================

    public int addTexture(String texturePath) throws IOException {
        if (built) {
            throw new IllegalStateException("Cannot add textures after build()");
        }

        if (textureIndices.containsKey(texturePath)) {
            logger.debug("Texture '{}' already added at index {}",
                    texturePath, textureIndices.get(texturePath));
            return textureIndices.get(texturePath);
        }

        TextureData texture = loadTexture(texturePath);

        if (textureSize == -1) {
            textureSize = texture.width;
            if (texture.width != texture.height) {
                texture.free();
                throw new IllegalArgumentException(
                        String.format("Texture must be square: %s (%dx%d)",
                                texturePath, texture.width, texture.height)
                );
            }

            logger.info("Detected texture size: {}x{}", textureSize, textureSize);
        }

        if (texture.width != textureSize || texture.height != textureSize) {
            texture.free();
            throw new IllegalArgumentException(
                    String.format("Texture size mismatch: %s is %dx%d, expected %dx%d",
                            texturePath, texture.width, texture.height,
                            textureSize, textureSize)
            );
        }

        if (layerCount >= MAX_LAYERS) {
            texture.free();
            throw new IllegalStateException(
                    String.format("Maximum texture layers (%d) exceeded", MAX_LAYERS)
            );
        }

        int index = layerCount++;
        textureIndices.put(texturePath, index);
        texturePaths.add(texturePath);
        pendingTextures.add(texture);

        logger.debug("Added texture '{}' at index {} - {}x{}",
                texturePath, index, textureSize, textureSize);

        return index;
    }

    public void build() {
        if (built) {
            throw new IllegalStateException("TextureArray already built");
        }
        if (pendingTextures.isEmpty()) {
            throw new IllegalStateException("Cannot build: no textures added");
        }

        logger.info("Building texture array: {} layers of {}x{} pixels",
                layerCount, textureSize, textureSize);

        long startTime = System.nanoTime();

        int layerSize = textureSize * textureSize * CHANNELS;
        ByteBuffer arrayBuffer = memAlloc(layerSize * layerCount);

        try {
            for (int i = 0; i < layerCount; i++) {
                TextureData texture = pendingTextures.get(i);
                texture.pixels.rewind();
                arrayBuffer.put(texture.pixels);
            }
            arrayBuffer.flip();

            uploadToGPU(arrayBuffer);

            built = true;

            long endTime = System.nanoTime();
            double duration = (endTime - startTime) / 1_000_000.0;
            double sizeMB = (layerSize * layerCount) / (1024.0 * 1024.0);

            logger.info("Texture array built: {}x{}x{} pixels, {} MB, {} ms",
                    textureSize, textureSize, layerCount,
                    String.format("%.2f", sizeMB),
                    String.format("%.2f", duration));

        } finally {
            memFree(arrayBuffer);
            pendingTextures.forEach(TextureData::free);
            pendingTextures.clear();
        }
    }

    // ==================== Chargement ====================

    private TextureData loadTexture(String path) throws IOException {
        ByteBuffer imageBuffer;
        try (InputStream stream = getClass().getClassLoader()
                .getResourceAsStream(path)) {
            if (stream == null) {
                throw new IOException("Texture not found: " + path);
            }
            imageBuffer = readStreamToBuffer(stream);
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            ByteBuffer pixels = STBImage.stbi_load_from_memory(
                    imageBuffer, w, h, channels, CHANNELS
            );

            if (pixels == null) {
                throw new IOException("Failed to load texture: " + path +
                        " - " + STBImage.stbi_failure_reason());
            }

            int width = w.get(0);
            int height = h.get(0);

            if (width != height) {
                STBImage.stbi_image_free(pixels);
                throw new IOException(
                        String.format("Texture must be square: %s (%dx%d)",
                                path, width, height)
                );
            }

            return new TextureData(pixels, width, height);
        } finally {
            memFree(imageBuffer);
        }
    }

    private ByteBuffer readStreamToBuffer(InputStream stream) throws IOException {
        try (ReadableByteChannel channel = Channels.newChannel(stream)) {
            ByteBuffer buffer = memAlloc(8192);

            while (channel.read(buffer) != -1) {
                if (buffer.remaining() == 0) {
                    buffer = memRealloc(buffer, buffer.capacity() * 2);
                }
            }

            buffer.flip();
            return buffer;
        }
    }

    // ==================== Upload GPU ====================

    private void uploadToGPU(ByteBuffer buffer) {
        bind();

        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER,
                GL_NEAREST_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        glTexImage3D(GL_TEXTURE_2D_ARRAY, 0, GL_RGBA8,
                textureSize, textureSize, layerCount,
                0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);

        glGenerateMipmap(GL_TEXTURE_2D_ARRAY);

        unbind();

        logger.debug("Texture array uploaded to GPU");
    }

    // ==================== Utilisation ====================

    public int getTextureIndex(String texturePath) {
        return textureIndices.getOrDefault(texturePath, -1);
    }

    public String getTexturePath(int index) {
        if (index < 0 || index >= texturePaths.size()) {
            return null;
        }
        return texturePaths.get(index);
    }

    public boolean contains(String texturePath) {
        return textureIndices.containsKey(texturePath);
    }

    public void bind(int textureUnit) {
        glActiveTexture(textureUnit);
        bind();
    }

    public void bind() {
        glBindTexture(GL_TEXTURE_2D_ARRAY, textureId);
    }

    public void unbind() {
        glBindTexture(GL_TEXTURE_2D_ARRAY, 0);
    }

    public void cleanup() {
        logger.debug("Cleaning up texture array {}", textureId);
        glDeleteTextures(textureId);

        pendingTextures.forEach(TextureData::free);
        pendingTextures.clear();
    }

    // ==================== Getters ====================

    public int getTextureId() {
        return textureId;
    }

    public int getLayerCount() {
        return layerCount;
    }

    public int getTextureSize() {
        return textureSize;
    }

    public boolean isBuilt() {
        return built;
    }
}
