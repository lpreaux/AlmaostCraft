#version 410 core

// ==================== Input ====================
in vec3 vColor; // Couleur interpolée depuis le vertex shader

// ==================== Output ====================
out vec4 FragColor; // Couleur finale du pixel

// ==================== Main ====================
void main() {
    FragColor = vec4(vColor, 1.0);
}
