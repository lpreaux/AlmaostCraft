package org.almostcraft.render;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL20.*;

/**
 * Représente un programme shader OpenGL compilé et prêt à l'emploi.
 * <p>
 * Cette classe gère :
 * <ul>
 *   <li>Le chargement des shaders depuis des fichiers</li>
 *   <li>La compilation et validation des shaders</li>
 *   <li>Le linking du programme shader</li>
 *   <li>La gestion des uniforms (variables shader)</li>
 *   <li>Le nettoyage des ressources OpenGL</li>
 * </ul>
 * </p>
 * <p>
 * Utilisation typique :
 * <pre>{@code
 * Shader shader = new Shader("shaders/basic.vert", "shaders/basic.frag");
 * shader.bind();
 * shader.setUniform("mvp", camera.getViewProjectionMatrix());
 * // ... render ...
 * shader.unbind();
 * shader.cleanup();
 * }</pre>
 * </p>
 *
 * @author Lucas Préaux
 * @version 1.0
 */
public class Shader {

    private static final Logger logger = LoggerFactory.getLogger(Shader.class);

    // ==================== Attributs ====================

    /**
     * ID du programme shader OpenGL.
     */
    private final int programId;

    /**
     * ID du vertex shader compilé.
     */
    private final int vertexShaderId;

    /**
     * ID du fragment shader compilé.
     */
    private final int fragmentShaderId;

    /**
     * Cache des locations des uniforms pour éviter les appels OpenGL répétés.
     */
    private final Map<String, Integer> uniformLocations;

    // ==================== Constructeur ====================

    /**
     * Crée et compile un programme shader à partir de fichiers.
     *
     * @param vertexPath   chemin vers le vertex shader (ex: "shaders/basic.vert")
     * @param fragmentPath chemin vers le fragment shader (ex: "shaders/basic.frag")
     * @throws RuntimeException si la compilation ou le linking échoue
     */
    public Shader(String vertexPath, String fragmentPath) {
        logger.debug("Creating shader program: vertex='{}', fragment='{}'", vertexPath, fragmentPath);

        this.uniformLocations = new HashMap<>();

        // Compiler les shaders
        this.vertexShaderId = compileShader(vertexPath, ShaderType.VERTEX);
        this.fragmentShaderId = compileShader(fragmentPath, ShaderType.FRAGMENT);

        // Créer et linker le programme
        this.programId = linkProgram(vertexShaderId, fragmentShaderId);

        logger.info("Shader program created successfully (ID: {})", programId);
    }

    // ==================== Compilation ====================

    /**
     * Compile un shader depuis un fichier.
     *
     * @param path chemin vers le fichier shader
     * @param type type de shader (VERTEX, FRAGMENT, etc.)
     * @return l'ID OpenGL du shader compilé
     * @throws RuntimeException si la compilation échoue
     */
    private int compileShader(String path, ShaderType type) {
        logger.debug("Compiling {} shader: {}", type.name().toLowerCase(), path);

        // Charger le code source
        String source = loadShaderSource(path);

        // Créer le shader OpenGL
        int shaderId = glCreateShader(type.getGlType());
        if (shaderId == 0) {
            throw new RuntimeException("Failed to create shader object for: " + path);
        }

        // Envoyer le source et compiler
        glShaderSource(shaderId, source);
        glCompileShader(shaderId);

        // Vérifier la compilation
        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == GL_FALSE) {
            String errorLog = glGetShaderInfoLog(shaderId);
            glDeleteShader(shaderId);
            logger.error("Shader compilation failed for '{}': {}", path, errorLog);
            throw new RuntimeException("Shader compilation failed for '" + path + "':\n" + errorLog);
        }

