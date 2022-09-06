#ifdef GL_ES
    precision mediump float;
#endif

varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform vec3 u_colour;

void main() {
    gl_FragColor = vec4(u_colour, texture2D(u_texture, v_texCoords).a);
}
