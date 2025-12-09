#version 410 core

// ==================== Input ====================
layout(location = 0) in vec3 aPosition; // Position (x, y, z)
layout(location = 1) in vec3 aColor;    // Couleur RGB

// ==================== Uniforms ====================
uniform mat4 uMVP; // Model-View-Projection

// ==================== Output ====================
out vec3 vColor; // Couleur interpolée vers le fragment shader

// ==================== Main ====================
void main() {
    // Transformer la position
    gl_Position = uMVP * vec4(aPosition, 1.0);
    
    // Passer la couleur au fragment shader
    vColor = aColor;
}