        logger.debug("{} shader compiled successfully (ID: {})", type.name(), shaderId);
        return shaderId;
    }

    /**
     * Charge le code source d'un shader depuis le classpath.
     *
     * @param path chemin relatif depuis /src/main/resources
     * @return le code source du shader
     * @throws RuntimeException si le fichier est introuvable ou illisible
     */
    private String loadShaderSource(String path) {
        StringBuilder source = new StringBuilder();

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(path)) {
            if (inputStream == null) {
                throw new RuntimeException("Shader file not found: " + path);
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    source.append(line).append("\n");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read shader file: " + path, e);
        }

        return source.toString();
    }

    // ==================== Linking ====================

    /**
     * Lie les shaders compilés en un programme shader.
     *
     * @param vertexId   ID du vertex shader
     * @param fragmentId ID du fragment shader
     * @return l'ID du programme shader
     * @throws RuntimeException si le linking échoue
     */
    private int linkProgram(int vertexId, int fragmentId) {
        logger.debug("Linking shader program");

        // Créer le programme
        int programId = glCreateProgram();
        if (programId == 0) {
            throw new RuntimeException("Failed to create shader program");
        }

        // Attacher les shaders
        glAttachShader(programId, vertexId);
        glAttachShader(programId, fragmentId);

        // Linker
        glLinkProgram(programId);

        // Vérifier le linking
        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
            String errorLog = glGetProgramInfoLog(programId);
            glDeleteProgram(programId);
            logger.error("Shader program linking failed: {}", errorLog);
            throw new RuntimeException("Shader program linking failed:\n" + errorLog);
        }

        // Valider le programme
        glValidateProgram(programId);
        if (glGetProgrami(programId, GL_VALIDATE_STATUS) == GL_FALSE) {
            String warningLog = glGetProgramInfoLog(programId);
            logger.warn("Shader program validation warning: {}", warningLog);
        }

        // Détacher les shaders (ils restent compilés mais ne sont plus liés au programme)
        glDetachShader(programId, vertexId);
        glDetachShader(programId, fragmentId);

        logger.debug("Shader program linked successfully (ID: {})", programId);
        return programId;
    }

    // ==================== Utilisation ====================

    /**
     * Active ce shader pour le rendu.
     * <p>
     * À appeler avant de dessiner avec ce shader.
     * </p>
     */
    public void bind() {
        glUseProgram(programId);
    }

    /**
     * Désactive ce shader.
     * <p>
     * À appeler après le rendu pour libérer le pipeline.
     * </p>
     */
    public void unbind() {
        glUseProgram(0);
    }

    // ==================== Gestion des uniforms ====================

    /**
     * Récupère la location d'un uniform (avec cache).
     *
     * @param name le nom de l'uniform dans le shader
     * @return la location, ou -1 si l'uniform n'existe pas
     */
    private int getUniformLocation(String name) {
        // Vérifier le cache d'abord
        if (uniformLocations.containsKey(name)) {
            return uniformLocations.get(name);
        }

        // Récupérer depuis OpenGL
        int location = glGetUniformLocation(programId, name);

        if (location == -1) {
            logger.warn("Uniform '{}' not found in shader program {}", name, programId);
        }

        // Mettre en cache
        uniformLocations.put(name, location);
        return location;
    }

    /**
     * Définit un uniform int.
     *
     * @param name  nom de l'uniform
     * @param value valeur à envoyer
     */
    public void setUniform(String name, int value) {
        glUniform1i(getUniformLocation(name), value);
    }

    /**
     * Définit un uniform float.
     *
     * @param name  nom de l'uniform
     * @param value valeur à envoyer
     */
    public void setUniform(String name, float value) {
        glUniform1f(getUniformLocation(name), value);
    }

    /**
     * Définit un uniform Vector3f.
     *
     * @param name  nom de l'uniform
     * @param value vecteur à envoyer
     */
    public void setUniform(String name, Vector3f value) {
        glUniform3f(getUniformLocation(name), value.x, value.y, value.z);
    }

    /**
     * Définit un uniform Matrix4f.
     *
     * @param name   nom de l'uniform
     * @param matrix matrice à envoyer
     */
    public void setUniform(String name, Matrix4f matrix) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(16);
            matrix.get(buffer);
            glUniformMatrix4fv(getUniformLocation(name), false, buffer);
        }
    }

    // ==================== Nettoyage ====================

    /**
     * Libère toutes les ressources OpenGL associées.
     * <p>
     * À appeler avant la fermeture de l'application.
     * </p>
     */
    public void cleanup() {
        logger.debug("Cleaning up shader program (ID: {})", programId);

        unbind();

        if (programId != 0) {
            glDeleteProgram(programId);
        }

        if (vertexShaderId != 0) {
            glDeleteShader(vertexShaderId);
        }

        if (fragmentShaderId != 0) {
            glDeleteShader(fragmentShaderId);
        }

        logger.debug("Shader program cleaned up");
    }

    // ==================== Getters ====================

    /**
     * Retourne l'ID du programme shader OpenGL.
     *
     * @return l'ID du programme
     */
    public int getProgramId() {
        return programId;
    }
}