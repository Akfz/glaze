#version 330 core

in vec2 vTexCoord;
uniform sampler2D uTexture;
out vec4 fragColor;

void main() {
    vec4 color = texture(uTexture, vTexCoord);
    fragColor = color;
}
