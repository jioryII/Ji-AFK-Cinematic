#version 330
#extension GL_ARB_separate_shader_objects : require
layout(std140) uniform FocusConfig {
    mat4 InverseProjection;
    vec4 Focus; // distance, protected half-width, max blur radius in pixels, fade
    vec4 Screen; // inverse width, inverse height, zero-to-one NDC, vignette mode
};
uniform sampler2D SceneSampler;
uniform sampler2D DepthSampler;
uniform sampler2D BlurSampler;

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

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

void main() {
    vec4 original = texture(SceneSampler, texCoord);
    if (Screen.w > 0.5) {
        float vignette = edgeVignette(texCoord);
        float intensity = min(0.38, 0.10 + Focus.z * 0.06);
        vec3 blurred = toDisplay(texture(BlurSampler, texCoord).rgb);
        fragColor = vec4(mix(original.rgb, blurred, vignette * intensity * Focus.w), original.a);
        return;
    }
    float depth = viewDepth(texCoord);
    float coc = abs(circleOfConfusion(depth));
    float blend = smoothstep(0.5, 2.0, coc) * Focus.w;
    if (blend <= 0.0) { fragColor = original; return; }
    // Bilateral upsampling: retain full-resolution detail at subject silhouettes.
    vec2 size = vec2(textureSize(BlurSampler, 0));
    vec2 pixel = texCoord * size - 0.5;
    ivec2 base = ivec2(floor(pixel));
    vec2 fraction = fract(pixel);
    vec3 sum = vec3(0.0);
    float total = 0.0;
    for (int y = 0; y < 2; y++) for (int x = 0; x < 2; x++) {
        ivec2 p = clamp(base + ivec2(x, y), ivec2(0), ivec2(size) - 1);
        vec4 sampleColor = texelFetch(BlurSampler, p, 0);
        float spatial = (x == 0 ? 1.0 - fraction.x : fraction.x) * (y == 0 ? 1.0 - fraction.y : fraction.y);
        float edge = exp(-abs(sampleColor.a - depth) / max(0.08, depth * 0.025));
        float w = spatial * edge;
        sum += sampleColor.rgb * w; total += w;
    }
    vec3 blurred = total > 0.0001 ? toDisplay(sum / total) : original.rgb;
    fragColor = vec4(mix(original.rgb, blurred, blend), original.a);
}
