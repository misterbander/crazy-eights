package misterbander.gframework.scene2d.module

import misterbander.gframework.GFramework
import misterbander.gframework.scene2d.GObject

/**
 * Modules can be added to [GObject]s to add custom behavior.
 * @property parent the parent [GObject] this module is attached to
 */
abstract class GModule<T : GFramework>(val parent: GObject<T>)
{
	/**
	 * Gets called every frame.
	 * @param delta the time in seconds since the last render
	 */
	open fun update(delta: Float) = Unit
}
