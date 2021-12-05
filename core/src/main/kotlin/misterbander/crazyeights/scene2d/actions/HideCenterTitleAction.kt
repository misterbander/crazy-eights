package misterbander.crazyeights.scene2d.actions

import com.badlogic.gdx.scenes.scene2d.actions.RunnableAction
import misterbander.crazyeights.Room

class HideCenterTitleAction(private val room: Room) : RunnableAction()
{
	override fun run()
	{
		room.centerTitleContainer.isVisible = false
	}
}
