#ifdef GL_ES
    precision mediump float;
#endif

varying vec2 v_position;
uniform sampler2D u_texture;

float step = 0.1;

float intensity(in vec4 color){
    return sqrt((color.x*color.x)+(color.y*color.y)+(color.z*color.z));
}

float sobel(float stepx, float stepy, vec2 center){
    // get samples around pixel
    float tleft = intensity(texture2D(u_texture, center + vec2(-stepx, stepy)));
    float left = intensity(texture2D(u_texture, center + vec2(-stepx, 0)));
    float bleft = intensity(texture2D(u_texture, center + vec2(-stepx, -stepy)));
    float top = intensity(texture2D(u_texture, center + vec2(0, stepy)));
    float bottom = intensity(texture2D(u_texture, center + vec2(0, -stepy)));
    float tright = intensity(texture2D(u_texture, center + vec2(stepx, stepy)));
    float right = intensity(texture2D(u_texture, center + vec2(stepx, 0)));
    float bright = intensity(texture2D(u_texture, center + vec2(stepx, -stepy)));

    float newAlpha = (tleft + top + tright + left + right + bleft + bottom + right)/9.0;
    return newAlpha;
}


void main() {
    vec2 correctedCoords = vec2(
        (v_position.x + 1.0) / 2.0,
        (v_position.y + 1.0) / 2.0
    );

    float newAlpha = sobel(step/1440, step/900, correctedCoords);
    gl_FragColor = vec4(newAlpha, newAlpha, newAlpha, texture2D(u_texture, correctedCoords).a);
}
