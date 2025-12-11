#version 410 core

// Input
in float vTextureIndex;
in vec2 vTexCoord;
in vec3 vTintColor;

// Uniforms
uniform sampler2DArray uTextureArray;

// Output
out vec4 FragColor;

void main() {
    // Échantillonner la texture array
    vec3 texCoord = vec3(vTexCoord, vTextureIndex);
    vec4 texColor = texture(uTextureArray, texCoord);

    // Appliquer la teinte
    vec3 tintedColor = texColor.rgb * vTintColor;

    FragColor = vec4(tintedColor, texColor.a);
}
