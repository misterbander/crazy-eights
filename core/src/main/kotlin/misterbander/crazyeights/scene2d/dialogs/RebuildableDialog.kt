package misterbander.crazyeights.scene2d.dialogs

import com.badlogic.gdx.utils.Align
import misterbander.crazyeights.CrazyEightsScreen

abstract class RebuildableDialog(screen: CrazyEightsScreen, title: String) : CrazyEightsDialog(screen, title)
{
	abstract fun build()
	
	fun rebuild()
	{
		val centerX = x + width/2
		val centerY = y + height/2
		contentTable.clear()
		buttonTable.clear()
		build()
		pack()
		setPosition(centerX, centerY, Align.center)
	}
	
	override fun show()
	{
		rebuild()
		super.show()
	}
}
