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
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.badlogic.gdx.utils.viewport.Viewport
import ktx.actors.plusAssign
import ktx.app.KtxScreen
import ktx.collections.GdxSet
import ktx.collections.plusAssign
import misterbander.gframework.scene2d.GContactListener
import misterbander.gframework.scene2d.GObject
import misterbander.gframework.scene2d.KeyboardHeightObserver

/**
 * `GScreen`s are extensions of [KtxScreen]s. `GScreen`s have a main camera, a viewport, and a `Scene2D` [Stage] already
 * defined for you. All you need to do is to override the `show()` method and place your initialization code in there.
 * This could include setting up Tiled maps, creating [GObject]s and/or build your UI.
 *
 * The camera and the viewport can be overridden to use your own camera and/or viewport.
 *
 * `GScreen`s provide convenient methods to spawn `GObject`s.
 *
 * `GScreen`s may also optionally include a `Box2D` world.
 * @property game parent GFramework instance
 */
abstract class GScreen<T : GFramework>(val game: T) : KtxScreen, ContactListener
{
	/** Main camera for this GScreen. Defaults to an `OrthographicCamera`. */
	open val camera: Camera = OrthographicCamera().apply { setToOrtho(false) }
	/** Secondary camera for UI on this GScreen. Also defaults to an `OrthographicCamera`. */
	open val uiCamera = OrthographicCamera().apply { setToOrtho(false) }
	/** Viewport to project camera contents. Defaults to `ExtendViewport`. */
	open val viewport: Viewport by lazy { ExtendViewport(1280F, 720F, camera) }
	/** Viewport to project UI contents. Also defaults to `ExtendViewport`. */
	open val uiViewport by lazy { ExtendViewport(1280F, 720F, uiCamera) }
	val stage by lazy { Stage(viewport, game.batch) }
	val uiStage by lazy { Stage(uiViewport, game.batch) }
	val keyboardHeightObservers = GdxSet<KeyboardHeightObserver>()
	
	open val world: World? = null
	open val mpp = 0.25F
	
	val scheduledAddingGObjects = GdxSet<GObject<T>>()
	val scheduledRemovalGObjects = GdxSet<GObject<T>>()
	
	override fun show()
	{
		Gdx.input.inputProcessor = InputMultiplexer(uiStage, stage)
		world?.setContactListener(this)
	}
	
	/**
	 * Spawns the GObject into the world and adds it to the stage. Calls `GObject::onSpawn()`.
	 */
	fun spawnGObject(gObject: GObject<T>)
	{
		stage += gObject
		gObject.onSpawn()
	}
	
	fun scheduleSpawnGObject(gObject: GObject<T>)
	{
		scheduledAddingGObjects += gObject
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
	
	override fun preSolve(contact: Contact, oldManifold: Manifold) {}
	
	override fun postSolve(contact: Contact, impulse: ContactImpulse) {}
	
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
	
	override fun render(delta: Float)
	{
		clearScreen()
		renderStage(camera, stage, delta)
		renderStage(uiCamera, uiStage, delta)
		updateWorld()
	}
	
	protected fun renderStage(camera: Camera, stage: Stage, delta: Float)
	{
		camera.update()
		game.batch.projectionMatrix = camera.combined
		game.shapeRenderer.projectionMatrix = camera.combined
		game.shapeDrawer.update()
		stage.act(delta)
		stage.draw()
	}
	
	protected fun updateWorld()
	{
		scheduledAddingGObjects.forEach { spawnGObject(it) }
		scheduledAddingGObjects.clear()
		
		world?.step(1/60F, 6, 4)
		
		scheduledRemovalGObjects.forEach { it.remove() }
		scheduledRemovalGObjects.clear()
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
	
	override fun dispose()
	{
		stage.dispose()
	}
}
