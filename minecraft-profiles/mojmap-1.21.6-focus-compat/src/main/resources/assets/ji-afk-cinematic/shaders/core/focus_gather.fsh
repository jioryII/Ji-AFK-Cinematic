#version 330
#moj_import <ji-afk-cinematic:focus.glsl>
in vec2 texCoord;
out vec4 fragColor;

void main() {
    if (Screen.w > 0.5) {
        float radius = Focus.z * edgeVignette(texCoord);
        vec3 sum = toLinear(texture(SceneSampler, texCoord).rgb);
        for (int i = 0; i < 32; i++) {
            float angle = float(i) * 2.39996323;
            float r = sqrt((float(i) + 0.5) / 32.0) * radius;
            vec2 uv = clamp(texCoord + vec2(cos(angle), sin(angle)) * r * Screen.xy,
                            Screen.xy * 0.5, vec2(1.0) - Screen.xy * 0.5);
            sum += toLinear(texture(SceneSampler, uv).rgb);
        }
        fragColor = vec4(sum / 33.0, 1.0);
        return;
    }
    float centerDepth = viewDepth(texCoord);
    float coc = circleOfConfusion(centerDepth);
    float radius = abs(coc);
    vec3 sum = toLinear(texture(SceneSampler, texCoord).rgb);
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
            sum += toLinear(texture(SceneSampler, uv).rgb) * w;
            weight += w;
        }
    }
    fragColor = vec4(sum / weight, clamp(log2(1.0 + centerDepth) / 14.0, 0.0, 1.0));
}
