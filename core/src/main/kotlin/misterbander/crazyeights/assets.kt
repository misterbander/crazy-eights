package misterbander.crazyeights

import com.badlogic.gdx.assets.loaders.ShaderProgramLoader
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import ktx.assets.assetDescriptor

object TextureAtlases
{
	val gui = assetDescriptor<TextureAtlas>("textures/gui.atlas")
}

object Fonts
{
	val notoSansSc = assetDescriptor<FreeTypeFontGenerator>("fonts/noto_sans_sc_light.otf")
	val montserrat = assetDescriptor<FreeTypeFontGenerator>("fonts/montserrat_bold.ttf")
}

object Sounds
{
	val click = assetDescriptor<Sound>("sounds/click.wav")
	val cardSlide = assetDescriptor<Sound>("sounds/cardslide.wav")
	val dramatic = assetDescriptor<Sound>("sounds/dramatic.wav")
	val deepwhoosh = assetDescriptor<Sound>("sounds/deepwhoosh.wav")
}

object Shaders
{
	val brighten = assetDescriptor<ShaderProgram>("brighten", ShaderProgramLoader.ShaderProgramParameter().apply {
		vertexFile = "shaders/passthrough.vsh"
		fragmentFile = "shaders/brighten.fsh"
	})
	val vignette = assetDescriptor<ShaderProgram>("vignette", ShaderProgramLoader.ShaderProgramParameter().apply {
		vertexFile = "shaders/passthrough.vsh"
		fragmentFile = "shaders/vignette.fsh"
	})
}
