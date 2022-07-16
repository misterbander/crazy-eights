package misterbander.crazyeights.scene2d

import misterbander.crazyeights.CrazyEights
import misterbander.crazyeights.RoomScreen
import misterbander.gframework.scene2d.GObject

abstract class Hand(room: RoomScreen) : GObject<CrazyEights>(room)
{
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
