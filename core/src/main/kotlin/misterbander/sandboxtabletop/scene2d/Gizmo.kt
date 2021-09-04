package misterbander.sandboxtabletop.scene2d

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Actor
import space.earlygrey.shapedrawer.ShapeDrawer

class Gizmo(private val shapeDrawer: ShapeDrawer, color: Color) : Actor()
{
	init
	{
		this.color = color
	}
	
	override fun draw(batch: Batch, parentAlpha: Float)
	{
		shapeDrawer.setColor(color)
		shapeDrawer.circle(x, y, 16F)
		shapeDrawer.line(x - 64, y, x + 64, y)
		shapeDrawer.line(x, y - 64, x, y + 64)
	}
}
