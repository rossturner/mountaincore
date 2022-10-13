#ifdef GL_ES
    precision mediump float;
#endif

varying vec2 v_position;
uniform sampler2D u_texture;
uniform vec2 u_viewportResolution;

const float numSteps = 4.0;

void main() {
    vec2 correctedCoords = vec2(
        (v_position.x + 1.0) / 2.0,
        (v_position.y + 1.0) / 2.0
    );

    // TODO make this not maintain the viewport aspect ratio
    float xStep = 1.0 / u_viewportResolution.x;
    float yStep = 1.0 / u_viewportResolution.y;

    vec4 maxSample = vec4(0, 0, 0, 0);

    float radius2 = (numSteps/2.0) * (numSteps/2.0);

    for (float x = -numSteps/2.0; x < numSteps/2.0; x++) {
        for (float y = -numSteps/2.0; y < numSteps/2.0; y++) {
            vec2 samplePosition = vec2(
                correctedCoords.x + (xStep * x),
                correctedCoords.y + (yStep * y)
            );

            if ((x * x) + (y * y) < radius2) {
                maxSample = max(maxSample, texture2D(u_texture, samplePosition));
            }
        }
    }

    gl_FragColor = maxSample;
}
