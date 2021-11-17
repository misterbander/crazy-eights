package misterbander.crazyeights.scene2d

import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.utils.Align
import ktx.actors.plusAssign
import ktx.scene2d.*
import misterbander.crazyeights.CrazyEights
import misterbander.crazyeights.scene2d.modules.Draggable
import misterbander.crazyeights.scene2d.modules.Rotatable
import misterbander.gframework.scene2d.GObject

class CardGhost(private val projector: GObject<CrazyEights>) : GObject<CrazyEights>(projector.screen)
{
	init
	{
		this += scene2d.image("cardghost") { setPosition(0F, 0F, Align.center) }
		
		touchable = Touchable.disabled
		
		setPosition(projector.x, projector.y)
		rotation = projector.rotation
	}
	
	override fun update(delta: Float)
	{
		val draggable = projector.getModule<Draggable>()!!
		val rotatable = projector.getModule<Rotatable>()!!
		isVisible = draggable.justDragged || rotatable.justRotated || rotatable.isPinching
	}
}
