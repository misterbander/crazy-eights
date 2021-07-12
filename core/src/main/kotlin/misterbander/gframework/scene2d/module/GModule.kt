package misterbander.gframework.scene2d.module

import misterbander.gframework.GFramework
import misterbander.gframework.scene2d.GObject

/**
 * Modules can be added to GObjects to add custom behavior while running.
 * @property parent the parent GObject this module is attached to
 */
abstract class GModule<T : GFramework>(protected val parent: GObject<T>)
{
	/**
	 * Gets called every frame.
	 * @param delta time after the last frame
	 */
	open fun update(delta: Float) {}
}
