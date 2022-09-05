#ifdef GL_ES
    precision mediump float;
#endif

varying vec2 v_position;
uniform sampler2D u_textureDepth;

float step = 0.1;

float intensity(in vec4 color){
    return sqrt((color.x*color.x)+(color.y*color.y)+(color.z*color.z));
}

float sobel(float stepx, float stepy, vec2 center){
    // get samples around pixel
    float tleft = intensity(texture2D(u_textureDepth, center + vec2(-stepx, stepy)));
    float left = intensity(texture2D(u_textureDepth, center + vec2(-stepx, 0)));
    float bleft = intensity(texture2D(u_textureDepth, center + vec2(-stepx, -stepy)));
    float top = intensity(texture2D(u_textureDepth, center + vec2(0, stepy)));
    float bottom = intensity(texture2D(u_textureDepth, center + vec2(0, -stepy)));
    float tright = intensity(texture2D(u_textureDepth, center + vec2(stepx, stepy)));
    float right = intensity(texture2D(u_textureDepth, center + vec2(stepx, 0)));
    float bright = intensity(texture2D(u_textureDepth, center + vec2(stepx, -stepy)));

    float x = tleft + 2.0*left + bleft - tright - 2.0*right - bright;
    float y = -tleft - 2.0*top - tright + bleft + 2.0 * bottom + bright;
    float newAlpha = sqrt((x*x) + (y*y));
    return newAlpha;
}


void main() {
    vec2 correctedCoords = vec2(
        (v_position.x + 1.0) / 2.0,
        (v_position.y + 1.0) / 2.0
    );

    //TODO: uniform for the resolution parameters
    float newAlpha = sobel(step/1440, step/900, correctedCoords);
    gl_FragColor = vec4(texture2D(u_textureDepth, correctedCoords).xyz, newAlpha);
}
