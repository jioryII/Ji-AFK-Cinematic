#version 150
in vec4 Position;
uniform mat4 ProjMat;
uniform vec2 OutSize;
out vec2 texCoord;
void main() {
    vec4 p = ProjMat * vec4(Position.xy, 0.0, 1.0);
    gl_Position = vec4(p.xy, 0.2, 1.0);
    texCoord = Position.xy / OutSize;
}
