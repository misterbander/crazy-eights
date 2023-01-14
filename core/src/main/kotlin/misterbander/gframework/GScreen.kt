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
import misterbander.gframework.scene2d.GObject
import misterbander.gframework.scene2d.KeyboardHeightObserver

/**
 * Extension of [KtxScreen] that supports [GLayer]s, notifying soft-keyboard height changes, and delayed adding and
 * removal of [GObject]s.
 *
 * `GScreen` defines two default [GLayer]s: [mainLayer] and [uiLayer]. [mainLayer] and [uiLayer] each hold a [Stage]
 * which can be used for the game world and UI respectively.
 *
 * [viewport] and [uiViewport] are also defined for use by [mainLayer] and [uiLayer].
 *
 * All [GLayer]s are processed in order specified by [layers], which can be overridden to change the order or to add
 * your own [GLayer]s.
 */
interface GScreen<T : GFramework> : KtxScreen
{
	/** The parent [GFramework] instance. */
	val game: T
	/** Primary viewport to project stage contents in [mainLayer]. */
	val viewport: Viewport
	/** Secondary viewport to project stage contents in [uiLayer]. */
	val uiViewport: ExtendViewport
	
	/** Main camera used by [viewport] to project contents of [stage] in [mainLayer]. */
	val camera: Camera
		get() = viewport.camera
	/** Secondary camera used by [uiViewport] to project contents of [uiStage] in [uiLayer]. */
	val uiCamera: OrthographicCamera
		get() = uiViewport.camera as OrthographicCamera
	
	val mainLayer: StageLayer
	val uiLayer: StageLayer
	/**
	 * All the [GLayer]s in the screen.
	 *
	 * [GLayer]s are ordered by their order in the array, so [GLayer]s will render on top of other [GLayer]s that come
	 * before it.
	 */
	val layers: Array<GLayer>
	
	/** Main stage in [mainLayer]. */
	val stage: Stage
		get() = mainLayer.stage
	/** Secondary stage in [uiLayer] meant for UI. */
	val uiStage: Stage
		get() = uiLayer.stage
	
	/**
	 * Convenient set of [KeyboardHeightObserver]s that you can notify soft-keyboard height changes in mobile
	 * platforms.
	 */
	val keyboardHeightObservers: GdxSet<KeyboardHeightObserver>
	
	val scheduledAddingGObjects: OrderedMap<GObject<*>, Group>
	val scheduledRemovalGObjects: OrderedSet<GObject<*>>
	
	override fun show()
	{
		Gdx.input.inputProcessor = InputMultiplexer(uiStage, stage)
	}
	
	override fun render(delta: Float)
	{
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
	fun clearScreen() = ScreenUtils.clear(Color.BLACK, true)
	
	override fun resize(width: Int, height: Int)
	{
		layers.forEach { it.resize(width, height) }
		Gdx.graphics.requestRendering()
	}
	
	/**
	 * Schedules [gObject] to be added into the world as a child of [parent].
	 *
	 * [gObject] will be added at the end of the next world time step. Useful during collision callbacks where creation
	 * and removal of objects are prohibited.
	 */
	fun scheduleSpawnGObject(gObject: GObject<*>, parent: Group = stage.root)
	{
		scheduledAddingGObjects[gObject] = parent
	}
	
	override fun dispose() = layers.forEach { it.dispose() }
}
