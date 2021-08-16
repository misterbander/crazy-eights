#ifdef GL_ES
#define LOWP lowp
precision mediump float;
#else
#define LOWP
#endif

varying vec2 v_texCoord0;
varying LOWP vec4 v_color;

uniform sampler2D u_texture;
uniform vec2 u_resolution;

void main()
{
	vec4 white = vec4(1.0, 1.0, 1.0, 0.5);
	vec4 textureColor = v_color*texture2D(u_texture, v_texCoord0);
	gl_FragColor = mix(textureColor, white, 0.4);
	gl_FragColor.a = textureColor.a;
}
