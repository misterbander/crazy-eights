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
	val msjhl = assetDescriptor<FreeTypeFontGenerator>("fonts/msjhl.ttc")
}

object Sounds
{
	val click = assetDescriptor<Sound>("sounds/click.wav")
	val cardSlide = assetDescriptor<Sound>("sounds/cardslide.wav")
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
