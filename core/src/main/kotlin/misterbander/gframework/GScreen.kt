package misterbander.gframework

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.OrderedMap
import com.badlogic.gdx.utils.OrderedSet
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.badlogic.gdx.utils.viewport.Viewport
import ktx.actors.plusAssign
import ktx.app.KtxScreen
import ktx.collections.*
import misterbander.gframework.layer.GLayer
import misterbander.gframework.layer.StageLayer
import misterbander.gframework.layer.TransitionLayer
import misterbander.gframework.scene2d.GObject
import misterbander.gframework.scene2d.KeyboardHeightObserver

/**
 * `GScreen`s are extensions of [KtxScreen]s. By default, it consists of three [GLayer]s: one for the main layer, one
 * for the UI layer, and one for the transition layer. The main [GLayer] and the UI [GLayer] each hold a [Stage] which
 * can be used for the world and UI.
 *
 * Two cameras and two viewports are also defined, one camera and one viewport each used by the main layer and the UI
 * layer. The cameras and viewports can be overridden to use your own camera and/or viewport.
 *
 * You can also override the layers to reorder or add your own [GLayer]s.
 *
 * @property game parent GFramework instance
 */
abstract class GScreen<T : GFramework>(val game: T) : KtxScreen
{
	/** Main camera used by [stage] and projected through [viewport] in the main layer. Defaults to an [OrthographicCamera]. */
	open val camera: Camera = OrthographicCamera()
	/** Secondary camera used by [uiStage] and projected through [uiViewport] in the UI layer. */
	open val uiCamera = OrthographicCamera()
	/** Primary viewport to project [camera] contents in the main layer. Defaults to [ExtendViewport]. */
	open val viewport: Viewport by lazy { ExtendViewport(1280F, 720F, camera) }
	/** Secondary viewport to project [uiCamera] contents in the UI layer. Also defaults to [ExtendViewport]. */
	open val uiViewport by lazy { ExtendViewport(1280F, 720F, uiCamera) }
	
	/** All the [GLayer]s in the [GScreen]. Can be overridden to customize the layers. */
	protected open val layers by lazy { arrayOf(mainLayer, uiLayer, transition) }
	protected open val mainLayer by lazy { StageLayer(game, camera, viewport, false) }
	protected open val uiLayer by lazy { StageLayer(game, uiCamera, uiViewport, true) }
	open val transition by lazy { TransitionLayer(this) }
	
	/** Main stage in the main layer for generic purposes. */
	val stage: Stage
		get() = mainLayer.stage
	/** Secondary stage in the UI layer optimized for UI. */
	val uiStage: Stage
		get() = uiLayer.stage
	
	/**
	 * Convenient set of [KeyboardHeightObserver]s that you can notify soft-keyboard height changes from mobile
	 * platforms.
	 */
	val keyboardHeightObservers = GdxSet<KeyboardHeightObserver>()
	
	val scheduledAddingGObjects = OrderedMap<GObject<*>, Group>()
	val scheduledRemovalGObjects = OrderedSet<GObject<*>>()
	
	private var deltaAccumulator = 0F
	var fixedUpdateCount = 0
		private set
	
	override fun show()
	{
		Gdx.input.inputProcessor = InputMultiplexer(uiStage, stage)
	}
	
	override fun render(delta: Float)
	{
		deltaAccumulator += delta
		fixedUpdateCount = 0
		while (deltaAccumulator >= 1/60F)
		{
			deltaAccumulator -= 1/60F
			fixedUpdateCount++
		}
		
		clearScreen()
		layers.forEach { it.update(delta) }
		layers.forEach { it.render(delta) }
		layers.forEach { it.postRender(delta) }
		for ((gObject, group) in scheduledAddingGObjects)
		{
			if (group != null)
				group += gObject
			else
				stage += gObject
		}
		scheduledAddingGObjects.clear()
		scheduledRemovalGObjects.forEach { it.remove() }
		scheduledRemovalGObjects.clear()
	}
	
	/**
	 * Clears the screen and paints it black. Called once every frame.
	 *
	 * You can override this to change the background color.
	 */
	open fun clearScreen() = ScreenUtils.clear(Color.BLACK, true)
	
	override fun resize(width: Int, height: Int)
	{
		layers.forEach { it.resize(width, height) }
		Gdx.graphics.requestRendering()
	}
	
	/**
	 * Schedules the [GObject] to be added into the world as a child of a specified [Group]. The [GObject] will be added
	 * at the end of the next world time step. Useful during collision callbacks where creation and removal of objects
	 * are prohibited.
	 * @param gObject the [GObject] to spawn
	 * @param parent the parent group to add the [GObject], defaults to [stage] root
	 */
	fun scheduleSpawnGObject(gObject: GObject<*>, parent: Group = stage.root)
	{
		scheduledAddingGObjects[gObject] = parent
	}
	
	override fun dispose() = layers.forEach { it.dispose() }
}
