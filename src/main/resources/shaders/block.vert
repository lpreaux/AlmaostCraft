#version 410 core

// Input
layout(location = 0) in vec3 aPosition;
layout(location = 1) in float aTextureIndex;
layout(location = 2) in vec2 aTexCoord;
layout(location = 3) in vec3 aTintColor;

// Uniforms
uniform mat4 uMVP;

// Output
out float vTextureIndex;
out vec2 vTexCoord;
out vec3 vTintColor;

void main() {
    gl_Position = uMVP * vec4(aPosition, 1.0);
    vTextureIndex = aTextureIndex;
    vTexCoord = aTexCoord;
    vTintColor = aTintColor;
}
