package misterbander.sandboxtabletop

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.math.MathUtils
import com.esotericsoftware.kryonet.Client
import com.esotericsoftware.kryonet.Server
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
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
import misterbander.sandboxtabletop.net.Network

/**
 * [com.badlogic.gdx.ApplicationListener] implementation shared by all platforms.
 */
class SandboxTabletop : GFramework()
{
	// Fonts
	private val generator by lazy { assetStorage[Fonts.msjhl] }
	val jhengheiui by lazy {
		generator.generateFont {
			size = 40
			minFilter = Texture.TextureFilter.Linear
			magFilter = Texture.TextureFilter.Linear
		}
	}
	val jhengheiuis by lazy {
		generator.generateFont {
			size = 25
			minFilter = Texture.TextureFilter.Linear
			magFilter = Texture.TextureFilter.Linear
		}
	}
	val jhengheiuixs by lazy {
		generator.generateFont {
			size = 15
			minFilter = Texture.TextureFilter.Linear
			magFilter = Texture.TextureFilter.Linear
		}
	}
	val jhengheiuil by lazy {
		generator.generateFont {
			size = 50
			minFilter = Texture.TextureFilter.Linear
			magFilter = Texture.TextureFilter.Linear
		}
	}
	
	// Skins
	private val skin by lazy {
		skin {
			addRegions(assetStorage[TextureAtlases.gui])
			label(INFO_LABEL_STYLE_S) { font = jhengheiuis; fontColor = Color.WHITE }
			label(INFO_LABEL_STYLE_XS) { font = jhengheiuixs; fontColor = Color.WHITE }
			label(CHAT_LABEL_STYLE, INFO_LABEL_STYLE_S) {
				background = this@skin.newDrawable("chatbackground")
				background.topHeight = 4F
				background.leftWidth = 16F
				background.rightWidth = 16F
				background.bottomHeight = 4F
			}
			label(PLAYER_NAMETAG_LABEL_STYLE) {
				font = jhengheiuixs
				fontColor = Color.WHITE
				background = this@skin.newDrawable("chatbackground")
				background.topHeight = 2F
				background.leftWidth = 12F
				background.rightWidth = 12F
				background.bottomHeight = 2F
			}
			window(WINDOW_STYLE) {
				background = this@skin["window"]
				titleFont = jhengheiuis
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
				font = jhengheiuis
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
	
	var network: Network? = null
	val server: Server?
		get() = network?.server
	val client: Client?
		get() = network?.client
	var stopNetworkJob: Deferred<Unit>? = null
		private set
	
	override fun create()
	{
		Gdx.app.logLevel = Application.LOG_DEBUG
//		Log.set(Log.LEVEL_DEBUG)
		KtxAsync.initiate()
		
		KtxAsync.launch {
			val assets = listOf(
				assetStorage.loadAsync(TextureAtlases.gui),
				assetStorage.loadAsync(Textures.title),
				assetStorage.loadAsync(Fonts.msjhl),
				assetStorage.loadAsync(Sounds.click),
				assetStorage.loadAsync(Shaders.brighten),
				assetStorage.loadAsync(Shaders.vignette)
			)
			assets.joinAll()
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
			
			addScreen(MainMenu(this@SandboxTabletop))
			addScreen(Room(this@SandboxTabletop))
			setScreen<MainMenu>()
		}
	}
	
	fun savePreferences()
	{
		val preferences = Gdx.app.getPreferences("misterbander.sandboxtabletop")
		preferences["username"] = user.username
		preferences["color"] = user.color.toString()
		preferences.flush()
	}
	
	fun stopNetwork()
	{
		KtxAsync.launch {
			stopNetworkJob = network?.stopAsync()
			network = null
			stopNetworkJob?.await()
			stopNetworkJob = null
			info("SandboxTabletop | INFO") { "Network stopped" }
		}
	}
}
