package misterbander.crazyeights.scene2d

import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.utils.Align
import ktx.actors.plusAssign
import ktx.scene2d.*
import misterbander.crazyeights.CrazyEights
import misterbander.gframework.scene2d.GObject

class CardGhost(parent: Groupable<CardGroup>) : GObject<CrazyEights>(parent.screen)
{
	init
	{
		this += scene2d.image("card_ghost") { setPosition(0F, 0F, Align.center) }
		
		touchable = Touchable.disabled
		
		setPosition(parent.x, parent.y)
		rotation = parent.rotation
	}
}
