#version 410 core

// ==================== Input ====================
// Location 0 : Position du vertex (x, y, z)
layout(location = 0) in vec3 aPosition;

// ==================== Uniforms ====================
// Matrice Model-View-Projection (transforme world space → clip space)
uniform mat4 uMVP;

// ==================== Output ====================
// Couleur interpolée pour le fragment shader
out vec3 vColor;

// ==================== Main ====================
void main() {
    // Transformer la position du vertex
    gl_Position = uMVP * vec4(aPosition, 1.0);
    
    // Pour le debug : colorer selon la position Y (plus haut = plus clair)
    // Tu pourras remplacer ça par des vraies couleurs/textures plus tard
    float heightFactor = (aPosition.y + 10.0) / 80.0; // Normaliser Y entre 0 et 1
    vColor = vec3(0.3 + heightFactor * 0.5, 0.6 + heightFactor * 0.2, 0.2);
}
