package misterbander.crazyeights.scene2d

import misterbander.crazyeights.CrazyEights
import misterbander.crazyeights.RoomScreen
import misterbander.gframework.scene2d.GObject

abstract class Hand(protected val room: RoomScreen) : GObject<CrazyEights>(room)
{
	protected val tabletop: Tabletop
		get() = room.tabletop
	abstract val cardGroup: CardGroup
	
	operator fun plusAssign(groupable: Groupable<CardGroup>)
	{
		cardGroup += groupable
	}
	
	operator fun minusAssign(groupable: Groupable<CardGroup>)
	{
		cardGroup -= groupable
	}
	
	abstract fun arrange(sort: Boolean = true)
}
