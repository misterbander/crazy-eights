package misterbander.gframework

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import ktx.app.KtxGame
import ktx.app.KtxScreen
import ktx.assets.DisposableContainer
import ktx.assets.DisposableRegistry
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
abstract class GFramework(
	private val disposableContainer: DisposableContainer = DisposableContainer()
) : KtxGame<KtxScreen>(clearScreen = false), DisposableRegistry by disposableContainer
{
	val batch = PolygonSpriteBatch()
	val shapeRenderer = ShapeRenderer()
	val shapeDrawer = ShapeDrawer(batch)
	val assetStorage = AssetStorage(newAsyncContext(Runtime.getRuntime().availableProcessors(), "AssetStorage-Thread")).apply {
		registerFreeTypeFontLoaders()
	}
	
	private var deltaAccumulator = 0F
	var fixedUpdateCount = 0
		private set
	
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
		super.dispose()
		batch.dispose()
		shapeRenderer.dispose()
		assetStorage.dispose()
		disposableContainer.dispose()
	}
}
