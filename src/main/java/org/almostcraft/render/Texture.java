package org.almostcraft.render;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;

/**
 * Représente une texture OpenGL chargée depuis un fichier PNG.
 */
public class Texture {

    private static final Logger logger = LoggerFactory.getLogger(Texture.class);

    private final int id;
    private final int width;
    private final int height;
    private final String path;

    /**
     * Charge une texture depuis le classpath.
     *
     * @param path chemin relatif depuis resources/ (ex: "textures/blocks/stone.png")
     */
    public Texture(String path) throws IOException {
        this.path = path;

        // Charger l'image depuis le classpath
        ImageData image = loadImage(path);
        this.width = image.width;
        this.height = image.height;

        // Générer la texture OpenGL
        this.id = glGenTextures();

        bind();

        // Paramètres de texture (pixel art = NEAREST)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        // Upload la texture
        glTexImage2D(
                GL_TEXTURE_2D,
                0,
                GL_RGBA,
                width,
                height,
                0,
                GL_RGBA,
                GL_UNSIGNED_BYTE,
                image.pixels
        );

        // Générer les mipmaps (APRÈS glTexImage2D)
        glGenerateMipmap(GL_TEXTURE_2D);

        unbind();

        logger.debug("Texture loaded: {} ({}x{})", path, width, height);
    }

    /**
     * Charge une image PNG depuis le classpath.
     */
    private ImageData loadImage(String path) throws IOException {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(path);
        if (stream == null) {
            throw new IOException("Texture not found in classpath: " + path);
        }

        BufferedImage image = ImageIO.read(stream);
        int width = image.getWidth();
        int height = image.getHeight();

        // Convertir en RGBA
        int[] pixels = new int[width * height];
        image.getRGB(0, 0, width, height, pixels, 0, width);

        ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 4);

        for (int pixel : pixels) {
            buffer.put((byte) ((pixel >> 16) & 0xFF)); // R
            buffer.put((byte) ((pixel >> 8) & 0xFF));  // G
            buffer.put((byte) (pixel & 0xFF));         // B
            buffer.put((byte) ((pixel >> 24) & 0xFF)); // A
        }

        buffer.flip();

        return new ImageData(buffer, width, height);
    }

    public void bind() {
        glBindTexture(GL_TEXTURE_2D, id);
    }

    public void unbind() {
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public void cleanup() {
        logger.trace("Deleting texture: {}", path);
        glDeleteTextures(id);
    }

    // Getters
    public int getId() { return id; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public String getPath() { return path; }

    private record ImageData(ByteBuffer pixels, int width, int height) {}
}
