package misterbander.crazyeights.scene2d

import com.badlogic.gdx.graphics.g2d.Batch
import ktx.actors.alpha
import misterbander.crazyeights.CrazyEights
import misterbander.crazyeights.RoomScreen
import misterbander.gframework.scene2d.GObject

class PowerCardEffectRing(room: RoomScreen) : GObject<CrazyEights>(room)
{
	private var radius = 1F
	private var thickness = 56F
	private val thicknessExpansionRate = -96
	private val expansionRate: Float
		get() = 2048 - radius*8
	private val fadingRate = 2
	
	init
	{
		val discardPileHolder = room.tabletop.discardPileHolder!!
		setPosition(discardPileHolder.x, discardPileHolder.y)
	}
	
	override fun update(delta: Float)
	{
		radius += expansionRate*delta
		alpha -= fadingRate*delta
		if (radius <= 0)
			remove()
		else if (alpha <= 0F)
		{
			alpha = 0F
			radius = 0F
			remove()
		}
		thickness += thicknessExpansionRate*delta
		if (thickness < 0)
			remove()
	}
	
	override fun draw(batch: Batch, parentAlpha: Float)
	{
		val shapeDrawer = game.shapeDrawer
		shapeDrawer.setColor(color)
		shapeDrawer.circle(x, y, radius, thickness)
	}
}
