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
