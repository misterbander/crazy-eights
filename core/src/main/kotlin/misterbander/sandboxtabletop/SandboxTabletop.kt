package misterbander.sandboxtabletop

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.math.MathUtils
import ktx.assets.getValue
import ktx.assets.load
import ktx.async.KtxAsync
import ktx.freetype.generateFont
import ktx.log.info
import ktx.preferences.get
import ktx.preferences.set
import ktx.scene2d.*
import ktx.style.*
import misterbander.gframework.GFramework
import misterbander.gframework.scene2d.gTextField
import misterbander.sandboxtabletop.model.User

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
	private val skin by lazy {
		skin {
			addRegions(guiAtlas)
			label(INFO_LABEL_STYLE) { font = jhengheiuiMini; fontColor = Color.WHITE }
			label(CHAT_LABEL_STYLE, INFO_LABEL_STYLE) {
				background = this@skin.newDrawable("chatbackground")
				background.topHeight = 4F
				background.leftWidth = 16F
				background.rightWidth = 16F
				background.bottomHeight = 4F
			}
			label(PLAYER_NAMETAG_LABEL_STYLE) {
				font = jhengheiuiTiny
				fontColor = Color.WHITE
				background = this@skin.newDrawable("chatbackground")
				background.topHeight = 2F
				background.leftWidth = 12F
				background.rightWidth = 12F
				background.bottomHeight = 2F
			}
			window(WINDOW_STYLE) {
				background = this@skin["window"]
				titleFont = jhengheiuiMini
				titleFontColor = Color.WHITE
			}
			scrollPane(SCROLL_PANE_STYLE) {
				background = this@skin.newDrawable("chatbackground")
				vScrollKnob = this@skin["textcursor"]
			}
			textButton(TEXT_BUTTON_STYLE) {
				up = this@skin["button"]
				over = this@skin["buttonover"]
				down = this@skin["buttondown"]
				font = jhengheiui
				fontColor = Color.WHITE
				downFontColor = Color.BLACK
			}
			button(CLOSE_BUTTON_STYLE) {
				up = this@skin["closebutton"]
				over = this@skin["closebuttonover"]
				down = this@skin["closebuttondown"]
				disabled = this@skin["closebuttondisabled"]
			}
			val imageButtonStyleBase = "imagebuttonstylebase"
			imageButton(imageButtonStyleBase) {
				up = this@skin["button"]
				over = this@skin["buttonover"]
				down = this@skin["buttondown"]
			}
			imageButton(COLOR_BUTTON_STYLE, imageButtonStyleBase) {
				imageUp = this@skin["colorcircle"]
			}
			imageButton(MENU_BUTTON_STYLE, imageButtonStyleBase) {
				imageUp = this@skin["menuicon"]
				imageDown = this@skin["menuicondown"]
			}
			val gTextFieldStyleBase = "mbtextfieldstylebase"
			gTextField(gTextFieldStyleBase) {
				font = jhengheiuiMini
				fontColor = Color.WHITE
				messageFontColor = Color.GRAY
				focusedFontColor = Color.WHITE
				cursor = this@skin["textcursor"]
				selection = this@skin["textselection"]
				disabledFontColor = Color(0xAAAAAAFF.toInt())
			}
			gTextField(CHAT_TEXT_FIELD_STYLE, gTextFieldStyleBase) {
				background = this@skin.newDrawable("chatbackground")
				background.topHeight = 16F
				background.leftWidth = 16F
				background.rightWidth = 16F
				background.bottomHeight = 16F
			}
			gTextField(FORM_TEXT_FIELD_STYLE, gTextFieldStyleBase) {
				background = this@skin["textfield"]
				focusedBackground = this@skin["textfieldfocused"]
			}
			slider(HUE_SLIDER_STYLE) {
				background = this@skin["hueslider"]
				knob = this@skin["huesliderknob"]
				knobOver = this@skin["huesliderknobover"]
				knobDown = this@skin["huesliderknobdown"]
			}
		}
	}
	
	lateinit var user: User
	
	override fun create()
	{
		Gdx.app.logLevel = Application.LOG_DEBUG
//		Log.set(Log.LEVEL_DEBUG)
		KtxAsync.initiate()
		
		assetManager.load("textures/logo.png", Texture::class.java)
		assetManager.load("sounds/click.wav", Sound::class.java)
		assetManager.finishLoading()
		Scene2DSkin.defaultSkin = skin
		
		info("SandboxTabletop | INFO") { "Finished loading assets!" }
		
		// Load settings
		val preferences = Gdx.app.getPreferences("misterbander.sandboxtabletop")
		val username: String = preferences["username", ""]
		val userColorStr: String? = preferences["color"]
		
		user = User(username)
		if (userColorStr != null)
			Color.valueOf(userColorStr, user.color)
		else
			user.color.fromHsv(MathUtils.random()*360, 0.8F, 0.8F)
		savePreferences()
		
		addScreen(MenuScreen(this))
		addScreen(RoomScreen(this))
		setScreen<MenuScreen>()
	}
	
	fun savePreferences()
	{
		val preferences = Gdx.app.getPreferences("misterbander.sandboxtabletop")
		preferences["username"] = user.username
		preferences["color"] = user.color.toString()
		preferences.flush()
	}
}
