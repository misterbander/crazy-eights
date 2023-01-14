package misterbander.gframework

import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.utils.OrderedMap
import com.badlogic.gdx.utils.OrderedSet
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.badlogic.gdx.utils.viewport.Viewport
import ktx.collections.*
import misterbander.gframework.layer.GLayer
import misterbander.gframework.layer.StageLayer
import misterbander.gframework.scene2d.GObject
import misterbander.gframework.scene2d.KeyboardHeightObserver

/**
 * Default implementation of [GScreen].
 *
 * @param game the parent [GFramework] instance
 * @param viewport Primary viewport to project stage contents in [mainLayer]. Defaults to [ExtendViewport] with min
 * world size 1280x720.
 * @param uiViewport Secondary viewport to project stage contents in [uiLayer]. Defaults to [ExtendViewport] with min
 * world size 1280x720.
 */
open class DefaultGScreen<T : GFramework>(
	final override val game: T,
	final override val viewport: Viewport = ExtendViewport(1280F, 720F),
	final override val uiViewport: ExtendViewport = ExtendViewport(1280F, 720F)
) : GScreen<T>
{
	override val mainLayer = StageLayer(game, viewport, false)
	override val uiLayer = StageLayer(game, uiViewport, true)
	override val layers: Array<GLayer> by lazy { arrayOf(mainLayer, uiLayer) }
	
	final override val keyboardHeightObservers = GdxSet<KeyboardHeightObserver>()
	
	final override val scheduledAddingGObjects = OrderedMap<GObject<*>, Group>()
	final override val scheduledRemovalGObjects = OrderedSet<GObject<*>>()
}
