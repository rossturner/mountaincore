#ifdef GL_ES
    precision mediump float;
#endif

varying vec2 v_position;
uniform sampler2D u_texture;
uniform vec2 u_viewportResolution;
uniform mat3 u_kernelX;
uniform mat3 u_kernelY;

float intensity(in vec4 color){
    return sqrt((color.x*color.x)+(color.y*color.y)+(color.z*color.z));
}

float convolution(float stepx, float stepy, vec2 center){
    // get samples around pixel
    float tleft = intensity(texture2D(u_texture, center + vec2(-stepx, stepy)));
    float left = intensity(texture2D(u_texture, center + vec2(-stepx, 0)));
    float bleft = intensity(texture2D(u_texture, center + vec2(-stepx, -stepy)));

    float top = intensity(texture2D(u_texture, center + vec2(0, stepy)));
    float middle = intensity(texture2D(u_texture, center));
    float bottom = intensity(texture2D(u_texture, center + vec2(0, -stepy)));

    float tright = intensity(texture2D(u_texture, center + vec2(stepx, stepy)));
    float right = intensity(texture2D(u_texture, center + vec2(stepx, 0)));
    float bright = intensity(texture2D(u_texture, center + vec2(stepx, -stepy)));

    float x = ( u_kernelX[0][0] * tleft +   u_kernelX[1][0] * top +      u_kernelX[2][0] * tright +
                u_kernelX[0][1] * left +    u_kernelX[1][1] * middle +   u_kernelX[2][1] * right +
                u_kernelX[0][2] * bleft +   u_kernelX[1][2] * bottom +   u_kernelX[2][2] * right);


    float y = ( u_kernelY[0][0] * tleft +   u_kernelY[1][0] * top +      u_kernelY[2][0] * tright +
                u_kernelY[0][1] * left +    u_kernelY[1][1] * middle +   u_kernelY[2][1] * right +
                u_kernelY[0][2] * bleft +   u_kernelY[1][2] * bottom +   u_kernelY[2][2] * right);

    return sqrt((x*x) + (y*y));
}


void main() {
    //Rocky suspects that the incoming image lacks an alpha channel, so is just opaque
    vec2 correctedCoords = vec2(
        (v_position.x + 1.0) / 2.0,
        (v_position.y + 1.0) / 2.0
    );

    float newAlpha = convolution(0.1/u_viewportResolution.x, 0.1/u_viewportResolution.y, correctedCoords);
    vec3 newColour = texture2D(u_texture, correctedCoords).xyz * newAlpha;
//    gl_FragColor = vec4(texture2D(u_texture, correctedCoords).xyz, newAlpha); //? probably wants a threshold for alpha, if above X then
    gl_FragColor = vec4(newColour, newAlpha);
}
