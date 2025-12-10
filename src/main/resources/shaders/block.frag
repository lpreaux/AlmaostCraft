#version 410 core

// Input
in vec2 vTexCoord;
in vec3 vTintColor;  // NOUVEAU

// Uniforms
uniform sampler2D uTexture;

// Output
out vec4 FragColor;

void main() {
    // Échantillonner la texture (niveaux de gris pour l'herbe)
    vec4 texColor = texture(uTexture, vTexCoord);

    // Appliquer la teinte (multiplication composante par composante)
    vec3 tintedColor = texColor.rgb * vTintColor;

    FragColor = vec4(tintedColor, texColor.a);
}