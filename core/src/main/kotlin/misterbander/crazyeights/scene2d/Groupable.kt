package misterbander.crazyeights.scene2d

import misterbander.crazyeights.CrazyEights
import misterbander.crazyeights.RoomScreen
import misterbander.crazyeights.scene2d.modules.Draggable
import misterbander.crazyeights.scene2d.modules.Lockable
import misterbander.crazyeights.scene2d.modules.Rotatable
import misterbander.crazyeights.scene2d.modules.SmoothMovable
import misterbander.gframework.scene2d.GObject

abstract class Groupable<T : GObject<CrazyEights>>(room: RoomScreen) : GObject<CrazyEights>(room)
{
	abstract val smoothMovable: SmoothMovable
	abstract val lockable: Lockable
	abstract val draggable: Draggable
	abstract val rotatable: Rotatable
}
