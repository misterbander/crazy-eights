package misterbander.sandboxtabletop.scene2d

import misterbander.gframework.scene2d.GObject
import misterbander.sandboxtabletop.SandboxTabletop
import misterbander.sandboxtabletop.scene2d.modules.Highlightable
import misterbander.sandboxtabletop.scene2d.modules.Lockable

interface DragTarget
{
	val lockable: Lockable
	val highlightable: Highlightable?
		get() = null
	
	fun canAccept(gObject: GObject<SandboxTabletop>): Boolean = false
	
	fun accept(gObject: GObject<SandboxTabletop>) = Unit
}
