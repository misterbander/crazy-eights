package misterbander.crazyeights

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
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
	private val notoSansScGenerator: FreeTypeFontGenerator
		get() = assetStorage[Fonts.notoSansSc]
	private val montserratGenerator: FreeTypeFontGenerator
		get() = assetStorage[Fonts.montserrat]
	val notoSansSc by lazy {
		notoSansScGenerator.generateFont {
			size = 40
			minFilter = Texture.TextureFilter.Linear
			magFilter = Texture.TextureFilter.Linear
			incremental = true
		}
	}
	val notoSansScSmall by lazy {
		notoSansScGenerator.generateFont {
			size = 25
			minFilter = Texture.TextureFilter.Linear
			magFilter = Texture.TextureFilter.Linear
			incremental = true
		}
	}
	val notoSansScTiny by lazy {
		notoSansScGenerator.generateFont {
			size = 15
			minFilter = Texture.TextureFilter.Linear
			magFilter = Texture.TextureFilter.Linear
			incremental = true
		}
	}
	val notoSansScLarge by lazy {
		notoSansScGenerator.generateFont {
			size = 64
			minFilter = Texture.TextureFilter.Linear
			magFilter = Texture.TextureFilter.Linear
			incremental = true
		}
	}
	val montserrat by lazy {
		montserratGenerator.generateFont {
			size = 128
			minFilter = Texture.TextureFilter.Linear
			magFilter = Texture.TextureFilter.Linear
			incremental = true
		}
	}
	val montserratOutlined by lazy {
		montserratGenerator.generateFont {
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
				assetStorage.loadAsync(Fonts.notoSansSc),
				assetStorage.loadAsync(Fonts.montserrat),
				assetStorage.loadAsync(Sounds.click),
				assetStorage.loadAsync(Sounds.cardSlide),
				assetStorage.loadAsync(Sounds.dramatic),
				assetStorage.loadAsync(Sounds.deepwhoosh),
				assetStorage.loadAsync(Shaders.brighten),
				assetStorage.loadAsync(Shaders.vignette)
			)
			assets.joinAll()
			
			shapeDrawer.setTextureRegion(assetStorage[TextureAtlases.gui].findRegion("pixel"))
			
			Scene2DSkin.defaultSkin = skin {
				addRegions(assetStorage[TextureAtlases.gui])
				add("noto_sans_sc", notoSansSc)
				add("noto_sans_sc_small", notoSansScSmall)
				add("noto_sans_sc_tiny", notoSansScTiny)
				add("noto_sans_sc_large", notoSansScLarge)
				add("montserrat", montserrat)
				add("montserrat_outlined", montserratOutlined)
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
