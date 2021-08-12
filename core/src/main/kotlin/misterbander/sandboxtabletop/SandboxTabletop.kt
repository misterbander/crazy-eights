package misterbander.sandboxtabletop

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.esotericsoftware.minlog.Log
import ktx.assets.getValue
import ktx.assets.load
import ktx.async.KtxAsync
import ktx.freetype.generateFont
import ktx.log.info
import ktx.preferences.get
import ktx.preferences.set
import ktx.style.*
import misterbander.gframework.GFramework
import misterbander.gframework.scene2d.mbTextField
import misterbander.sandboxtabletop.model.User
import java.util.UUID
import kotlin.random.Random

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
			label(INFO_LABEL_STYLE) { font = jhengheiuiMini; fontColor = Color.WHITE }
			label(CHAT_LABEL_STYLE, INFO_LABEL_STYLE) {
				background = this@skin.newDrawable("chatbackground")
				background.topHeight = 4F
				background.leftWidth = 16F
				background.rightWidth = 16F
				background.bottomHeight = 4F
			}
			label(PLAYER_NAMETAG_LABEL_STYLE) {
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
			val mbTextFieldStyleBase = "mbtextfieldstylebase"
			mbTextField(mbTextFieldStyleBase) {
				font = jhengheiuiMini
				fontColor = Color.WHITE
				messageFontColor = Color.GRAY
				focusedFontColor = Color.WHITE
				cursor = this@skin["textcursor"]
				selection = this@skin["textselection"]
				disabledFontColor = Color(0xAAAAAAFF.toInt())
			}
			mbTextField(CHAT_TEXT_FIELD_STYLE, mbTextFieldStyleBase) {
				background = this@skin.newDrawable("chatbackground")
				background.topHeight = 16F
				background.leftWidth = 16F
				background.rightWidth = 16F
				background.bottomHeight = 16F
			}
			mbTextField(FORM_TEXT_FIELD_STYLE, mbTextFieldStyleBase) {
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
	val userColor = Color(0F, 0F, 0F, 1F)
	
	override fun create()
	{
		KtxAsync.initiate()
		Gdx.app.logLevel = Application.LOG_DEBUG
		assetManager.load("textures/logo.png", Texture::class.java)
		assetManager.load("sounds/click.wav", Sound::class.java)
		assetManager.finishLoading()
		
		Log.set(Log.LEVEL_DEBUG)
		info("SandboxTabletop | INFO") { "Finished loading assets!" }
		
		// Load settings
		val preferences = Gdx.app.getPreferences("misterbander.sandboxtabletop")
		val uuidStr: String? = preferences["uuid"]
		val uuid = if (uuidStr != null) UUID.fromString(uuidStr) else UUID.randomUUID()
		val username: String = preferences["username", ""]
		val userColorStr: String? = preferences["color"]
		
		user = User(uuid, username)
		if (userColorStr != null)
			Color.valueOf(userColorStr, userColor)
		else
		{
			val random = Random(uuid.hashCode().toLong())
			userColor.fromHsv(random.nextFloat()*360, 0.8F, 0.8F)
		}
		savePreferences()
		
		addScreen(MenuScreen(this))
		addScreen(RoomScreen(this))
		setScreen<MenuScreen>()
	}
	
	fun savePreferences()
	{
		val preferences = Gdx.app.getPreferences("misterbander.sandboxtabletop")
		preferences["uuid"] = user.uuid.toString()
		preferences["username"] = user.username
		preferences["color"] = userColor.toString()
		preferences.flush()
	}
}
