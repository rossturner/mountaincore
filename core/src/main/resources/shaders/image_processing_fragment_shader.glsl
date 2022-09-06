#ifdef GL_ES
    precision mediump float;
#endif

varying vec2 v_position;
uniform sampler2D u_texture;
uniform vec2 u_viewportResolution;
uniform mat3 u_kernelX;
uniform mat3 u_kernelY;

vec4 convolution(float stepx, float stepy, vec2 center){
    // get samples around pixel
    vec4 tleft = texture2D(u_texture, center + vec2(-stepx, stepy));
    vec4 left = texture2D(u_texture, center + vec2(-stepx, 0));
    vec4 bleft = texture2D(u_texture, center + vec2(-stepx, -stepy));

    vec4 top = texture2D(u_texture, center + vec2(0, stepy));
    vec4 middle = texture2D(u_texture, center);
    vec4 bottom = texture2D(u_texture, center + vec2(0, -stepy));

    vec4 tright = texture2D(u_texture, center + vec2(stepx, stepy));
    vec4 right = texture2D(u_texture, center + vec2(stepx, 0));
    vec4 bright = texture2D(u_texture, center + vec2(stepx, -stepy));

    vec4 outputColour = vec4(0, 0, 0, 0);

    float colourChannels = 0.0;
    for (int i = 0; i < 3; i++) {
        float x = ( u_kernelX[0][0] * tleft[i] +   u_kernelX[1][0] * top[i] +      u_kernelX[2][0] * tright[i] +
                    u_kernelX[0][1] * left[i] +    u_kernelX[1][1] * middle[i] +   u_kernelX[2][1] * right[i] +
                    u_kernelX[0][2] * bleft[i] +   u_kernelX[1][2] * bottom[i] +   u_kernelX[2][2] * right[i]);


        float y = ( u_kernelY[0][0] * tleft[i] +   u_kernelY[1][0] * top[i] +      u_kernelY[2][0] * tright[i] +
                    u_kernelY[0][1] * left[i] +    u_kernelY[1][1] * middle[i] +   u_kernelY[2][1] * right[i] +
                    u_kernelY[0][2] * bleft[i] +   u_kernelY[1][2] * bottom[i] +   u_kernelY[2][2] * right[i]);


        float colourChannel = sqrt((x*x) + (y*y));
        colourChannels += colourChannel;
        outputColour[i] = colourChannel;
    }

    outputColour.a = step(0.5, colourChannels);


    return outputColour;
}


void main() {
    //Rocky suspects that the incoming image lacks an alpha channel, so is just opaque
    vec2 correctedCoords = vec2(
        (v_position.x + 1.0) / 2.0,
        (v_position.y + 1.0) / 2.0
    );

//    float newAlpha = convolution(0.1/u_viewportResolution.x, 0.1/u_viewportResolution.y, correctedCoords);
//    vec3 newColour = texture2D(u_texture, correctedCoords).xyz * newAlpha;
//    gl_FragColor = vec4(texture2D(u_texture, correctedCoords).xyz, newAlpha); //? probably wants a threshold for alpha, if above X then
//    gl_FragColor = vec4(newColour, newAlpha);
    gl_FragColor = convolution(0.2/u_viewportResolution.x, 0.2/u_viewportResolution.y, correctedCoords);
}
