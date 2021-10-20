package misterbander.crazyeights.scene2d

import misterbander.crazyeights.CrazyEights
import misterbander.crazyeights.scene2d.modules.Highlightable
import misterbander.crazyeights.scene2d.modules.Lockable
import misterbander.gframework.scene2d.GObject

interface DragTarget
{
	val lockable: Lockable
	val highlightable: Highlightable?
		get() = null
	
	fun canAccept(gObject: GObject<CrazyEights>): Boolean = false
	
	fun accept(gObject: GObject<CrazyEights>) = Unit
}
