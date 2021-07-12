package misterbander.gframework

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import ktx.app.KtxGame
import ktx.app.KtxScreen
import ktx.freetype.registerFreeTypeFontLoaders
import space.earlygrey.shapedrawer.ShapeDrawer

/**
 * Framework built on top of LibKTX.
 *
 * `GFramework` is the central spine, it contains references to the [SpriteBatch], [ShapeRenderer], [ShapeDrawer],
 * default [BitmapFont] and an [AssetManager].
 *
 * To start, create a class that extends `GFramework` and override the `create()` method. Load your resources with
 * `AssetManager`, add some [GScreen]s and set your first screen.
 *
 * @author Mister_Bander
 */
abstract class GFramework : KtxGame<KtxScreen>(clearScreen = false)
{
	val batch by lazy { PolygonSpriteBatch() }
	val shapeRenderer by lazy { ShapeRenderer() }
	val shapeDrawer by lazy {
		val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
		pixmap.setColor(Color.WHITE)
		pixmap.fill()
		val texture = Texture(pixmap)
		texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
		val region = TextureRegion(texture)
		pixmap.dispose()
		ShapeDrawer(batch, region)
	}
	val assetManager by lazy {
		val assetManager = AssetManager()
		assetManager.registerFreeTypeFontLoaders()
		assetManager
	}
	
	override fun dispose()
	{
		batch.dispose()
		shapeRenderer.dispose()
		assetManager.dispose()
		super.dispose()
	}
}
