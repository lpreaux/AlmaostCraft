#version 410 core

// Input
layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec2 aTexCoord;
layout(location = 2) in vec3 aTintColor;  // NOUVEAU

// Uniforms
uniform mat4 uMVP;

// Output
out vec2 vTexCoord;
out vec3 vTintColor;  // NOUVEAU

void main() {
    gl_Position = uMVP * vec4(aPosition, 1.0);
    vTexCoord = aTexCoord;
    vTintColor = aTintColor;  // NOUVEAU
}