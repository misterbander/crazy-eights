package misterbander.sandboxtabletop

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import ktx.assets.getValue
import ktx.assets.load
import ktx.freetype.generateFont
import ktx.log.info
import ktx.style.*
import misterbander.gframework.GFramework
import misterbander.gframework.scene2d.mbTextField

val BACKGROUND_COLOR = Color(0x5F1F56FF)
val ACCENT_COLOR = Color(0x7C2870FF)
const val ANIMATION_DURATION = 0.2F

/**
 * [com.badlogic.gdx.ApplicationListener] implementation shared by all platforms.
 */
class SandboxTabletop : GFramework()
{
	// Assets
	
	private val guiAtlas by assetManager.load<TextureAtlas>("textures/gui.atlas")
	
	// Fonts
	private val generator by assetManager.load<FreeTypeFontGenerator>("fonts/msjhl.ttc")
	val jhengheiui by lazy {
		generator.generateFont {
			size = 40
			minFilter = Texture.TextureFilter.Linear
			magFilter = Texture.TextureFilter.Linear
		}
	}
	val jhengheiuiMini by lazy {
		generator.generateFont {
			size = 25
			minFilter = Texture.TextureFilter.Linear
			magFilter = Texture.TextureFilter.Linear
		}
	}
	val jhengheiuiTiny by lazy {
		generator.generateFont {
			size = 15
			minFilter = Texture.TextureFilter.Linear
			magFilter = Texture.TextureFilter.Linear
		}
	}
	val jhengheiuiMax by lazy {
		generator.generateFont {
			size = 50
			minFilter = Texture.TextureFilter.Linear
			magFilter = Texture.TextureFilter.Linear
		}
	}
	
	// Skins
	val skin by lazy {
		skin {
			addRegions(guiAtlas)
			label("infolabelstyle") { font = jhengheiuiMini; fontColor = Color.WHITE }
			label("chatlabelstyle", "infolabelstyle") {
				background = this@skin.newDrawable("chatbackground")
				background.topHeight = 4F
				background.leftWidth = 16F
				background.rightWidth = 16F
				background.bottomHeight = 4F
			}
			label("playernametaglabelstyle") {
				background = this@skin.newDrawable("chatbackground")
				background.topHeight = 2F
				background.leftWidth = 12F
				background.rightWidth = 12F
				background.bottomHeight = 2F
			}
			window("windowstyle") {
				background = this@skin["window"]
				titleFont = jhengheiuiMini
				titleFontColor = Color.WHITE
			}
			scrollPane("scrollpanestyle") {
				background = this@skin.newDrawable("chatbackground")
				vScrollKnob = this@skin["textcursor"]
			}
			textButton("textbuttonstyle") {
				up = this@skin["button"]
				over = this@skin["buttonover"]
				down = this@skin["buttondown"]
				font = jhengheiui
				fontColor = Color.WHITE
				downFontColor = Color.BLACK
			}
			button("closebuttonstyle") {
				up = this@skin["closebutton"]
				over = this@skin["closebuttonover"]
				down = this@skin["closebuttondown"]
			}
			imageButton("imagebuttonstylebase") {
				up = this@skin["button"]
				over = this@skin["buttonover"]
				down = this@skin["buttondown"]
			}
			imageButton("menubuttonstyle", "imagebuttonstylebase") {
				imageUp = this@skin["menuicon"]
				imageDown = this@skin["menuicondown"]
			}
			mbTextField("mbtextfieldstylebase") {
				font = jhengheiuiMini
				fontColor = Color.WHITE
				messageFontColor = Color.GRAY
				focusedFontColor = Color.WHITE
				cursor = this@skin["textcursor"]
				selection = this@skin["textselection"]
				disabledFontColor = Color(0xAAAAAAFF.toInt())
			}
			mbTextField("chattextfieldstyle", "mbtextfieldstylebase") {
				background = this@skin.newDrawable("chatbackground")
				background.topHeight = 16F
				background.leftWidth = 16F
				background.rightWidth = 16F
				background.bottomHeight = 16F
			}
			mbTextField("formtextfieldstyle", "mbtextfieldstylebase") {
				background = this@skin["textfield"]
				focusedBackground = this@skin["textfieldfocused"]
			}
		}
	}
	
	override fun create()
	{
		super.create()
		Gdx.app.logLevel = Application.LOG_DEBUG
		assetManager.load("textures/logo.png", Texture::class.java)
		assetManager.load("sounds/click.wav", Sound::class.java)
		assetManager.finishLoading()
		
		info("SandboxTabletop | INFO") { "Finished loading assets!" }
		
		addScreen(MenuScreen(this))
		setScreen<MenuScreen>()
	}
}