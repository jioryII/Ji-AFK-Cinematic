#version 150
uniform mat4 InverseProjection;
uniform vec4 Focus;
uniform vec4 Screen;
uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;

float viewDepth(vec2 uv) {
    float d = texture(DepthSampler, clamp(uv, vec2(0.0), vec2(1.0))).r;
    // 26.2 uses reversed Z on both backends. The actual projection owns the convention.
    float ndc = Screen.z > 0.5 ? d : d * 2.0 - 1.0;
    vec4 p = InverseProjection * vec4(uv * 2.0 - 1.0, ndc, 1.0);
    return abs(p.w) < 0.000001 ? 10000.0 : clamp(abs(p.z / p.w), 0.05, 10000.0);
}
float circleOfConfusion(float depth) {
    float outside = max(0.0, abs(depth - Focus.x) - Focus.y);
    return sign(depth - Focus.x) * min(1.0, outside / max(depth, 0.1)) * Focus.z;
}
vec3 toLinear(vec3 color) { return sign(color) * pow(abs(color), vec3(2.2)); }
vec3 toDisplay(vec3 color) { return sign(color) * pow(abs(color), vec3(1.0 / 2.2)); }
float edgeVignette(vec2 uv) {
    vec2 edge = abs(uv * 2.0 - 1.0);
    return smoothstep(0.62, 1.0, max(edge.x, edge.y));
}

in vec2 texCoord;
out vec4 fragColor;

void main() {
    if (Screen.w > 0.5) {
        float radius = Focus.z * edgeVignette(texCoord);
        vec3 sum = toLinear(texture(DiffuseSampler, texCoord).rgb);
        for (int i = 0; i < 32; i++) {
            float angle = float(i) * 2.39996323;
            float r = sqrt((float(i) + 0.5) / 32.0) * radius;
            vec2 uv = clamp(texCoord + vec2(cos(angle), sin(angle)) * r * Screen.xy,
                            Screen.xy * 0.5, vec2(1.0) - Screen.xy * 0.5);
            sum += toLinear(texture(DiffuseSampler, uv).rgb);
        }
        fragColor = vec4(sum / 33.0, 1.0);
        return;
    }
    float centerDepth = viewDepth(texCoord);
    float coc = circleOfConfusion(centerDepth);
    float radius = abs(coc);
    vec3 sum = toLinear(texture(DiffuseSampler, texCoord).rgb);
    float weight = 1.0;
    if (radius > 0.5) {
        // Fixed Vogel disk: no temporal noise or sample count changes between levels.
        for (int i = 0; i < 32; i++) {
            float angle = float(i) * 2.39996323;
            float r = sqrt((float(i) + 0.5) / 32.0) * radius;
            vec2 uv = clamp(texCoord + vec2(cos(angle), sin(angle)) * r * Screen.xy,
                            Screen.xy * 0.5, vec2(1.0) - Screen.xy * 0.5);
            float sampleDepth = viewDepth(uv);
            float sampleCoc = circleOfConfusion(sampleDepth);
            // Keep near and far layers separate. Sharp subjects never bleed into a blurred background.
            float sameLayer = coc >= 0.0 ? step(Focus.x + Focus.y, sampleDepth)
                                         : 1.0 - step(Focus.x - Focus.y, sampleDepth);
            float edge = smoothstep(0.0, 1.0, abs(sampleCoc));
            float w = sameLayer * edge;
            sum += toLinear(texture(DiffuseSampler, uv).rgb) * w;
            weight += w;
        }
    }
    fragColor = vec4(sum / weight, clamp(log2(1.0 + centerDepth) / 14.0, 0.0, 1.0));
}
