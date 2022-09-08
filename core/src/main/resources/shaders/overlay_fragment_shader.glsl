#ifdef GL_ES
    precision mediump float;
#endif

varying vec2 v_position;
uniform sampler2D u_textureOverlay;
uniform sampler2D u_textureToSubtract;

void main() {
    vec2 correctedCoords = vec2(
        (v_position.x + 1.0) / 2.0,
        (v_position.y + 1.0) / 2.0
    );

    vec4 overlayColor = texture2D(u_textureOverlay, correctedCoords);
    vec4 subtractColor = texture2D(u_textureToSubtract, correctedCoords);

    vec4 combined = overlayColor - subtractColor;

    gl_FragColor = combined;
}
