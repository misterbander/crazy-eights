package misterbander.crazyeights

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.math.MathUtils
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import ktx.async.KtxAsync
import ktx.freetype.generateFont
import ktx.log.info
import ktx.preferences.get
import ktx.preferences.set
import ktx.scene2d.*
import ktx.style.*
import misterbander.crazyeights.model.User
import misterbander.crazyeights.net.CrazyEightsClient
import misterbander.crazyeights.net.CrazyEightsServer
import misterbander.crazyeights.net.Network
import misterbander.gframework.GFramework

/**
 * [com.badlogic.gdx.ApplicationListener] implementation shared by all platforms.
 */
class CrazyEights(private val args: Array<String> = emptyArray()) : GFramework()
{
	// Fonts
	private val msJhengHeiUiGenerator by lazy { assetStorage[Fonts.msJhengHeiUi] }
	private val twCenMtGenerator by lazy { assetStorage[Fonts.twCenMt] }
	val msJhengHeiUi by lazy {
		msJhengHeiUiGenerator.generateFont {
			size = 40
			minFilter = Texture.TextureFilter.Linear
			magFilter = Texture.TextureFilter.Linear
			incremental = true
		}
	}
	val msJhengHeiUiSmall by lazy {
		msJhengHeiUiGenerator.generateFont {
			size = 25
			minFilter = Texture.TextureFilter.Linear
			magFilter = Texture.TextureFilter.Linear
			incremental = true
		}
	}
	val msJhengHeiUiTiny by lazy {
		msJhengHeiUiGenerator.generateFont {
			size = 15
			minFilter = Texture.TextureFilter.Linear
			magFilter = Texture.TextureFilter.Linear
			incremental = true
		}
	}
	val msJhengHeiUiLarge by lazy {
		msJhengHeiUiGenerator.generateFont {
			size = 64
			minFilter = Texture.TextureFilter.Linear
			magFilter = Texture.TextureFilter.Linear
			incremental = true
		}
	}
	val twCenMt by lazy {
		twCenMtGenerator.generateFont {
			size = 128
			minFilter = Texture.TextureFilter.Linear
			magFilter = Texture.TextureFilter.Linear
			incremental = true
		}
	}
	val twCenMtOutlined by lazy {
		twCenMtGenerator.generateFont {
			size = 108
			minFilter = Texture.TextureFilter.Linear
			magFilter = Texture.TextureFilter.Linear
			incremental = true
			borderColor = Color.BLACK
			borderWidth = 3F
		}
	}
	
	lateinit var user: User
	
	val network = Network()
	val server: CrazyEightsServer?
		get() = network.server
	val client: CrazyEightsClient?
		get() = network.client
	
	override fun create()
	{
		Gdx.app.logLevel = when (args.firstOrNull())
		{
			"--debug" -> Application.LOG_DEBUG
			"--info" -> Application.LOG_INFO
			else -> Application.LOG_ERROR
		}
		Gdx.input.setCatchKey(Input.Keys.BACK, true)
//		Log.set(Log.LEVEL_DEBUG)
		KtxAsync.initiate()
		
		KtxAsync.launch {
			val assets = listOf(
				assetStorage.loadAsync(TextureAtlases.gui),
				assetStorage.loadAsync(Textures.title),
				assetStorage.loadAsync(Fonts.msJhengHeiUi),
				assetStorage.loadAsync(Fonts.twCenMt),
				assetStorage.loadAsync(Sounds.click),
				assetStorage.loadAsync(Sounds.cardSlide),
				assetStorage.loadAsync(Sounds.dramatic),
				assetStorage.loadAsync(Sounds.deepwhoosh),
				assetStorage.loadAsync(Shaders.brighten),
				assetStorage.loadAsync(Shaders.vignette)
			)
			assets.joinAll()
			Scene2DSkin.defaultSkin = skin {
				addRegions(assetStorage[TextureAtlases.gui])
				add("ms_jhenghei_ui", msJhengHeiUi)
				add("ms_jhenghei_ui_small", msJhengHeiUiSmall)
				add("ms_jhenghei_ui_tiny", msJhengHeiUiTiny)
				add("ms_jhenghei_ui_large", msJhengHeiUiLarge)
				add("tw_cen_mt", twCenMt)
				add("tw_cen_mt_outlined", twCenMtOutlined)
				load(Gdx.files.internal("textures/skin.json"))
			}
			
			info("CrazyEights | INFO") { "Finished loading assets!" }
			
			// Load settings
			val preferences = Gdx.app.getPreferences("misterbander.crazyeights")
			val username: String = preferences["username", ""]
			val userColorStr: String? = preferences["color"]
			
			user = User(username)
			if (userColorStr != null)
				Color.valueOf(userColorStr, user.color)
			else
				user.color.fromHsv(MathUtils.random()*360, 0.8F, 0.8F)
			savePreferences()
			
			addScreen(MainMenuScreen(this@CrazyEights))
			addScreen(RoomScreen(this@CrazyEights))
			setScreen<MainMenuScreen>()
		}
	}
	
	fun savePreferences()
	{
		val preferences = Gdx.app.getPreferences("misterbander.crazyeights")
		preferences["username"] = user.name
		preferences["color"] = user.color.toString()
		preferences.flush()
	}
}
