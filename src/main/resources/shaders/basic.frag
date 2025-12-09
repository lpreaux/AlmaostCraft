#version 410 core

// ==================== Input ====================
// Couleur interpolée depuis le vertex shader
in vec3 vColor;

// ==================== Output ====================
// Couleur finale du pixel
out vec4 FragColor;

// ==================== Main ====================
void main() {
    // Simplement afficher la couleur reçue
    FragColor = vec4(vColor, 1.0);
}
