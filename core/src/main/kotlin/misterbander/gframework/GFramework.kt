package misterbander.gframework

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import ktx.app.KtxGame
import ktx.app.KtxScreen
import ktx.assets.async.AssetStorage
import ktx.async.newAsyncContext
import ktx.freetype.async.registerFreeTypeFontLoaders
import space.earlygrey.shapedrawer.ShapeDrawer

/**
 * Framework built on top of KTX designed to be simple yet flexible.
 *
 * `GFramework` serves to be the main game class. It stores references to a [PolygonSpriteBatch], a [ShapeRenderer], a
 * [ShapeDrawer], and an [AssetStorage].
 *
 * To start, create a class that extends `GFramework` and override the [create] method. Load your resources with
 * [AssetStorage], add some [GScreen]s and set your first screen.
 *
 * @author Mister_Bander
 */
abstract class GFramework : KtxGame<KtxScreen>(clearScreen = false)
{
	val batch = PolygonSpriteBatch()
	val shapeRenderer = ShapeRenderer()
	val shapeDrawer: ShapeDrawer
	val assetStorage = AssetStorage(newAsyncContext(Runtime.getRuntime().availableProcessors(), "AssetStorage-Thread")).apply {
		registerFreeTypeFontLoaders()
	}
	
	private var deltaAccumulator = 0F
	var fixedUpdateCount = 0
		private set
	
	init
	{
		val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888).apply { setColor(Color.WHITE); fill() }
		val texture = Texture(pixmap)
		texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
		val region = TextureRegion(texture)
		pixmap.dispose()
		shapeDrawer = ShapeDrawer(batch, region)
	}
	
	override fun render()
	{
		deltaAccumulator += Gdx.graphics.deltaTime
		fixedUpdateCount = 0
		while (deltaAccumulator >= 1/60F)
		{
			deltaAccumulator -= 1/60F
			fixedUpdateCount++
		}
		super.render()
	}
	
	override fun dispose()
	{
		batch.dispose()
		shapeRenderer.dispose()
		assetStorage.dispose()
		super.dispose()
	}
}
