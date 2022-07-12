package misterbander.crazyeights

import com.badlogic.gdx.assets.loaders.ShaderProgramLoader
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import ktx.assets.assetDescriptor

object TextureAtlases
{
	val gui = assetDescriptor<TextureAtlas>("textures/gui.atlas")
}

object Textures
{
	val title = assetDescriptor<Texture>("textures/title.png")
}

object Fonts
{
	val msJhengHeiUi = assetDescriptor<FreeTypeFontGenerator>("fonts/ms_jhenghei_ui_light.ttc")
	val twCenMt = assetDescriptor<FreeTypeFontGenerator>("fonts/tw_cen_mt_bold.ttf")
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
