package misterbander.gframework

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.physics.box2d.Contact
import com.badlogic.gdx.physics.box2d.ContactImpulse
import com.badlogic.gdx.physics.box2d.ContactListener
import com.badlogic.gdx.physics.box2d.Manifold
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.OrderedMap
import com.badlogic.gdx.utils.OrderedSet
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.badlogic.gdx.utils.viewport.Viewport
import ktx.app.KtxScreen
import ktx.collections.GdxSet
import ktx.collections.set
import misterbander.gframework.scene2d.GContactListener
import misterbander.gframework.scene2d.GObject
import misterbander.gframework.scene2d.KeyboardHeightObserver
import misterbander.gframework.scene2d.plusAssign

/**
 * `GScreen`s are extensions of [KtxScreen]s and defines two cameras, two viewports, and two [Stage]s which can be used
 * for the world and UI.
 *
 * The cameras and viewports can be overridden to use your own camera and/or viewport.
 *
 * `GScreen`s provide convenient methods to spawn [GObject]s.
 *
 * `GScreen`s may also optionally include a `Box2D` [World].
 * @property game parent GFramework instance
 */
abstract class GScreen<T : GFramework>(val game: T) : KtxScreen, ContactListener
{
	/** Main camera used by [stage] and projected through [viewport]. Defaults to an [OrthographicCamera]. */
	open val camera: Camera = OrthographicCamera().apply { setToOrtho(false) }
	/** Secondary camera used by [uiStage] and projected through [uiViewport]. */
	open val uiCamera = OrthographicCamera().apply { setToOrtho(false) }
	/** Primary viewport to project [camera] contents. Defaults to [ExtendViewport]. */
	open val viewport: Viewport by lazy { ExtendViewport(1280F, 720F, camera) }
	/** Secondary viewport to project [uiCamera] contents. Also defaults to [ExtendViewport]. */
	open val uiViewport by lazy { ExtendViewport(1280F, 720F, uiCamera) }
	/** Main stage for generic purposes. */
	val stage by lazy { Stage(viewport, game.batch) }
	/** Secondary stage optimized for UI. */
	val uiStage by lazy { Stage(uiViewport, game.batch) }
	
	/**
	 * Convenient set of [KeyboardHeightObserver]s that you can notify soft-keyboard height changes from mobile
	 * platforms.
	 */
	val keyboardHeightObservers = GdxSet<KeyboardHeightObserver>()
	
	/** Optional `Box2D` [World] for this `GScreen`. */
	open val world: World? = null
	/** Meters per pixel. Used for `Box2D` unit conversions. */
	open val mpp = 0.25F
	
	val scheduledAddingGObjects = OrderedMap<GObject<T>, Group>()
	val scheduledRemovalGObjects = OrderedSet<GObject<T>>()
	
	override fun show()
	{
		Gdx.input.inputProcessor = InputMultiplexer(uiStage, stage)
		world?.setContactListener(this)
	}
	
	override fun render(delta: Float)
	{
		clearScreen()
		renderStage(camera, stage, delta)
		renderStage(uiCamera, uiStage, delta)
		updateWorld()
	}
	
	/**
	 * Clears the screen and paints it black. Gets called every frame.
	 *
	 * You can override this to change the background color.
	 */
	open fun clearScreen()
	{
		Gdx.gl.glClearColor(0F, 0F, 0F, 1F)
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
	}
	
	/**
	 * Acts and draws the stage with the specified camera.
	 * @param camera the camera to render the stage
	 * @param stage the stage to render
	 * @param delta the time in seconds since the last render
	 */
	protected fun renderStage(camera: Camera, stage: Stage, delta: Float)
	{
		camera.update()
		game.batch.projectionMatrix = camera.combined
		game.shapeRenderer.projectionMatrix = camera.combined
		game.shapeDrawer.update()
		stage.act(delta)
		stage.draw()
	}
	
	/**
	 * Steps the `Box2D` [World], and then adds all [GObject]s scheduled for adding, and removes all [GObject]s scheduled
	 * for removal. Should be called in [render].
	 */
	protected fun updateWorld()
	{
		world?.step(1/60F, 6, 4)
		scheduledAddingGObjects.forEach {
			val gObject: GObject<T> = it.key
			val group: Group? = it.value
			if (group != null)
				group += gObject
			else
				stage += gObject
		}
		scheduledAddingGObjects.clear()
		scheduledRemovalGObjects.forEach { it.remove() }
		scheduledRemovalGObjects.clear()
	}
	
	override fun resize(width: Int, height: Int)
	{
		if (viewport is ExtendViewport)
		{
			val extendViewport = viewport as ExtendViewport
			camera.position.set(extendViewport.minWorldWidth/2, extendViewport.minWorldHeight /2, 0F)
		}
		viewport.update(width, height, false)
		uiViewport.update(width, height, true)
		Gdx.graphics.requestRendering()
	}
	
	/**
	 * Schedules the [GObject] to be added into the world as a child of a specified [Group]. The [GObject] will be added
	 * at the end of the next world time step. Useful during collision callbacks where creation and removal of objects
	 * are prohibited.
	 * @param gObject the [GObject] to spawn
	 * @param parent the parent group to add the [GObject], defaults to [stage] root
	 */
	fun scheduleSpawnGObject(gObject: GObject<T>, parent: Group = stage.root)
	{
		scheduledAddingGObjects[gObject] = parent
	}
	
	override fun beginContact(contact: Contact)
	{
		if (contact.fixtureA.body?.userData is GContactListener)
			(contact.fixtureA.body.userData as GContactListener).beginContact(contact.fixtureB)
		if (contact.fixtureB.body?.userData is GContactListener)
			(contact.fixtureB.body.userData as GContactListener).beginContact(contact.fixtureA)
	}
	
	override fun endContact(contact: Contact)
	{
		if (contact.fixtureA.body?.userData is GContactListener)
			(contact.fixtureA.body.userData as GContactListener).endContact(contact.fixtureB)
		if (contact.fixtureB.body?.userData is GContactListener)
			(contact.fixtureB.body.userData as GContactListener).endContact(contact.fixtureA)
	}
	
	override fun preSolve(contact: Contact, oldManifold: Manifold) = Unit
	
	override fun postSolve(contact: Contact, impulse: ContactImpulse) = Unit
	
	override fun dispose() = stage.dispose()
}
