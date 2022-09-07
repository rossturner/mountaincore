#ifdef GL_ES
    precision mediump float;
#endif

varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;

void main() {
    vec4 texture_sample = texture2D(u_texture, v_texCoords);
    float averaged = (texture_sample.r + texture_sample.g + texture_sample.b) / 3.0;
    gl_FragColor = vec4(averaged * v_color.r, averaged * v_color.g, averaged * v_color.b, texture_sample.a * v_color.a);
}
