#ifdef GL_ES
    precision mediump float;
#endif

varying vec2 v_position;
uniform sampler2D u_texture;
uniform vec2 u_viewportResolution;
uniform mat3 u_kernelX;
uniform mat3 u_kernelY;
uniform float u_step;
uniform bool u_reduceByMax;

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

    //TODO: refactor to be easier to scale up by kernel size
    float averageColour = 0.0;
    for (int i = 0; i < 3; i++) {

        if (u_reduceByMax) {
            float x = 0;
            x = max(u_kernelX[0][0] * tleft[i], x);
            x = max(u_kernelX[0][1] * left[i], x);
            x = max(u_kernelX[0][2] * bleft[i], x);

            x = max(u_kernelX[1][0] * top[i], x);
            x = max(u_kernelX[1][1] * middle[i], x);
            x = max(u_kernelX[1][2] * bottom[i], x);

            x = max(u_kernelX[2][0] * tright[i], x);
            x = max(u_kernelX[2][1] * right[i], x);
            x = max(u_kernelX[2][2] * bright[i], x);

            float y = 0; //todo: implement y

            float colourChannel = sqrt((x*x) + (y*y));

            averageColour += colourChannel / 3.0;
            outputColour[i] = colourChannel;
        } else {
            float x = (
            u_kernelX[0][0] * tleft[i] +   u_kernelX[1][0] * top[i] +      u_kernelX[2][0] * tright[i] +
            u_kernelX[0][1] * left[i] +    u_kernelX[1][1] * middle[i] +   u_kernelX[2][1] * right[i] +
            u_kernelX[0][2] * bleft[i] +   u_kernelX[1][2] * bottom[i] +   u_kernelX[2][2] * bright[i]);


            float y = (
            u_kernelY[0][0] * tleft[i] +   u_kernelY[1][0] * top[i] +      u_kernelY[2][0] * tright[i] +
            u_kernelY[0][1] * left[i] +    u_kernelY[1][1] * middle[i] +   u_kernelY[2][1] * right[i] +
            u_kernelY[0][2] * bleft[i] +   u_kernelY[1][2] * bottom[i] +   u_kernelY[2][2] * bright[i]);


            float colourChannel = sqrt((x*x) + (y*y));

            averageColour += colourChannel / 3.0;
            outputColour[i] = colourChannel;
        }
    }

    outputColour.a = mix(0.0, 1, averageColour);


    return outputColour;
}


void main() {
    vec2 correctedCoords = vec2(
        (v_position.x + 1.0) / 2.0,
        (v_position.y + 1.0) / 2.0
    );

    gl_FragColor = convolution(u_step/u_viewportResolution.x, u_step/u_viewportResolution.y, correctedCoords);
}
