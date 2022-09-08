#ifdef GL_ES
    precision mediump float;
#endif

varying vec2 v_position;
uniform sampler2D u_textureOverlay;
uniform float u_alpha;

void main() {
    vec2 correctedCoords = vec2(
        (v_position.x + 1.0) / 2.0,
        (v_position.y + 1.0) / 2.0
    );

    vec4 overlayColor = texture2D(u_textureOverlay, correctedCoords);

    vec4 combined = overlayColor * vec4(1.0, 1.0, 1.0, u_alpha);

    gl_FragColor = combined;
}
