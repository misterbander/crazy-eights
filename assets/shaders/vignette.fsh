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
	// Normalize
	vec2 uv = gl_FragCoord.xy/u_resolution.xy;
	uv*=1.0 - uv.xy;   //vec2(1.0)- uv.yx; -> 1.-u.yx; Thanks FabriceNeyret !

	float vig = uv.x*uv.y*15.0; // multiply with sth for intensity
	vig = mix(0.7, 1, pow(vig, 0.25)); // change pow for modifying the extend of the  vignette

	gl_FragColor = v_color*texture2D(u_texture, v_texCoord0)*vec4(vig, vig, vig, 1);
}
